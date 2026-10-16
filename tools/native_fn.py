"""Dump precise PE exception function ranges for offline review; no process access."""
import argparse,json,struct,math,hashlib
from pathlib import Path
import pefile,capstone
p=argparse.ArgumentParser();p.add_argument('address',nargs='+');p.add_argument('--out',required=True);p.add_argument('--dll',default='D:/SteamLibrary/steamapps/common/Deadlock/game/citadel/bin/win64/client.dll');a=p.parse_args()
pe=pefile.PE(a.dll,fast_load=True)
pe.parse_data_directories(directories=[pefile.DIRECTORY_ENTRY['IMAGE_DIRECTORY_ENTRY_EXCEPTION']]);base=pe.OPTIONAL_HEADER.ImageBase;source_hash=hashlib.sha256(Path(a.dll).read_bytes()).hexdigest()
def primary(entry):
 while getattr(entry.unwindinfo,'UNW_FLAG_CHAININFO',False):entry=entry.unwindinfo._chained_entry
 return entry.struct.BeginAddress
groups={}
for entry in pe.DIRECTORY_ENTRY_EXCEPTION:
 root=primary(entry);groups.setdefault(root,[]).append(entry.struct)
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_64);md.detail=True;result=[]
for address in a.address:
 va=int(address,16)
 if not any(s.Characteristics&0x20000000 and base+s.VirtualAddress<=va<base+s.VirtualAddress+s.Misc_VirtualSize for s in pe.sections):
  raise ValueError('Not an executable image address: '+address+' (quote hexadecimal addresses in PowerShell)')
 entry=next((f for f in pe.DIRECTORY_ENTRY_EXCEPTION if base+f.struct.BeginAddress<=va<base+f.struct.EndAddress),None)
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
 if leaf:
  pending=[va];decoded={}
  while pending:
   block=pending.pop()
   for ins in md.disasm(pe.get_data(block-base,base+fn.EndAddress-block),block):
    if ins.address in decoded:break
    decoded[ins.address]=ins
    if ins.mnemonic.startswith('j')and ins.mnemonic!='jmp'and ins.operands and ins.operands[0].type==capstone.x86.X86_OP_IMM:
     target=ins.operands[0].imm
     if va<=target<base+fn.EndAddress:pending.append(target)
    if ins.mnemonic in ['ret','jmp','int3']:break
  instructions=[decoded[address]for address in sorted(decoded)]
  if instructions:fn.EndAddress=max(i.address+i.size for i in instructions)-base
 else:
  decoded={}
  for fragment in fragments:
   for ins in md.disasm(pe.get_data(fragment.BeginAddress,fragment.EndAddress-fragment.BeginAddress),base+fragment.BeginAddress):decoded[ins.address]=ins
  instructions=[decoded[address]for address in sorted(decoded)]
 for ins in instructions:
  row={'address':hex(ins.address),'mnemonic':ins.mnemonic,'operands':ins.op_str}
  for op in ins.operands:
   if op.type==capstone.x86.X86_OP_MEM and op.mem.base==capstone.x86.X86_REG_RIP:
    dest=ins.address+ins.size+op.mem.disp
    try:blob=pe.get_data(dest-base,120)if base<=dest<base+pe.OPTIONAL_HEADER.SizeOfImage else b''
    except pefile.PEFormatError:blob=b''
    row['rip_va']=hex(dest)
    if len(blob)>=4:
     value=struct.unpack('<f',blob[:4])[0]
     if math.isfinite(value):row['float']=value
    st=blob.split(b'\0')[0]
    if len(st)>=4 and all(32<=x<127 for x in st):row['string']=st.decode()
  rows.append(row)
 ranges=[{'start':hex(base+f.BeginAddress),'end':hex(base+f.EndAddress)}for f in fragments]if not leaf else [{'start':hex(base+fn.BeginAddress),'end':hex(base+fn.EndAddress)}]
 result.append({'sourceFile':a.dll,'sourceSha256':source_hash,'start':hex(base+fn.BeginAddress),'end':hex(base+fn.EndAddress),'ranges':ranges,'leafControlFlowFallback':leaf,'instructions':rows})
Path(a.out).write_text(json.dumps(result,indent=2));print([(f['start'],len(f['instructions']))for f in result])
