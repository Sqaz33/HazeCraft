"""Find and verify direct x64 call/tail-jump references in an owned, pinned PE.

Candidate byte matches are decoded at the exact instruction address. This is not
proof that an undisassembled candidate is reachable; callers retain that caveat.
"""
import argparse
import bisect
import hashlib
import json
from pathlib import Path
import re
import struct
import capstone
import pefile

p = argparse.ArgumentParser()
p.add_argument('addresses', nargs='+')
p.add_argument('--dll', required=True)
p.add_argument('--out', required=True)
a = p.parse_args()
data = Path(a.dll).read_bytes()
pe = pefile.PE(data=data)
base = pe.OPTIONAL_HEADER.ImageBase
targets = {int(s, 16) for s in a.addresses}
functions = sorted((f.struct.BeginAddress, f.struct.EndAddress) for f in pe.DIRECTORY_ENTRY_EXCEPTION)
starts = [f[0] for f in functions]
md = capstone.Cs(capstone.CS_ARCH_X86, capstone.CS_MODE_64)
refs = []
for section in pe.sections:
    if not section.Characteristics & 0x20000000:
        continue
    raw = section.get_data()
    va = base + section.VirtualAddress
    for m in re.finditer(rb'[\xe8\xe9]', raw):
        offset = m.start()
        if offset + 5 > len(raw):
            continue
        target = va + offset + 5 + struct.unpack_from('<i', raw, offset + 1)[0]
        if target not in targets:
            continue
        ins = next(md.disasm(raw[offset:offset + 5], va + offset), None)
        if ins is None or ins.size != 5 or ins.mnemonic not in ('call', 'jmp'):
            continue
        index = bisect.bisect_right(starts, va + offset - base) - 1
        owner = functions[index] if index >= 0 else None
        if owner and va + offset - base >= owner[1]:
            owner = None
        refs.append({'address': hex(ins.address), 'target': hex(target), 'kind': ins.mnemonic,
                     'unwindFragment': [hex(base + v) for v in owner] if owner else None})
Path(a.out).write_text(json.dumps({'sourceSha256': hashlib.sha256(data).hexdigest(), 'refs': refs}, indent=2))
print(json.dumps(refs))
