"""Read MSVC RTTI and disassemble a class's virtual methods, offline only."""
import argparse,struct,json,re,math
from pathlib import Path
import pefile,capstone
p=argparse.ArgumentParser();p.add_argument('name');p.add_argument('out');a=p.parse_args()
path=Path('D:/SteamLibrary/steamapps/common/Deadlock/game/citadel/bin/win64/client.dll')
pe=pefile.PE(str(path),fast_load=True);base=pe.OPTIONAL_HEADER.ImageBase;data=path.read_bytes()
name=('\x2e?AV'+a.name+'@@\0').encode();offset=data.find(name)
if offset<0:raise SystemExit('RTTI type not found')
td=pe.get_rva_from_offset(offset)-16
cols=[]
for s in pe.sections:
 if s.Characteristics&0x20000000:continue
 raw=s.get_data()
 for match in re.finditer(re.escape(struct.pack('<I',td)),raw):
  pos=match.start()-12
  if pos<0:continue
  sig,off,ctor,typ,hier,selfrva=struct.unpack_from('<6I',raw,pos)
  if sig==1 and selfrva==s.VirtualAddress+pos:cols.append(base+selfrva)
tables=[]
textRanges=[(base+s.VirtualAddress,base+s.VirtualAddress+s.Misc_VirtualSize)for s in pe.sections if s.Characteristics&0x20000000]
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_64);md.detail=True
for col in cols:
 for s in pe.sections:
  if s.Characteristics&0x20000000:continue
  raw=s.get_data()
  for match in re.finditer(re.escape(struct.pack('<Q',col)),raw):
   pos=match.start()+8;methods=[]
   while pos+8<len(raw):
    addr=struct.unpack_from('<Q',raw,pos)[0]
    if not any(lo<=addr<hi for lo,hi in textRanges):break
    rows=[]
    for ins in md.disasm(pe.get_data(addr-base,2048),addr):
     row={'address':hex(ins.address),'mnemonic':ins.mnemonic,'operands':ins.op_str}
     for op in ins.operands:
      if op.type==capstone.x86.X86_OP_MEM and op.mem.base==capstone.x86.X86_REG_RIP:
       dest=ins.address+ins.size+op.mem.disp
       if base<=dest<base+pe.OPTIONAL_HEADER.SizeOfImage:
        blob=pe.get_data(dest-base,120);row['rip_va']=hex(dest)
        if len(blob)>=4:
         number=struct.unpack('<f',blob[:4])[0]
         if math.isfinite(number):row['float']=number
        st=blob.split(b'\0')[0]
        if len(st)>=4 and all(32<=x<127 for x in st):row['string']=st.decode()
     rows.append(row)
     if ins.mnemonic=='ret':break
    methods.append({'index':len(methods),'va':hex(addr),'instructions':rows});pos+=8
   if methods:tables.append({'vtable':hex(base+s.VirtualAddress+match.start()+8),'methods':methods})
Path(a.out).write_text(json.dumps({'class':a.name,'tables':tables},indent=2,allow_nan=False).replace('NaN','null'))
print(a.name,[(t['vtable'],len(t['methods']))for t in tables])
