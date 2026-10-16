"""Run the owned camera approach arithmetic offline; no game launch or process access."""
import hashlib,json,struct
from pathlib import Path
import pefile
from unicorn import Uc,UC_ARCH_X86,UC_MODE_64
from unicorn.x86_const import UC_X86_REG_RSP,UC_X86_REG_XMM0,UC_X86_REG_XMM1,UC_X86_REG_XMM2,UC_X86_REG_XMM3

lab=Path('D:/DeadlockHazeLab');ref=lab/'reference/25950029'
raw=(ref/'client.dll').read_bytes();source=json.loads((ref/'source.json').read_text())
assert hashlib.sha256(raw).hexdigest()==source['clientSha256']
pe=pefile.PE(data=raw,fast_load=True);base=pe.OPTIONAL_HEADER.ImageBase
uc=Uc(UC_ARCH_X86,UC_MODE_64);uc.mem_map(base,(pe.OPTIONAL_HEADER.SizeOfImage+4095)&~4095)
for section in pe.sections:uc.mem_write(base+section.VirtualAddress,section.get_data())
stack,stop=0x10000000,0x20000000;uc.mem_map(stack,0x20000);uc.mem_map(stop,4096)

def native(current,target,strength,dt):
 rsp=stack+0x1ff08;uc.mem_write(rsp,struct.pack('<Q',stop));uc.reg_write(UC_X86_REG_RSP,rsp)
 for reg,value in zip([UC_X86_REG_XMM0,UC_X86_REG_XMM1,UC_X86_REG_XMM2,UC_X86_REG_XMM3],[current,target,strength,dt]):
  uc.reg_write(reg,struct.unpack('<I',struct.pack('<f',value))[0])
 uc.emu_start(0x1821effe0,stop,count=100)
 assert uc.reg_read(UC_X86_REG_RSP)==rsp+8
 return struct.unpack('<f',struct.pack('<I',uc.reg_read(UC_X86_REG_XMM0)&0xffffffff))[0]

cases=[]
for current in [0,.15,.4,.7,.99,1]:
 for target in [0,.25,.8,1]:
  for strength in [5,80]:
   for dt in [0,1/240,1/60,.15]:
    cases.append(dict(current=current,target=target,strength=strength,dt=dt,output=native(current,target,strength,dt)))
traces=[]
for dt in [1/30,1/64,1/120,1/240]:
 current=1;frames=[]
 for i in range(round(.5/dt)):
  target=0 if i*dt<.055 or i*dt>=.25 else 1
  current=native(current,target,80,dt);frames.append(dict(target=target,output=current))
 traces.append(dict(dt=dt,initial=1,strength=80,frames=frames))
output=lab/'research/25950029/camera/native-blend-cases.json';output.parent.mkdir(parents=True,exist_ok=True)
output.write_bytes(json.dumps(dict(build='25950029',sourceSha256=source['clientSha256'],function='0x1821effe0',
 caller='0x1805ea030',cvar='citadel_camera_height_approach_speed',strength=80,
 qualification='Original scalar blend helper only; camera object/state queries and pose composition are audited separately.',
 cases=cases,traces=traces),indent=2).encode())
print(dict(cases=len(cases),traceFrames=sum(len(t['frames'])for t in traces),out=str(output)))
