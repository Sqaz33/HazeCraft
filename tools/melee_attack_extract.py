"""Verify missing nested AttackData_t defaults in the pinned native constructor."""
import hashlib,json,struct
from pathlib import Path
import capstone,pefile
lab=Path('D:/DeadlockHazeLab');source=lab/'reference/25950029/client.dll'
digest=hashlib.sha256(source.read_bytes()).hexdigest()
schema=json.loads((lab/'evidence/25950029-melee-attack-fields.json').read_text())
pe=pefile.PE(str(source),fast_load=True);md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_64)
instructions=list(md.disasm(pe.get_data(0x239b2a,190),0x180239b2a));proofs={}
for field,offset in [('m_flMovementAcc',0xb0),('m_flAttackStateTime',0xb4)]:
 member=next(f for f in schema['fields'] if f['field']==field and f['offset']==hex(offset))
 store=next(i for i in instructions if i.mnemonic=='mov' and i.op_str.startswith(f'dword ptr [rbx + {hex(offset)}], '))
 value=struct.unpack('<f',struct.pack('<I',int(store.op_str.split(', ')[1],0)))[0]
 proofs[field]=dict(value=value,offset=hex(offset),store=hex(store.address),constructor='0x180239b2a',clientSha256=digest,schema=[member])
path=lab/'research/25950029/movement/native-defaults.json';defaults=json.loads(path.read_text());defaults['AttackData_t']=proofs;path.write_text(json.dumps(defaults,indent=2))
path=lab/'research/25950029/movement/shared-definitions.json';definitions=json.loads(path.read_text())
for definition in definitions.values():
 if isinstance(definition,dict) and definition.get('_class')=='citadel_ability_hold_melee':
  for attack in definition.get('m_mapAttacks',{}).values():
   for field,proof in proofs.items():attack.setdefault(field,proof['value'])
path.write_text(json.dumps(definitions,indent=2))
(lab/'evidence/25950029-heavy-melee-attack-defaults.json').write_text(json.dumps(proofs,indent=2))
print({k:v['value'] for k,v in proofs.items()})
