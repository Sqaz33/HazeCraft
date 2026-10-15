"""Execute selected pure math from the owned client in an isolated CPU emulator.

No DLL loading, processes, injection or OS calls. Only the four verified math
functions and vector-length helper may execute. Results remain in the private lab.
This validates those kernels, not full movement/collision fidelity.
"""
import argparse
import hashlib
import json
from pathlib import Path
import random
import struct
from kv3 import loads

import pefile
from unicorn import Uc, UC_ARCH_X86, UC_MODE_64, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_RCX, UC_X86_REG_RDX, UC_X86_REG_RSP, UC_X86_REG_RAX
from unicorn.x86_const import UC_X86_REG_XMM0, UC_X86_REG_XMM1, UC_X86_REG_XMM2, UC_X86_REG_XMM3

p = argparse.ArgumentParser()
p.add_argument('--reference', default='D:/DeadlockHazeLab/reference/25939744')
p.add_argument('--out', default='D:/DeadlockHazeLab/research/25939744/movement/native-math-cases.json')
a = p.parse_args()
reference = Path(a.reference)
source = json.loads((reference / 'source.json').read_text())
data = (reference / 'client.dll').read_bytes()
assert source['build'] == '25939744'
assert hashlib.sha256(data).hexdigest() == source['clientSha256']
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
allowed = [(0x1821e8ab0, 0x1821e8b04), (0x180292b60, 0x180292be0),
           (0x1821fc440, 0x1821fc603), (0x18085fcb0, 0x18085fce7),
           (0x180282a90, 0x180282ac0)]

def guard(machine, address, size, _):
    assert any(lo <= address and address+size <= hi for lo, hi in allowed), hex(address)

uc.hook_add(UC_HOOK_CODE, guard)

def bits(value):
    return struct.unpack('<I', struct.pack('<f', value))[0]

def invoke(address, registers=(), floats=(), fifth=None):
    rsp = stack + 0x1ff08
    uc.mem_write(rsp, struct.pack('<Q', stop))
    uc.reg_write(UC_X86_REG_RSP, rsp)
    for reg, value in registers:
        uc.reg_write(reg, value)
    for reg, value in zip([UC_X86_REG_XMM0, UC_X86_REG_XMM1, UC_X86_REG_XMM2, UC_X86_REG_XMM3], floats):
        uc.reg_write(reg, bits(value))
    if fifth is not None:
        uc.mem_write(rsp + 0x28, struct.pack('<f', fifth))
    uc.emu_start(address, stop, count=10000)
    assert uc.reg_read(UC_X86_REG_RSP) == rsp + 8, 'Did not return within instruction budget'
    return struct.unpack('<f', struct.pack('<I', uc.reg_read(UC_X86_REG_XMM0) & 0xffffffff))[0]

cases = []
rng = random.Random(25939744)
for value in [0, .2, .25, .5, .75, 1] + [rng.random() for _ in range(40)]:
    for bias in [.2, .6]:
        cases.append({'kernel': 'bias', 'input': [value, bias],
                      'expected': invoke(0x1821e8ab0, floats=[value, bias])})
for value in [-1, 0, .1, .3, .55, 1, 2]:
    args = [value, 0, 1.25, .2, 1]
    cases.append({'kernel': 'remap', 'input': args,
                  'expected': invoke(0x180292b60, floats=args[:4], fifth=args[4])})

ability_data=(reference / 'scripts/abilities.vdata').read_bytes()
assert hashlib.sha256(ability_data).hexdigest()==source['dataHashes']['scripts/abilities.vdata']
definitions = loads(ability_data.decode('utf-8'))
curves = {
    'wallLateral': definitions['citadel_ability_jump']['m_WallJumpLateralSpeedFalloffVsAlongSpeed'],
    'slideGetup': definitions['citadel_ability_slide']['m_GetupSpeedCurve'],
}
for name, curve in curves.items():
    points = curve['m_spline']
    uc.mem_write(heap, bytes(0x40))
    uc.mem_write(heap, struct.pack('<I', len(points)))
    uc.mem_write(heap + 8, struct.pack('<Q', heap + 0x100))
    uc.mem_write(heap + 0x34, struct.pack('<f', curve['m_vDomainMins'][1]))
    uc.mem_write(heap + 0x3c, struct.pack('<f', curve['m_vDomainMaxs'][1]))
    for i, point in enumerate(points):
        uc.mem_write(heap + 0x100 + 16*i, struct.pack('<4f', point['x'], point['y'],
                                                    point['m_flSlopeIncoming'], point['m_flSlopeOutgoing']))
    low, high = points[0]['x'], points[-1]['x']
    for x in [low-10, low, high, high+10] + [low+(high-low)*i/40 for i in range(41)]:
        cases.append({'kernel': 'curve', 'curve': name, 'input': [x],
                      'expected': invoke(0x1821fc440, registers=[(UC_X86_REG_RCX, heap)], floats=[0, x])})

for speed in [0, 50, 300, 600, 650, 700, 750, 800, 900]:
    uc.mem_write(heap, struct.pack('<3f', speed, 0, 0))
    cases.append({'kernel': 'airControlBlend', 'input': [speed],
                  'expected': invoke(0x18085fcb0, registers=[(UC_X86_REG_RDX, heap)])})
output = Path(a.out)
assert not output.resolve().is_relative_to(Path(__file__).resolve().parents[1])
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps({'source': source, 'method': 'isolated x64 emulation, pure kernels only',
                              'curves': curves, 'cases': cases}, indent=2))
print(json.dumps({'cases': len(cases), 'out': str(output)}))
