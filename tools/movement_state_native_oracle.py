"""Original x64 button transitions and percentage folding, with synthetic inputs.

No process attachment, OS services or original-code redistribution. Native result
buffers are read directly; final percentage composition is a separately traced
parent operation in 0x181307be0, outside this kernel's emulated scope.
"""
import argparse
import hashlib
import json
from pathlib import Path
import random
import struct

import pefile
from unicorn import Uc, UC_ARCH_X86, UC_MODE_64, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_RCX, UC_X86_REG_RDX, UC_X86_REG_R8, UC_X86_REG_RAX, UC_X86_REG_RSP, UC_X86_REG_XMM3

p = argparse.ArgumentParser()
p.add_argument('--reference', default='D:/DeadlockHazeLab/reference/25939744')
p.add_argument('--evidence', default='D:/DeadlockHazeLab/evidence/25939744-input-percent-and-physical-kernels.json')
p.add_argument('--out', default='D:/DeadlockHazeLab/research/25939744/movement/native-state-cases.json')
a = p.parse_args()
reference = Path(a.reference)
source = json.loads((reference / 'source.json').read_text())
data = (reference / 'client.dll').read_bytes()
assert source['build'] == '25939744'
assert hashlib.sha256(data).hexdigest() == source['clientSha256']
names = {'0x1812fc8c0', '0x180d409c0', '0x180d4c340', '0x180d4c280'}
names.add('0x180d4c400')
rows=json.loads(Path(a.evidence).read_text())
rows+=json.loads((Path(a.evidence).parent / '25939744-parry-modifiers-and-release-kernel-functions.json').read_text())
functions = [f for f in rows if f['start'] in names]
assert {f['start'] for f in functions} == names
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

def invoke(address, rcx, rdx, r8=0):
    rsp = stack + 0x1ff08
    write(rsp, '<Q', stop)
    for reg, value in [(UC_X86_REG_RSP, rsp), (UC_X86_REG_RCX, rcx), (UC_X86_REG_RDX, rdx), (UC_X86_REG_R8, r8)]:
        uc.reg_write(reg, value)
    uc.reg_write(UC_X86_REG_XMM3, struct.unpack('<I', struct.pack('<f', 100))[0])
    uc.emu_start(address, stop, count=10000)
    assert uc.reg_read(UC_X86_REG_RSP) == rsp+8, 'Instruction budget exceeded'
    return bool(uc.reg_read(UC_X86_REG_RAX) & 0xff)

def state(mask):
    down, changed, history = struct.unpack('<3Q', uc.mem_read(heap+8, 24))
    return int(bool(down & mask)) | (int(bool(changed & mask)) << 1) | (int(bool(history & mask)) << 2)

buttons = []
for bit in [0, 31, 63]:
    mask = 1 << bit
    for initial in range(8):
        for pressed in [False, True]:
            for offset, flag in [(8, 1), (16, 2), (24, 4)]:
                write(heap+offset, '<Q', mask if initial & flag else 0)
            invoke(0x180d409c0, heap, mask, int(pressed))
            buttons.append(dict(bit=bit, initial=initial, down=pressed, expectedState=state(mask),
                expectedPressed=invoke(0x180d4c340, heap, mask),
                expectedReleased=invoke(0x180d4c400, heap, mask),
                expectedDoublePressed=invoke(0x180d4c280, heap, mask)))
assert next(c for c in buttons if c['initial']==0 and c['down'])['expectedState'] == 3
assert next(c for c in buttons if c['initial']==4 and c['down'])['expectedDoublePressed']

percentages = []
rng = random.Random(25939744)
sequences = [[], [0], [-25], [-25]*2, [-25]*3, [-25]*4, [10, -25], [-25, 10],
             [100], [-100], [20, 30, -25, -50], [-125, -125], [125]]
sequences += [[rng.uniform(-80, 80) for _ in range(rng.randrange(1, 10))] for _ in range(64)]
for values in sequences:
    src, positive, negative = heap+0x100, heap+0x200, heap+0x300
    uc.mem_write(positive, bytes(32))
    uc.mem_write(negative, bytes(32))
    write(positive+16, '<f', 1)
    write(negative+16, '<f', 1)
    for value in values:
        write(src, '<I', 2)
        write(src+16, '<f', value)
        invoke(0x1812fc8c0, src, positive, negative)
    percentages.append(dict(input=values,
        positiveProduct=struct.unpack('<f', uc.mem_read(positive+16, 4))[0],
        negativeProduct=struct.unpack('<f', uc.mem_read(negative+16, 4))[0]))
output = Path(a.out)
assert not output.resolve().is_relative_to(Path(__file__).resolve().parents[1])
output.write_text(json.dumps(dict(source=source, method='isolated original x64; synthetic button planes and percentage values',
                                  buttons=buttons, percentages=percentages), indent=2))
print(json.dumps(dict(buttonCases=len(buttons), percentageCases=len(percentages), out=str(output))))
