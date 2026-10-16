"""Execute owned current-build air-jump arithmetic offline. Game object inputs are supplied;
the original clamp, velocity arithmetic and percent-to-scale conversion are not replaced.
No game process is launched or attached. Private binary/output remain outside Git.
"""
import hashlib,json,struct
from pathlib import Path
import pefile,capstone
from unicorn import Uc,UC_ARCH_X86,UC_MODE_64,UC_HOOK_CODE
from unicorn.x86_const import *

lab=Path('D:/DeadlockHazeLab');reference=lab/'reference/25950029'
raw=(reference/'client.dll').read_bytes();source=json.loads((reference/'source.json').read_text())
assert hashlib.sha256(raw).hexdigest()==source['clientSha256']
pe=pefile.PE(data=raw,fast_load=True);base=pe.OPTIONAL_HEADER.ImageBase
functions=json.loads((lab/'evidence/25950029-core-controller-functions.json').read_text())
functions+=json.loads((lab/'evidence/25950029-value-percentage-conversion-functions.json').read_text())
air=next(f for f in functions if f['start']=='0x18024ce40')
conversion=next(f for f in functions if f['start']=='0x18131abe0')
allowed=[(int(r['start'],16),int(r['end'],16)) for f in [air,conversion] for r in f['ranges']]
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_64)
for clamp in [0x18022bf50,0x1802323a0]:
 for i in md.disasm(pe.get_data(clamp-base,128),clamp):
    if i.mnemonic=='ret':allowed.append((clamp,i.address+i.size));break
uc=Uc(UC_ARCH_X86,UC_MODE_64);uc.mem_map(base,(pe.OPTIONAL_HEADER.SizeOfImage+4095)&~4095)
for s in pe.sections:uc.mem_write(base+s.VirtualAddress,s.get_data())
stack,heap,stop=0x10000000,0x20000000,0x30000000
uc.mem_map(stack,0x20000);uc.mem_map(heap,0x40000);uc.mem_map(stop,4096)
ability,pawn,movement,stats,vdata,vector,tls,slots,thread=[heap+i*0x4000 for i in range(9)]
def write(a,fmt,*v):uc.mem_write(a,struct.pack(fmt,*v))
def float_result(v):uc.reg_write(UC_X86_REG_XMM0,struct.unpack('<I',struct.pack('<f',v))[0])
write(tls+0x58,'<Q',slots);write(slots,'<Q',thread);write(thread+0x68,'<i',0x7fffffff)
uc.reg_write(UC_X86_REG_GS_BASE,tls)
# These native TLS guards describe initialized empty/scalar caches in a running game.
write(0x183527050,'<I',2)
calls={int(r['address'],16):r for r in air['instructions'] if r['mnemonic']=='call'}
state={};injected={}
def guard(machine,address,size,_):
    assert any(lo<=address<hi for lo,hi in allowed), 'Unexpected native execution '+hex(address)
    if address not in calls:return
    row=calls[address];target=row['operands']
    if target in ['0x18022bf50','0x18131abe0']:return
    rcx,rdx=[uc.reg_read(r) for r in [UC_X86_REG_RCX,UC_X86_REG_RDX]]
    if target=='0x1804aca50':kind='resource object';uc.reg_write(UC_X86_REG_RAX,stats)
    elif target=='0x1804a0860':kind='resource event skipped';uc.reg_write(UC_X86_REG_RAX,0)
    elif target in ['0x1804ac870','0x180a759f0']:kind='pawn getter';uc.reg_write(UC_X86_REG_RAX,pawn)
    elif target=='0x180a72660':
        kind='input velocity getter';write(rdx,'<3f',*state['velocity']);uc.reg_write(UC_X86_REG_RAX,rdx)
    elif target=='0x18086b2f0':
        kind='input wish getter';write(rdx,'<3f',*state['wish']);uc.reg_write(UC_X86_REG_RAX,rdx)
    elif target in ['0x18086ba70','0x180932b30']:
        kind='input maximum speed getter';float_result(state['maxSpeed'])
    elif target=='0x1804ad910':kind='input VerticalSpeed property getter';float_result(state['verticalSpeed'])
    elif target=='0x181306340':
        assert uc.reg_read(UC_X86_REG_R8)==136
        kind='single registered raw bonus property (before original conversion)'
        write(rdx,'<I3I4f',2,0,0,0,state['bonusPercent'],0,0,0);uc.reg_write(UC_X86_REG_RAX,rdx)
    elif target in ['0x180232790','0x18022d420','0x181724bb0']:
        raise AssertionError('Unexpected eligibility/bonus branch: '+target)
    elif target in ['0x18130d120','0x1806e2b30'] or target.startswith('qword ptr'):
        kind='presentation event/string operation skipped'
    else:raise AssertionError('Unspecified dependency: '+target)
    injected[hex(address)]={'target':target,'kind':kind}
    uc.reg_write(UC_X86_REG_RIP,address+size)
uc.hook_add(UC_HOOK_CODE,guard)
cases=[]
for velocity in [[0,0,0],[0,0,250],[0,0,-400],[300,0,100],[500,0,-100],[-300,200,0]]:
 for wish in [[0,0,0],[1,0,0],[-1,0,0],[0,1,0],[.70710678,.70710678,0]]:
  for percent in [0,75,100,-50]:
    state.update(velocity=velocity,wish=wish,maxSpeed=8.2/.0254,verticalSpeed=300,bonusPercent=percent)
    uc.mem_write(ability,b'\0'*0x2000);uc.mem_write(pawn,b'\0'*0x2000)
    write(ability+0x390,'<Q',vdata);write(vdata+0x16d4,'<f',200)
    write(pawn+0x348,'<Q',stats);write(pawn+0xec0,'<Q',movement)
    write(movement+0x278,'<Q',0)
    rsp=stack+0x1ff08;write(rsp,'<Q',stop)
    for reg,val in [(UC_X86_REG_RSP,rsp),(UC_X86_REG_RCX,ability),(UC_X86_REG_RDX,vector),(UC_X86_REG_R8,pawn)]:uc.reg_write(reg,val)
    uc.emu_start(0x18024ce40,stop,count=20000)
    assert uc.reg_read(UC_X86_REG_RSP)==rsp+8,'Native instruction budget exhausted'
    result=list(struct.unpack('<3f',uc.mem_read(vector,12)))
    cases.append(dict(state,output=result,airJumpCount=uc.mem_read(ability+0x1bb5,1)[0]))
out=lab/'research/25950029/movement/native-air-jump-cases.json';out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(dict(sourceSha256=source['clientSha256'],build=source['build'],
    function='0x18024ce40',conversion='0x18131abe0',qualification='Impulse arithmetic with supplied object getters, one raw bonus value, and skipped presentation/resource events; eligibility is not covered',
    dependencies=injected,cases=cases),indent=2))
print({'cases':len(cases),'hazeAirJumpUp':next(c['output'][2] for c in cases if c['bonusPercent']==75),'out':str(out)})
