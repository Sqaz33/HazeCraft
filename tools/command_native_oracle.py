"""Execute the original double-press block with explicit command/button history.

Entry 0x1817b71b8 and exit 0x1817b725b delimit the arithmetic/state loop after
button preparation and before presentation/command housekeeping. The register
and stack setup is explicit, not a claim to emulate the entire containing method.
No patched movement instructions, injected calculations or live process access.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct

import pefile
from unicorn import Uc, UC_ARCH_X86, UC_MODE_64, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_RSP, UC_X86_REG_RSI, UC_X86_REG_R12, UC_X86_REG_XMM0

p=argparse.ArgumentParser()
p.add_argument('--reference',default='D:/DeadlockHazeLab/reference/25939744')
p.add_argument('--out',default='D:/DeadlockHazeLab/research/25939744/movement/native-command-cases.json')
a=p.parse_args()
reference=Path(a.reference)
source=json.loads((reference/'source.json').read_text())
data=(reference/'client.dll').read_bytes()
assert source['build']=='25939744'
assert hashlib.sha256(data).hexdigest()==source['clientSha256']
evidence=reference.parents[1]/'evidence'
rows=json.loads((evidence/'25939744-subtick-button-state-functions.json').read_text())
rows+=json.loads((evidence/'25939744-input-percent-and-physical-kernels.json').read_text())
functions={f['start']:f for f in rows if f['start'] in ['0x1817b7000','0x180d4c280','0x180d4c340']}
assert len(functions)==3
assert all(f['sourceSha256']==source['clientSha256'] for f in functions.values())
entry,exit_address=0x1817b71b8,0x1817b725b
assert all(any(int(r['start'],16)<=address<int(r['end'],16) for r in functions['0x1817b7000']['ranges'])
           for address in [entry,exit_address])
allowed=[(entry,exit_address)]
allowed += [(int(r['start'],16),int(r['end'],16)) for name,f in functions.items()
            if name!='0x1817b7000' for r in f['ranges']]
pe=pefile.PE(data=data,fast_load=True)
base=pe.OPTIONAL_HEADER.ImageBase
uc=Uc(UC_ARCH_X86,UC_MODE_64)
uc.mem_map(base,(pe.OPTIONAL_HEADER.SizeOfImage+4095)&~4095)
for section in pe.sections:uc.mem_write(base+section.VirtualAddress,section.get_data())
stack,heap=0x10000000,0x20000000
uc.mem_map(stack,0x20000)
uc.mem_map(heap,0x10000)

def guard(machine,address,size,_):
    assert any(lo<=address and address+size<=hi for lo,hi in allowed),hex(address)
uc.hook_add(UC_HOOK_CODE,guard)

def write(address,fmt,*values):uc.mem_write(address,struct.pack(fmt,*values))

cases=[]
actor,command,globals_=heap,heap+0x2000,heap+0x3000
write(0x183359bf0,'<Q',globals_)
for bit in [0,31,48,58,62,63]:
    mask=1<<bit
    for state in range(8):
        for interval in [1/64,1/128]:
            for delta in [0,1,13,14,15,27,28,29,0xffffffff]:
                current=5
                previous=(current-delta)&0xffffffff
                history=[0xffff0000+i for i in range(64)]
                history[bit]=previous
                for offset,flag in [(0x58,1),(0x60,2),(0x68,4)]:write(actor+offset,'<Q',mask if state&flag else 0)
                write(actor+0x88,'<64I',*history)
                write(command+8,'<I',current)
                write(globals_+0x54,'<f',interval)
                rsp=stack+0x10008
                write(rsp+0xb8,'<Q',command)
                uc.reg_write(UC_X86_REG_RSP,rsp)
                uc.reg_write(UC_X86_REG_RSI,actor)
                uc.reg_write(UC_X86_REG_R12,0)
                uc.reg_write(UC_X86_REG_XMM0,struct.unpack('<I',struct.pack('<f',.22))[0])
                uc.emu_start(entry,exit_address,count=10000)
                assert uc.reg_read(UC_X86_REG_R12)==64,'Did not finish all button histories'
                actual=struct.unpack('<Q',uc.mem_read(actor+0x80,8))[0]
                after=list(struct.unpack('<64I',uc.mem_read(actor+0x88,256)))
                assert all(after[i]==history[i] for i in range(64) if i!=bit),'Unrelated button changed'
                cases.append(dict(bit=bit,state=state,command=current,previous=previous,
                    interval=interval,window=.22,expectedDouble=bool(actual&mask),expectedLastPress=after[bit]))
assert len(cases)==864
out=Path(a.out)
assert not out.resolve().is_relative_to(Path(__file__).resolve().parents[1])
out.write_text(json.dumps(dict(source=source,
    method='isolated original command-history block and original button predicates; explicit register/stack inputs',cases=cases),indent=2))
print(json.dumps(dict(cases=len(cases),out=str(out))))
