"""Read a named native schema enum from the owner's pinned PE, without SDK values."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import struct
import pefile

p = argparse.ArgumentParser()
p.add_argument('--dll', required=True)
p.add_argument('--prefix', required=True)
p.add_argument('--anchor', help='Walk one contiguous schema enum around this exact member')
p.add_argument('--out', required=True)
a = p.parse_args()
raw = Path(a.dll).read_bytes()
pe = pefile.PE(data=raw, fast_load=True)
base = pe.OPTIONAL_HEADER.ImageBase
names = {}
for match in re.finditer(re.escape(a.prefix.encode()) + rb'[A-Z_0-9]+\x00', raw):
    address = base + pe.get_rva_from_offset(match.start())
    names[address] = match.group()[:-1].decode('ascii')
entries = {}
if a.anchor:
    anchor_offset = raw.find(a.anchor.encode() + b'\0')
    assert anchor_offset >= 0, a.anchor
    anchor_address = base + pe.get_rva_from_offset(anchor_offset)
    candidates = []
    for section in pe.sections:
        if section.Characteristics & 0x20000000:
            continue
        section_data = section.get_data()
        for match in re.finditer(re.escape(struct.pack('<Q', anchor_address)), section_data):
            pos = match.start()
            if pos % 8 or pos + 32 > len(section_data):
                continue
            ordinal = struct.unpack_from('<q', section_data, pos + 8)[0]
            if not 0 <= ordinal < 4096:
                continue
            records = {}
            for direction in [-1, 1]:
                offset, expected = pos, ordinal
                while 0 <= offset <= len(section_data) - 32:
                    ptr, value, metadata_count, metadata_ptr = struct.unpack_from('<QqQQ', section_data, offset)
                    if value != expected or metadata_count > 32 or not base <= ptr < base + pe.OPTIONAL_HEADER.SizeOfImage:
                        break
                    name_raw = pe.get_data(ptr-base, 160).split(b'\0')[0]
                    if not re.fullmatch(re.escape(a.prefix.encode()) + rb'[A-Za-z_0-9]+', name_raw):
                        break
                    records[name_raw.decode()] = {'value': value, 'stringAddress': hex(ptr),
                                                  'tableAddress': hex(base+section.VirtualAddress+offset)}
                    offset += direction * 32
                    expected += direction
            if len(records) > 1:
                candidates.append(records)
    assert len(candidates) == 1, 'Anchor must identify exactly one contiguous enum'
    entries = candidates[0]
for section in pe.sections:
    if a.anchor:
        break
    if section.Characteristics & 0x20000000:
        continue
    data = section.get_data()
    for offset in range(0, len(data) - 32, 8):
        pointer = struct.unpack_from('<Q', data, offset)[0]
        if pointer not in names:
            continue
        value = struct.unpack_from('<q', data, offset + 8)[0]
        # Schema entries have a value followed by their metadata count and pointer.
        if not -1 <= value < 4096:
            continue
        name = names[pointer]
        proof = {'value': value, 'stringAddress': hex(pointer),
                 'tableAddress': hex(base + section.VirtualAddress + offset)}
        if name in entries:
            assert entries[name]['value'] == value, (name, entries[name], proof)
        entries[name] = proof
assert entries, 'No verified schema table entries'
result = {'sourceFile': a.dll, 'sourceSha256': hashlib.sha256(raw).hexdigest(),
          'prefix': a.prefix, 'entries': entries}
Path(a.out).write_text(json.dumps(result, indent=2), encoding='utf-8')
print(json.dumps({'prefix': a.prefix, 'entries': len(entries), 'out': a.out}))
