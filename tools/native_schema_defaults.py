"""Extract omitted melee VData defaults from verified schema offsets and constructor stores."""
import json,re,struct,pefile
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
pe=pefile.PE('D:/SteamLibrary/steamapps/common/Deadlock/game/citadel/bin/win64/client.dll',fast_load=True)
base=pe.OPTIONAL_HEADER.ImageBase
def store(address,offset,register):
    raw=pe.get_data(address-base,10)
    assert raw[0]==0xc7 and raw[1]==register and struct.unpack_from('<I',raw,2)[0]==offset
    return dict(value=struct.unpack_from('<f',raw,6)[0],offset=hex(offset),instruction=hex(address))
out['citadel_ability_sprint']={'m_flSprintAccMS':store(0x18023aa15,0x14d8,0x83)}
generic={
    'm_flInCombatWithHeroDuration':store(0x1806a98d5,0xd80,0x81),
    'm_flInCombatWithNonHeroDuration':store(0x1806a98df,0xd84,0x81),
    'm_flInCombatWithNeutralDuration':store(0x1806a98e9,0xd88,0x81)}
(lab/'evidence/native-generic-defaults.json').write_text(json.dumps(generic,indent=2))
mantle_constructor=next(f for f in json.loads((lab/'evidence/mantle-type-constructors.json').read_text())if f['start']=='0x18023ccb0')
mantle={}
for field,offset in [('m_flVerticalTime',0x14),('m_flHorizontalTime',0x18)]:
    instruction=next(i for i in mantle_constructor['instructions']if i['mnemonic']=='mov'and re.fullmatch(r'(?:dword|qword) ptr \[rcx \+ '+hex(offset)+r'\], 0x3e4ccccd',i['operands']))
    mantle[field]={'value':struct.unpack('<f',struct.pack('<I',0x3e4ccccd))[0],'offset':hex(offset),'constructor':mantle_constructor['start'],'instruction':instruction['address'],'className':'MantleType_t'}
(lab/'evidence/native-mantle-type-defaults.json').write_text(json.dumps(mantle,indent=2))
(lab/'evidence/native-ability-defaults.json').write_text(json.dumps(out,indent=2));print({k:v['value']for k,v in result.items()})
