"""Find exact RIP-memory references to a pinned data address; offline instruction evidence."""
import argparse,json
from pathlib import Path
import numpy as np
import pefile,capstone
p=argparse.ArgumentParser();p.add_argument('address');p.add_argument('--dll',required=True);p.add_argument('--out',required=True);a=p.parse_args()
pe=pefile.PE(a.dll,fast_load=True);base=pe.OPTIONAL_HEADER.ImageBase;target=int(a.address,16)
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_64);md.detail=True;result=[]
for section in pe.sections:
 if not section.Characteristics&0x20000000:continue
 raw=section.get_data();va=base+section.VirtualAddress
 displacements=np.ndarray((len(raw)-4,),dtype='<i4',buffer=raw,strides=(1,))
 candidates=np.flatnonzero(displacements.astype(np.int64)+np.arange(len(displacements),dtype=np.int64)+va+4==target)
 for offset in candidates:
  for start in range(max(0,int(offset)-12),int(offset)+1):
   ins=next(md.disasm(raw[start:start+15],va+start),None)
   if ins and any(op.type==capstone.x86.X86_OP_MEM and op.mem.base==capstone.x86.X86_REG_RIP and ins.address+ins.size+op.mem.disp==target for op in ins.operands):
    result.append(dict(address=hex(ins.address),mnemonic=ins.mnemonic,operands=ins.op_str))
Path(a.out).write_text(json.dumps(result,indent=2));print(result)
