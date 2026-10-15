"""Export current native slot/button metadata with schema enum names, not guessed IDs."""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import pefile

p=argparse.ArgumentParser()
p.add_argument('--reference',default='D:/DeadlockHazeLab/reference/25939744')
p.add_argument('--out',default='D:/DeadlockHazeLab/research/25939744/movement/input-bindings.json')
a=p.parse_args()
reference=Path(a.reference)
source=json.loads((reference/'source.json').read_text())
data=(reference/'client.dll').read_bytes()
assert hashlib.sha256(data).hexdigest()==source['clientSha256']
evidence=reference.parents[1]/'evidence'
enum=json.loads((evidence/'25939744-ability-slot-enum.json').read_text())
assert enum['sourceSha256']==source['clientSha256']
pe=pefile.PE(data=data,fast_load=True)
base=pe.OPTIONAL_HEADER.ImageBase
table=0x183280ee0
names={entry['value']:name for name,entry in enum['entries'].items()}
slots=[]
for index in range(23):
    raw=pe.get_data(table-base+index*24,24)
    slot,category=struct.unpack('<2H',raw[:4])
    pointer,mask=struct.unpack('<2Q',raw[8:])
    assert slot==index and base<=pointer<base+pe.OPTIONAL_HEADER.SizeOfImage
    command=pe.get_data(pointer-base,100).split(b'\0')[0].decode('ascii')
    assert command.startswith('+') and mask and not mask&(mask-1)
    slots.append(dict(slot=slot,schemaName=names.get(slot),category=category,
        command=command,buttonMask=hex(mask),buttonBit=mask.bit_length()-1,
        inBusyQueueSlotMask=bool((0x7a1000>>slot)&1)))
by_name={r['schemaName']:r for r in slots if r['schemaName']}
assert by_name['ESlot_Ability_Jump']['slot']==12
assert by_name['ESlot_Ability_Jump']['buttonBit']==48
assert by_name['ESlot_Ability_Mantle']['buttonBit']==48
assert by_name['ESlot_Ability_ZipLine']['slot']==9
out=Path(a.out)
assert not out.resolve().is_relative_to(Path(__file__).resolve().parents[1])
out.write_text(json.dumps(dict(source=source,tableAddress=hex(table),
    note='Busy queue mask is only the first eligibility filter; action failure/timing predicates still apply. Rope/zipline remain excluded.',
    slots=slots),indent=2))
print(json.dumps({name:by_name[name] for name in ['ESlot_Ability_Jump','ESlot_Ability_Mantle',
    'ESlot_Ability_Innate_1','ESlot_Ability_Innate_3','ESlot_Weapon_Melee']},indent=2))
