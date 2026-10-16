"""Locate existing research functions in another owned build; candidates, not semantic proof."""
import argparse, hashlib, json, re
from pathlib import Path
import capstone, pefile

p=argparse.ArgumentParser()
p.add_argument('--old',required=True);p.add_argument('--new',required=True)
p.add_argument('--functions',required=True);p.add_argument('--out',required=True)
a=p.parse_args()
old_data,new_data=Path(a.old).read_bytes(),Path(a.new).read_bytes()
old,new=pefile.PE(data=old_data,fast_load=True),pefile.PE(data=new_data,fast_load=True)
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_64);md.detail=True
sections=[s for s in new.sections if s.Characteristics&0x20000000]
records=[]
for path in sorted(Path(a.functions).glob('Movement_*.c')):
    address=int(path.stem.split('_')[1],16)
    raw=old.get_data(address-old.OPTIONAL_HEADER.ImageBase,192)
    mask=bytearray(b'\1'*len(raw));length=0
    for ins in md.disasm(raw,address):
        offset=ins.address-address;length=offset+ins.size
        if any(op.type==capstone.x86.X86_OP_MEM and op.mem.base==capstone.x86.X86_REG_RIP for op in ins.operands):
            for i in range(ins.disp_size):mask[offset+ins.disp_offset+i]=0
        if ins.group(capstone.CS_GRP_JUMP) or ins.group(capstone.CS_GRP_CALL):
            for i in range(ins.imm_size):mask[offset+ins.imm_offset+i]=0
        if ins.mnemonic in ('ret','int3') or (ins.mnemonic=='jmp' and ins.operands[0].type==capstone.x86.X86_OP_IMM):break
    pattern=b''.join(re.escape(bytes([raw[i]])) if mask[i] else b'.' for i in range(length))
    matches=[]
    for section in sections:
        for found in re.finditer(pattern,section.get_data(),re.S):
            matches.append(hex(new.OPTIONAL_HEADER.ImageBase+section.VirtualAddress+found.start()))
            if len(matches)>32:break
    records.append(dict(old=hex(address),new=matches[0] if len(matches)==1 else None,
                        candidates=matches,signatureBytes=length))
result=dict(oldSha256=hashlib.sha256(old_data).hexdigest(),newSha256=hashlib.sha256(new_data).hexdigest(),
            qualification='Relocation-masked prefix candidates only; full bodies and dependencies must be checked',functions=records)
Path(a.out).write_text(json.dumps(result,indent=2))
print({'functions':len(records),'unique':sum(r['new'] is not None for r in records),'output':a.out})
