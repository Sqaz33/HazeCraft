"""Extract omitted melee VData defaults from verified schema offsets and constructor stores."""
import json,re,struct
from pathlib import Path
lab=Path('D:/DeadlockHazeLab')
fields={'m_flHeavyAttackRequiredHoldTime':0x13f0,'m_flLightAttackMaxHoldTime':0x13f4,'m_flChargeAnimDelayTime':0x1410}
constructor=json.loads((lab/'evidence/melee-vdata-constructor.json').read_text())[0]
result={}
for field,offset in fields.items():
    expression=re.compile(r'dword ptr \[rdi \+ '+hex(offset)+r'\], (0x[0-9a-f]+)$')
    found=[]
    for instruction in constructor['instructions']:
        match=expression.fullmatch(instruction['operands'])
        if match:
            value=struct.unpack('<f',struct.pack('<I',int(match[1],16)))[0]
            found.append(dict(value=value,offset=hex(offset),instruction=instruction['address'],constructor=constructor['start'],className='CAbilityHoldMelee_VData'))
    assert len(found)==1,(field,found)
    result[field]=found[0]
out=dict(ability_melee_haze=result)
(lab/'evidence/native-ability-defaults.json').write_text(json.dumps(out,indent=2));print({k:v['value']for k,v in result.items()})
