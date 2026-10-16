"""Offline wall-impulse execution. Supplies getters, fatigue and curve result explicitly."""
import hashlib,json,struct
from pathlib import Path
import pefile,capstone
from unicorn import Uc,UC_ARCH_X86,UC_MODE_64,UC_HOOK_CODE
from unicorn.x86_const import *
lab=Path('D:/DeadlockHazeLab');source=json.loads((lab/'reference/25950029/source.json').read_text())
raw=(lab/'reference/25950029/client.dll').read_bytes();assert hashlib.sha256(raw).hexdigest()==source['clientSha256']
pe=pefile.PE(data=raw,fast_load=True);base=pe.OPTIONAL_HEADER.ImageBase
rows=json.loads((lab/'evidence/25950029-core-controller-functions.json').read_text())
rows+=json.loads((lab/'evidence/25950029-wall-slide-kernels-functions.json').read_text())
fn=next(f for f in rows if f['start']=='0x18024ded0')
selected=[f for f in rows if f['start'] in ['0x18024ded0','0x180287aa0','0x180251e00']]
allowed=[(int(r['start'],16),int(r['end'],16)) for f in selected for r in f['ranges']]
calls={int(r['address'],16):r for r in fn['instructions'] if r['mnemonic']=='call'}
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_64)
for address in [0x18022bf50]:
 for ins in md.disasm(pe.get_data(address-base,128),address):
  if ins.mnemonic=='ret':allowed.append((address,ins.address+ins.size));break
uc=Uc(UC_ARCH_X86,UC_MODE_64);uc.mem_map(base,(pe.OPTIONAL_HEADER.SizeOfImage+4095)&~4095)
for section in pe.sections:uc.mem_write(base+section.VirtualAddress,section.get_data())
stack,heap,stop=0x10000000,0x20000000,0x30000000
uc.mem_map(stack,0x20000);uc.mem_map(heap,0x40000);uc.mem_map(stop,4096)
ability,pawn,vdata,vector,stats=[heap+i*0x4000 for i in range(5)]
def write(a,fmt,*v):uc.mem_write(a,struct.pack(fmt,*v))
def floating(v):uc.reg_write(UC_X86_REG_XMM0,struct.unpack('<I',struct.pack('<f',v))[0])
state={};injected={}
write(0x183513b60,'<Q',heap+0x20000);write(heap+0x20058,'<i',1)
def guard(machine,address,size,_):
 assert any(lo<=address<hi for lo,hi in allowed),'Unexpected execution '+hex(address)
 if address not in calls:return
 target=calls[address]['operands'];rcx=uc.reg_read(UC_X86_REG_RCX);rdx=uc.reg_read(UC_X86_REG_RDX)
 if target in ['0x18022bf50','0x180287aa0','0x180251e00']:return
 if target=='0x1804aca50':kind='resource object';uc.reg_write(UC_X86_REG_RAX,stats)
 elif target=='0x1804a0860':kind='resource event skipped';uc.reg_write(UC_X86_REG_RAX,0)
 elif target=='0x180875b70':kind='combat condition input';uc.reg_write(UC_X86_REG_RAX,0)
 elif target in ['0x1804ac870','0x180a759f0']:kind='pawn getter';uc.reg_write(UC_X86_REG_RAX,pawn)
 elif target=='0x180a72660':kind='velocity input';write(rdx,'<3f',*state['velocity']);uc.reg_write(UC_X86_REG_RAX,rdx)
 elif target=='0x18086b2f0':kind='wish input';write(rdx,'<3f',*state['wish']);uc.reg_write(UC_X86_REG_RAX,rdx)
 elif target=='0x180256500':assert rcx==0x183513b28;kind='verified corner boost cvar';floating(.6)
 elif target=='0x18025ae50':kind='fatigue input';floating(state['power'])
 elif target=='0x1821fcc00':kind='curve output input';floating(state['falloff']);state['curveInput']=struct.unpack('<f',struct.pack('<I',uc.reg_read(UC_X86_REG_XMM1)&0xffffffff))[0]
 elif target=='0x18076b230':kind='time input';write(rcx,'<f',100);uc.reg_write(UC_X86_REG_RAX,rcx)
 elif target=='0x18023f8f0':kind='time plus duration';write(rcx,'<f',101);uc.reg_write(UC_X86_REG_RAX,rcx)
 elif target in ['0x1804fa4d0','0x18130d120','0x1806e2b30'] or target.startswith('qword ptr'):kind='presentation event skipped'
 else:raise AssertionError('Unspecified dependency '+target)
 injected[hex(address)]={'target':target,'kind':kind};uc.reg_write(UC_X86_REG_RIP,address+size)
uc.hook_add(UC_HOOK_CODE,guard);cases=[]
for normal in [[1,0,0],[-1,0,0],[.70710678,.70710678,0],[.8,0,.6],[.8,0,-.6]]:
 for velocity in [[0,0,0],[-300,200,-400],[100,500,100],[400,-150,-1]]:
  for wish in [[0,0,0],[1,0,0],[-1,0,0],[0,1,0],[.70710678,.70710678,0]]:
   for power,falloff in [(1,1),(.2,.3)]:
    state.clear();state.update(normal=normal,velocity=velocity,wish=wish,power=power,falloff=falloff)
    uc.mem_write(ability,b'\0'*0x2000);uc.mem_write(pawn,b'\0'*0x2000)
    write(ability+0x10,'<Q',heap+0x1c000);write(ability+0x390,'<Q',vdata);write(ability+0x16f0,'<3f',*normal)
    for offset,value in [(0x1750,525),(0x1754,100),(0x1798,225),(0x179c,310)]:write(vdata+offset,'<f',value)
    write(pawn+0x348,'<Q',stats)
    rsp=stack+0x1ff08;write(rsp,'<Q',stop)
    for reg,value in [(UC_X86_REG_RSP,rsp),(UC_X86_REG_RCX,ability),(UC_X86_REG_RDX,vector),(UC_X86_REG_R8,pawn)]:uc.reg_write(reg,value)
    try:uc.emu_start(0x18024ded0,stop,count=20000)
    except Exception:
     print({'instruction':hex(uc.reg_read(UC_X86_REG_RIP)),'case':state});raise
    assert uc.reg_read(UC_X86_REG_RSP)==rsp+8
    cases.append(dict(state,output=list(struct.unpack('<3f',uc.mem_read(vector,12)))))
out=lab/'research/25950029/movement/native-wall-jump-cases.json'
out.write_text(json.dumps(dict(build=source['build'],sourceSha256=source['clientSha256'],function='0x18024ded0',
 qualification='Original impulse, normal blend/projection and curve input; supplied fatigue and curve output; resource/presentation and eligibility excluded',dependencies=injected,cases=cases),indent=2))
print({'cases':len(cases),'out':str(out)})
