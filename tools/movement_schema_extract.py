"""Recover schema-member offsets from exact native metadata registrations.

Only the explicit member pointer in r9 is accepted. The output keeps the owner
function and instruction evidence, without guessing class names from addresses.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import capstone
import pefile

p = argparse.ArgumentParser()
p.add_argument('--dll', required=True)
p.add_argument('--refs', required=True)
p.add_argument('--out', required=True)
a = p.parse_args()
data = Path(a.dll).read_bytes()
pe = pefile.PE(data=data)
base = pe.OPTIONAL_HEADER.ImageBase
md = capstone.Cs(capstone.CS_ARCH_X86, capstone.CS_MODE_64)
md.detail = True
records = []
for ref in json.loads(Path(a.refs).read_text())['refs']:
    if not ref['string'].startswith('m_'):
        continue
    va = int(ref['xref_va'], 16)
    entry = next((f for f in pe.DIRECTORY_ENTRY_EXCEPTION
                  if f.struct.BeginAddress <= va-base < f.struct.EndAddress), None)
    if entry is None:
        continue
    rows = list(md.disasm(pe.get_data(va-base, 100), va))
    assert rows and rows[0].address == va, ref
    names = []
    for operand in rows[0].operands:
        if operand.type == capstone.x86.X86_OP_MEM and operand.mem.base == capstone.x86.X86_REG_RIP:
            pointer = va + rows[0].size + operand.mem.disp
            names.append(pe.get_data(pointer-base, len(ref['string'])+1))
    assert (ref['string']+'\0').encode() in names, 'Name reference does not match selected DLL: '+str(ref)
    for ins in rows[1:12]:
        if ins.mnemonic in ('call', 'ret', 'jmp'):
            break
        match = re.fullmatch(r'r9, \[(r[a-z0-9]+)(?: \+ (0x[0-9a-f]+|[0-9]+))?\]', ins.op_str)
        # RIP-relative metadata/callback addresses are not instance member offsets.
        if ins.mnemonic == 'lea' and match and match[1] != 'rip':
            records.append({'field': ref['string'], 'offset': hex(int(match[2] or '0', 0)),
                            'baseRegister': match[1], 'nameReference': hex(va),
                            'offsetReference': hex(ins.address),
                            'ownerFragment': hex(base + entry.struct.BeginAddress)})
            break
Path(a.out).write_text(json.dumps({'sourceSha256': hashlib.sha256(data).hexdigest(),
                                  'fields': records}, indent=2))
print(f'Recovered {len(records)} explicit member-pointer registrations')
