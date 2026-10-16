"""Current air acceleration executed offline; wish preparation and modifiers supplied as inputs."""
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
fn=next(f for f in rows if f['start']=='0x18085a1b0')
selected=[f for f in rows if f['start'] in ['0x18085a1b0','0x18085fcd0']]
allowed=[(int(r['start'],16),int(r['end'],16)) for f in selected for r in f['ranges']]
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_64)
allowed.append((0x180292b80,0x180292bd1)) # complete multi-return RemapValClamped body
for address in [0x180282ab0]:
 for ins in md.disasm(pe.get_data(address-base,128),address):
  if ins.mnemonic=='ret':allowed.append((address,ins.address+ins.size));break
uc=Uc(UC_ARCH_X86,UC_MODE_64);uc.mem_map(base,(pe.OPTIONAL_HEADER.SizeOfImage+4095)&~4095)
for section in pe.sections:uc.mem_write(base+section.VirtualAddress,section.get_data())
stack,heap,stop=0x10000000,0x20000000,0x30000000
uc.mem_map(stack,0x20000);uc.mem_map(heap,0x40000);uc.mem_map(stop,4096)
movement,move,pawn,stats=[heap+i*0x4000 for i in range(4)]
def write(a,fmt,*v):uc.mem_write(a,struct.pack(fmt,*v))
def floating(v):uc.reg_write(UC_X86_REG_XMM0,struct.unpack('<I',struct.pack('<f',v))[0])
write(movement+0x38,'<Q',pawn);write(pawn+0x348,'<Q',stats)
for address,slot,value in [(0x1837db338,0,80),(0x1837db328,1,50),(0x183d3d2a8,2,10)]:
 write(address,'<Q',heap+0x20000+slot*0x100);write(heap+0x20058+slot*0x100,'<f',value)
calls={int(r['address'],16):r for f in selected for r in f['instructions'] if r['mnemonic']=='call'}
state={};injected={}
def guard(machine,address,size,_):
 assert any(lo<=address<hi for lo,hi in allowed),'Unexpected execution '+hex(address)
 if address not in calls:return
 target=calls[address]['operands'];rcx=uc.reg_read(UC_X86_REG_RCX)
 if target in ['0x18085fcd0','0x180282ab0','0x180292b80']:return
 if target=='0x18085fd10':
  kind='prepared wish input';write(uc.reg_read(UC_X86_REG_R8),'<3f',*state['wish']);write(uc.reg_read(UC_X86_REG_R9),'<f',state['wishSpeed'])
 elif target=='0x18086d350':kind='air-speed override input';floating(state['override'])
 elif target in ['0x1804acf20','0x1804bd8b0','0x180a769e0']:kind='no special ability/state';uc.reg_write(UC_X86_REG_RAX,0)
 elif target in ['0x180852370','0x1808521d0','0x180852410']:kind='attribute descriptors';uc.reg_write(UC_X86_REG_RAX,heap+0x22000)
 elif target=='0x180850e80':kind='air-control multiplier input';floating(1)
 elif target=='0x180850fb0':kind='acceleration multiplier input';floating(state['accelScale'])
 else:raise AssertionError('Unspecified dependency '+target)
 injected[hex(address)]={'target':target,'kind':kind};uc.reg_write(UC_X86_REG_RIP,address+size)
uc.hook_add(UC_HOOK_CODE,guard);cases=[]
for velocity in [[0,0,0],[10,0,20],[0,200,-50],[200,0,0],[-300,200,-200],[500,500,50]]:
 for wish in [[1,0,0],[0,1,0],[.70710678,.70710678,0],[-1,0,0]]:
  for override in [0,600]:
   for scale in [1,.6]:
    for dt in [1/64,1/128,.004]:
     state.update(velocity=velocity,wish=wish,wishSpeed=max(8.2/.0254,override),override=override,accelScale=scale,dt=dt)
     uc.mem_write(move,b'\0'*0x200);write(move+0x38,'<3f',*velocity)
     uc.mem_write(move+0xf4,pe.get_data(0x25ba434,4)*3)
     rsp=stack+0x1ff08;write(rsp,'<Q',stop)
     uc.reg_write(UC_X86_REG_RSP,rsp);uc.reg_write(UC_X86_REG_RCX,movement);uc.reg_write(UC_X86_REG_RDX,move)
     uc.reg_write(UC_X86_REG_XMM2,struct.unpack('<I',struct.pack('<f',dt))[0])
     uc.emu_start(0x18085a1b0,stop,count=20000);assert uc.reg_read(UC_X86_REG_RSP)==rsp+8
     cases.append(dict(state,output=list(struct.unpack('<3f',uc.mem_read(move+0x38,12)))))
out=lab/'research/25950029/movement/native-air-accel-cases.json'
out.write_text(json.dumps(dict(build=source['build'],sourceSha256=source['clientSha256'],function='0x18085a1b0',
 qualification='Original acceleration and speed-to-control remap. Prepared wish and modifier scalars supplied; displacement-controlled/special ability branches excluded.',dependencies=injected,cases=cases),indent=2))
print({'cases':len(cases),'out':str(out)})
