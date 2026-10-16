"""Current native horizontal air drag with explicit object/attribute inputs; offline only."""
import hashlib,json,math,struct
from pathlib import Path
import pefile,capstone
from unicorn import Uc,UC_ARCH_X86,UC_MODE_64,UC_HOOK_CODE
from unicorn.x86_const import *
lab=Path('D:/DeadlockHazeLab');source=json.loads((lab/'reference/25950029/source.json').read_text())
raw=(lab/'reference/25950029/client.dll').read_bytes();assert hashlib.sha256(raw).hexdigest()==source['clientSha256']
pe=pefile.PE(data=raw,fast_load=True);base=pe.OPTIONAL_HEADER.ImageBase
rows=json.loads((lab/'evidence/25950029-core-controller-functions.json').read_text())
fn=next(f for f in rows if f['start']=='0x18085a730')
primitives=json.loads((lab/'evidence/25950029-vector-primitives.json').read_text())
allowed=[(int(r['start'],16),int(r['end'],16)) for f in [fn]+primitives for r in f['ranges']]
calls={int(r['address'],16):r for r in fn['instructions'] if r['mnemonic']=='call'}
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_64)
for ins in md.disasm(pe.get_data(0x566ff0,32),0x180566ff0):
 if ins.mnemonic=='ret':allowed.append((0x180566ff0,ins.address+ins.size));break
uc=Uc(UC_ARCH_X86,UC_MODE_64);uc.mem_map(base,(pe.OPTIONAL_HEADER.SizeOfImage+4095)&~4095)
for s in pe.sections:uc.mem_write(base+s.VirtualAddress,s.get_data())
stack,heap,stop=0x10000000,0x20000000,0x30000000
uc.mem_map(stack,0x20000);uc.mem_map(heap,0x40000);uc.mem_map(stop,4096)
movement,move,pawn,stats,rotation=heap,heap+0x4000,heap+0x8000,heap+0x10000,heap+0x18000
def write(a,fmt,*v):uc.mem_write(a,struct.pack(fmt,*v))
def float_result(v):uc.reg_write(UC_X86_REG_XMM0,struct.unpack('<I',struct.pack('<f',v))[0])
write(movement+0x38,'<Q',pawn);write(pawn+0x348,'<Q',stats)
# Static initializer 1801eb280 copies this exact 16-byte constant to the basis.
assert pe.get_data(0x1eb280,8)==bytes.fromhex('660f6f05787f5102')
basis=pe.get_data(0x2703200,16);assert struct.unpack('<4f',basis)==(1,0,0,0)
uc.mem_write(0x183e7e570,basis)
write(0x1837db2b8,'<Q',heap+0x20000);write(heap+0x20058,'<f',.2)
write(0x183d3d208,'<Q',heap+0x21000);write(heap+0x21058,'<f',100)
state={};injected={}
def guard(machine,address,size,_):
 assert any(lo<=address<hi for lo,hi in allowed),'Unexpected execution '+hex(address)
 if address not in calls:return
 target=calls[address]['operands'];rcx=uc.reg_read(UC_X86_REG_RCX)
 if target in ['0x180282ab0','0x1802351d0','0x180566ff0']:return
 if target=='0x18086d350':kind='air-speed override input';float_result(state['override'])
 elif target=='0x180a71650':kind='body rotation getter';uc.reg_write(UC_X86_REG_RAX,rotation)
 elif target in ['0x180852550','0x1808524b0']:kind='attribute descriptors';uc.reg_write(UC_X86_REG_RAX,heap+0x22000)
 elif target=='0x180850e40':kind='air-drag attribute input';float_result(state['drag'])
 elif target=='0x180886c60':kind='slow penalty input';float_result(state['slowScale'])
 else:raise AssertionError('Unspecified dependency '+target)
 injected[hex(address)]={'target':target,'kind':kind};uc.reg_write(UC_X86_REG_RIP,address+size)
uc.hook_add(UC_HOOK_CODE,guard)
cases=[]
for velocity in [[0,0,0],[10,0,20],[0,200,-50],[200,0,0],[-300,200,-200],[500,500,50]]:
 for yaw in [0,45,90]:
  for inputs in [[0,0],[1,0],[0,1],[1,1]]:
   for override in [0,600]:
    for dt in [1/64,1/128,.004]:
     state.update(velocity=velocity,yaw=yaw,inputs=inputs,override=override,dt=dt,drag=0,slowScale=1,minimumDrag=.2,stopSpeed=100)
     uc.mem_write(move,b'\0'*0x200);write(move+0x38,'<3f',*velocity);write(move+0x2c,'<2f',*inputs)
     half=math.radians(yaw)/2;write(rotation+0x10,'<4f',0,0,math.sin(half),math.cos(half))
     rsp=stack+0x1ff08;write(rsp,'<Q',stop)
     for reg,val in [(UC_X86_REG_RSP,rsp),(UC_X86_REG_RCX,movement),(UC_X86_REG_RDX,move)]:uc.reg_write(reg,val)
     uc.reg_write(UC_X86_REG_XMM2,struct.unpack('<I',struct.pack('<f',dt))[0])
     uc.emu_start(0x18085a730,stop,count=20000)
     assert uc.reg_read(UC_X86_REG_RSP)==rsp+8
     cases.append(dict(state,output=list(struct.unpack('<3f',uc.mem_read(move+0x38,12)))))
out=lab/'research/25950029/movement/native-air-drag-cases.json';out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(dict(build=source['build'],sourceSha256=source['clientSha256'],function='0x18085a730',
 qualification='Original horizontal drag, body rotation/projection and vector primitives; supplied object/attribute/slow inputs. Vertical-drag and special states are excluded.',dependencies=injected,cases=cases),indent=2))
print({'cases':len(cases),'pureLateral':next(c['output'] for c in cases if c['velocity']==[0,200,-50] and c['yaw']==0 and c['override']==0),'out':str(out)})
