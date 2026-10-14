"""Dump precise PE exception function ranges for offline review; no process access."""
import argparse,json,struct,math
from pathlib import Path
import pefile,capstone
p=argparse.ArgumentParser();p.add_argument('address',nargs='+');p.add_argument('--out',required=True);a=p.parse_args()
pe=pefile.PE('D:/SteamLibrary/steamapps/common/Deadlock/game/citadel/bin/win64/client.dll',fast_load=True)
pe.parse_data_directories(directories=[pefile.DIRECTORY_ENTRY['IMAGE_DIRECTORY_ENTRY_EXCEPTION']]);base=pe.OPTIONAL_HEADER.ImageBase
def primary(entry):
 while getattr(entry.unwindinfo,'UNW_FLAG_CHAININFO',False):entry=entry.unwindinfo._chained_entry
 return entry.struct.BeginAddress
groups={}
for entry in pe.DIRECTORY_ENTRY_EXCEPTION:
 root=primary(entry);groups.setdefault(root,[]).append(entry.struct)
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_64);md.detail=True;result=[]
for address in a.address:
 va=int(address,16);entry=next((f for f in pe.DIRECTORY_ENTRY_EXCEPTION if base+f.struct.BeginAddress<=va<base+f.struct.EndAddress),None)
 fn=entry.struct if entry else None
 leaf=fn is None
 if leaf:
  class Leaf:pass
  fn=Leaf();fn.BeginAddress=va-base;fn.EndAddress=fn.BeginAddress+1024
 else:
  fragments=groups[primary(entry)]
  class Whole:pass
  fn=Whole();fn.BeginAddress=min(f.BeginAddress for f in fragments);fn.EndAddress=max(f.EndAddress for f in fragments)
 rows=[]
 for ins in md.disasm(pe.get_data(fn.BeginAddress,fn.EndAddress-fn.BeginAddress),base+fn.BeginAddress):
  row={'address':hex(ins.address),'mnemonic':ins.mnemonic,'operands':ins.op_str}
  for op in ins.operands:
   if op.type==capstone.x86.X86_OP_MEM and op.mem.base==capstone.x86.X86_REG_RIP:
    dest=ins.address+ins.size+op.mem.disp;blob=pe.get_data(dest-base,120)if dest>=base else b''
    row['rip_va']=hex(dest)
    if len(blob)>=4:
     value=struct.unpack('<f',blob[:4])[0]
     if math.isfinite(value):row['float']=value
    st=blob.split(b'\0')[0]
    if len(st)>=4 and all(32<=x<127 for x in st):row['string']=st.decode()
  rows.append(row)
  if leaf and ins.mnemonic in ['ret','jmp']:break
 result.append({'start':hex(base+fn.BeginAddress),'end':hex(base+fn.EndAddress),'instructions':rows})
Path(a.out).write_text(json.dumps(result,indent=2));print([(f['start'],len(f['instructions']))for f in result])
