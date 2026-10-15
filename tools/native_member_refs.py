"""Locate displacement uses decoded from native unwind-fragment boundaries.

Displacement matches are navigation evidence only: unrelated classes may use the
same offset. Leaf code without unwind entries is intentionally not claimed here.
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

p=argparse.ArgumentParser()
p.add_argument('offsets',nargs='+')
p.add_argument('--dll',required=True)
p.add_argument('--out',required=True)
a=p.parse_args()
data=Path(a.dll).read_bytes()
pe=pefile.PE(data=data)
base=pe.OPTIONAL_HEADER.ImageBase
offsets={int(value,0) for value in a.offsets}
fragments=sorted((f.struct.BeginAddress,f.struct.EndAddress) for f in pe.DIRECTORY_ENTRY_EXCEPTION)
starts=[f[0] for f in fragments]
candidates=set()
for section in pe.sections:
    if not section.Characteristics&0x20000000:continue
    raw=section.get_data()
    for offset in offsets:
        for match in re.finditer(re.escape(struct.pack('<i',offset)),raw):
            rva=section.VirtualAddress+match.start()
            index=bisect.bisect_right(starts,rva)-1
            if index>=0 and rva<fragments[index][1]:candidates.add(index)
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_64)
md.detail=True
rows=[]
for index in sorted(candidates):
    start,end=fragments[index]
    for ins in md.disasm(pe.get_data(start,end-start),base+start):
        for operand in ins.operands:
            if operand.type==capstone.x86.X86_OP_MEM and operand.mem.base!=capstone.x86.X86_REG_RIP and operand.mem.disp in offsets:
                rows.append(dict(address=hex(ins.address),mnemonic=ins.mnemonic,operands=ins.op_str,
                    displacement=hex(operand.mem.disp),unwindFragment=[hex(base+start),hex(base+end)]))
Path(a.out).write_text(json.dumps(dict(sourceSha256=hashlib.sha256(data).hexdigest(),
    limitation='Displacement use is not class identity or runtime reachability; no unwind-free leaf coverage.',refs=rows),indent=2))
print(json.dumps(rows,indent=2))
