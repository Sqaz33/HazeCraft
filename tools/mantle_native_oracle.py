"""Evaluate the owner's current native mantle trajectory offline.

Synthetic movement state is the input. No game process, OS callbacks, game code
copying, collision emulation or claims about the untested mantle probe.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct

import pefile
from unicorn import Uc, UC_ARCH_X86, UC_MODE_64, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_RCX, UC_X86_REG_RDX, UC_X86_REG_RSP, UC_X86_REG_R8

p = argparse.ArgumentParser()
p.add_argument('--reference', default='D:/DeadlockHazeLab/reference/25939744')
p.add_argument('--evidence', default='D:/DeadlockHazeLab/evidence/25939744-native-mantle-kernels.json')
p.add_argument('--out', default='D:/DeadlockHazeLab/research/25939744/movement/native-mantle-cases.json')
a = p.parse_args()
reference = Path(a.reference)
source = json.loads((reference / 'source.json').read_text())
data = (reference / 'client.dll').read_bytes()
assert source['build'] == '25939744'
assert hashlib.sha256(data).hexdigest() == source['clientSha256']
functions = json.loads(Path(a.evidence).read_text())
assert {f['start'] for f in functions} == {
    '0x1802574c0', '0x180241210', '0x18023f8e0', '0x18022bf50', '0x1821e8ab0'}
assert all(f['sourceSha256'] == source['clientSha256'] for f in functions)
allowed = [(int(r['start'], 16), int(r['end'], 16)) for f in functions for r in f['ranges']]
pe = pefile.PE(data=data, fast_load=True)
base = pe.OPTIONAL_HEADER.ImageBase
uc = Uc(UC_ARCH_X86, UC_MODE_64)
uc.mem_map(base, (pe.OPTIONAL_HEADER.SizeOfImage + 4095) & ~4095)
for section in pe.sections:
    uc.mem_write(base + section.VirtualAddress, section.get_data())
stack, heap, stop = 0x10000000, 0x20000000, 0x30000000
uc.mem_map(stack, 0x20000)
uc.mem_map(heap, 0x10000)
uc.mem_map(stop, 4096)

def guard(machine, address, size, _):
    assert any(lo <= address and address+size <= hi for lo, hi in allowed), hex(address)

uc.hook_add(UC_HOOK_CODE, guard)

def write(address, fmt, *values):
    uc.mem_write(address, struct.pack(fmt, *values))

def evaluate(row, t):
    actor, vdata, mantle_type, globals_, result = [heap+i for i in [0, 0x2000, 0x4000, 0x5000, 0x6000]]
    write(actor + 0x390, '<Q', vdata)
    write(vdata + 0x13e8, '<I', 1)
    write(vdata + 0x13f0, '<Q', mantle_type)
    write(mantle_type + 0x14, '<2f', row['verticalTime'], row['horizontalTime'])
    write(actor + 0x1704, '<I', 0)
    write(actor + 0x1708, '<f', row['startTime'])
    write(actor + 0x16e0, '<3f', *row['start'])
    write(actor + 0x16ec, '<3f', *row['end'])
    write(actor + 0x16d8, '<2f', row['upCorrection'], row['frontCorrection'])
    write(0x183359bf0, '<Q', globals_)
    write(globals_ + 0x54, '<f', row['tickInterval'])
    rsp = stack + 0x1ff08
    write(rsp, '<Q', stop)
    uc.reg_write(UC_X86_REG_RSP, rsp)
    uc.reg_write(UC_X86_REG_RCX, actor)
    uc.reg_write(UC_X86_REG_RDX, result)
    # GameTime_t is a 4-byte value class: Windows x64 passes its bits in R8D,
    # despite provisional decompiler C displaying a plain float parameter.
    uc.reg_write(UC_X86_REG_R8, struct.unpack('<I', struct.pack('<f', t))[0])
    uc.emu_start(0x1802574c0, stop, count=10000)
    assert uc.reg_read(UC_X86_REG_RSP) == rsp+8, 'Instruction budget exceeded'
    return list(struct.unpack('<3f', uc.mem_read(result, 12)))

cases = []
for tick in [1/64, 1/128, 1/20]:
    for height, vertical_time in [(32, .2), (64, .3), (96, .4), (128, .5)]:
        for direction in [(1, 0), (.6, .8), (-.8, .6)]:
            row = dict(start=[120, -450, 30], end=[120+48*direction[0], -450+48*direction[1], 30+height],
                       startTime=10.25, tickInterval=tick, verticalTime=vertical_time,
                       horizontalTime=.2, frontCorrection=14, upCorrection=height-40)
            for elapsed in [-.05, 0, .015625, vertical_time/4, vertical_time/2,
                            vertical_time-.001, vertical_time, vertical_time+.001,
                            vertical_time+.1, vertical_time+.2, vertical_time+.3]:
                case = dict(row, time=row['startTime']+elapsed)
                cases.append(dict(input=case, expected=evaluate(row, case['time'])))
output = Path(a.out)
assert cases[2]['expected'] != cases[0]['expected'], 'Trajectory must respond to input time'
assert not output.resolve().is_relative_to(Path(__file__).resolve().parents[1])
output.write_text(json.dumps(dict(source=source, method='isolated original x64 trajectory; synthetic state; Source XYZ coordinates', cases=cases), indent=2))
print(json.dumps(dict(cases=len(cases), out=str(output))))
