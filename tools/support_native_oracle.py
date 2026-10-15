"""Original support spring and dash-parry gravity, offline with synthetic state.

The only injected dependency is a ground-entity getter returning the case's
boolean. Its RET trampoline contains no movement logic. All spring/time/gravity
arithmetic executes the owner's pinned x64 instructions. No host collision,
Windows DLL loading, process access or complete-controller claim is involved.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct

import pefile
from unicorn import Uc, UC_ARCH_X86, UC_MODE_64, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_RCX, UC_X86_REG_RDX, UC_X86_REG_RSP, UC_X86_REG_RAX, UC_X86_REG_XMM2

p = argparse.ArgumentParser()
p.add_argument('--reference', default='D:/DeadlockHazeLab/reference/25939744')
p.add_argument('--out', default='D:/DeadlockHazeLab/research/25939744/movement/native-support-cases.json')
a = p.parse_args()
reference = Path(a.reference)
source = json.loads((reference / 'source.json').read_text())
data = (reference / 'client.dll').read_bytes()
assert source['build'] == '25939744'
assert hashlib.sha256(data).hexdigest() == source['clientSha256']
evidence = reference.parents[1] / 'evidence'
functions = json.loads((evidence / '25939744-support-and-parry-kernels-functions.json').read_text())
functions += json.loads((evidence / '25939744-time-value-kernel.json').read_text())
assert {f['start'] for f in functions} == {'0x18085c280', '0x180259870', '0x18076b210',
    '0x180292b60', '0x18023f8e0', '0x18023caa0'}
assert all(f['sourceSha256'] == source['clientSha256'] for f in functions)
allowed = [(int(r['start'], 16), int(r['end'], 16)) for f in functions for r in f['ranges']]
pe = pefile.PE(data=data, fast_load=True)
base = pe.OPTIONAL_HEADER.ImageBase
uc = Uc(UC_ARCH_X86, UC_MODE_64)
uc.mem_map(base, (pe.OPTIONAL_HEADER.SizeOfImage + 4095) & ~4095)
for section in pe.sections:
    uc.mem_write(base + section.VirtualAddress, section.get_data())
stack, heap, stop, ground_getter = 0x10000000, 0x20000000, 0x30000000, 0x30000100
uc.mem_map(stack, 0x20000)
uc.mem_map(heap, 0x10000)
uc.mem_map(stop, 4096)
uc.mem_write(ground_getter, b'\xc3')
ground_entity = False

def guard(machine, address, size, _):
    if address == ground_getter:
        machine.reg_write(UC_X86_REG_RAX, int(ground_entity))
        return
    assert any(lo <= address and address+size <= hi for lo, hi in allowed), hex(address)

uc.hook_add(UC_HOOK_CODE, guard)

def write(address, fmt, *values):
    uc.mem_write(address, struct.pack(fmt, *values))

def floats(address, count):
    return list(struct.unpack('<'+'f'*count, uc.mem_read(address, 4*count)))

def invoke(address, rcx, rdx, dt=0):
    rsp = stack + 0x1ff08
    write(rsp, '<Q', stop)
    for reg, value in [(UC_X86_REG_RSP, rsp), (UC_X86_REG_RCX, rcx), (UC_X86_REG_RDX, rdx)]:
        uc.reg_write(reg, value)
    uc.reg_write(UC_X86_REG_XMM2, struct.unpack('<I', struct.pack('<f', dt))[0])
    uc.emu_start(address, stop, count=10000)
    assert uc.reg_read(UC_X86_REG_RSP) == rsp+8, 'Instruction budget exceeded'

movement, move_data, pawn, vtable = [heap+i for i in [0, 0x1000, 0x2000, 0x3000]]
frequency_cvar, damping_cvar = heap+0x4000, heap+0x4100
write(movement+0x38, '<Q', pawn)
write(pawn, '<Q', vtable)
write(vtable+0x5b8, '<Q', ground_getter)
write(0x183d3c278, '<Q', frequency_cvar)
write(0x183d3c288, '<Q', damping_cvar)
spring_cases = []
for ground_entity in [False, True]:
    for contact in [False, True]:
        for disabled in [False, True]:
            for mode, velocity in [(0, [100, 0, -20]), (2, [100, 0, -20]),
                                   (2, [100, 0, 20]), (1, [400, 0, 0]),
                                   (1, [400.01, 0, 0]), (1, [300, 300, -30])]:
                for dt in [1/64, 1/128, .004]:
                    row = dict(grounded=ground_entity, support=[12, -45, 100] if contact else [0, 0, 0],
                        disabled=disabled, mode=mode, velocity=velocity, springVelocity=[2, -3, 20],
                        originUp=101.5, frequency=10, damping=1, dt=dt)
                    write(movement+0x2cc, '<3f', *row['support'])
                    write(movement+0x2bc, '<3f', *row['springVelocity'])
                    write(move_data+0x10c, '<B', disabled)
                    write(move_data+0x118, '<I', mode)
                    write(move_data+0x38, '<3f', *velocity)
                    write(move_data+0xd0, '<f', row['originUp'])
                    write(frequency_cvar+0x58, '<f', row['frequency'])
                    write(damping_cvar+0x58, '<f', row['damping'])
                    invoke(0x18085c280, movement, move_data, dt)
                    spring_cases.append(dict(input=row, springVelocity=floats(movement+0x2bc, 3),
                        velocity=floats(move_data+0x38, 3)))

actor, vdata, time_owner, globals_, result = [heap+i for i in [0x5000, 0x7000, 0x9000, 0xa000, 0xb000]]
write(actor+0x390, '<Q', vdata)
write(actor+0x10, '<Q', time_owner)
write(time_owner+0x38, '<I', 0)
write(0x183359bf0, '<Q', globals_)
write(0x183d3c360, '<Q', 0)
parry_cases = []
for start in [0, 10.25]:
    for duration in [0, .4, .8]:
        for initial in [.3, .6]:
            for elapsed in [-.01, 0, .015625, .1, .2, .39999, .4, .5, .8, 1]:
                row = dict(start=start, duration=duration, initialScale=initial, time=start+elapsed)
                write(actor+0x1704, '<f', start)
                write(vdata+0x183c, '<2f', duration, initial)
                write(globals_+0x30, '<f', row['time'])
                uc.mem_write(result, bytes(32))
                invoke(0x180259870, actor, result)
                parry_cases.append(dict(input=row, active=struct.unpack('<I', uc.mem_read(result, 4))[0] != 0,
                    scale=floats(result+0x10, 1)[0]))
assert len(spring_cases) == 144 and len(parry_cases) == 120
output = Path(a.out)
assert not output.resolve().is_relative_to(Path(__file__).resolve().parents[1])
output.write_text(json.dumps(dict(source=source,
    method='isolated original x64; synthetic movement/support/time; injected ground boolean getter only',
    spring=spring_cases, parry=parry_cases), indent=2))
print(json.dumps(dict(springCases=len(spring_cases), parryCases=len(parry_cases), out=str(output))))
