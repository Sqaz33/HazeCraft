"""Refresh a pinned owned build, rechecking every carried native default in current bytes/schema."""
import argparse,copy,hashlib,json,re,struct,subprocess,sys
from pathlib import Path
import capstone,pefile
from kv3 import loads

p=argparse.ArgumentParser();p.add_argument('--build',default='25950029');a=p.parse_args()
lab=Path('D:/DeadlockHazeLab');ref=lab/'reference'/a.build;oldref=lab/'reference/25939744'
out=lab/'research'/a.build/'movement';out.mkdir(parents=True,exist_ok=True)
source=json.loads((ref/'source.json').read_text());raw=(ref/'client.dll').read_bytes()
assert hashlib.sha256(raw).hexdigest()==source['clientSha256']
for name,digest in source['dataHashes'].items():assert hashlib.sha256((ref/name).read_bytes()).hexdigest()==digest
old=pefile.PE(str(oldref/'client.dll'),fast_load=True);new=pefile.PE(data=raw,fast_load=True)
base=old.OPTIONAL_HEADER.ImageBase;assert new.OPTIONAL_HEADER.ImageBase==base
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_64);md.detail=True
defaults=json.loads((lab/'research/25939744/movement/native-defaults.json').read_text())
names=sorted({name for fields in defaults.values() for name in fields})
refs=lab/'evidence'/f'{a.build}-default-field-refs.json';members=lab/'evidence'/f'{a.build}-default-field-members.json'
subprocess.run([sys.executable,str(Path(__file__).with_name('native_probe.py')),str(ref/'client.dll'),str(refs),'--pattern','^(?:'+'|'.join(map(re.escape,names))+')$'],check=True)
subprocess.run([sys.executable,str(Path(__file__).with_name('movement_schema_extract.py')),'--dll',str(ref/'client.dll'),'--refs',str(refs),'--out',str(members)],check=True)
schema=json.loads(members.read_text());assert schema['sourceSha256']==source['clientSha256']
mapped={}
def relocate(va):
 if va in mapped:return mapped[va]
 blob=old.get_data(va-base,192);mask=bytearray(b'\1'*len(blob));length=0
 for ins in md.disasm(blob,va):
  offset=ins.address-va;length=offset+ins.size
  if any(op.type==capstone.x86.X86_OP_MEM and op.mem.base==capstone.x86.X86_REG_RIP for op in ins.operands):
   for i in range(ins.disp_size):mask[offset+ins.disp_offset+i]=0
  if ins.group(capstone.CS_GRP_JUMP) or ins.group(capstone.CS_GRP_CALL):
   for i in range(ins.imm_size):mask[offset+ins.imm_offset+i]=0
  if ins.mnemonic in ['ret','int3']:break
 pattern=b''.join(re.escape(bytes([blob[i]])) if mask[i] else b'.' for i in range(length))
 found=[base+s.VirtualAddress+m.start() for s in new.sections if s.Characteristics&0x20000000 for m in re.finditer(pattern,s.get_data(),re.S)]
 assert len(found)==1,(hex(va),found)
 mapped[va]=found[0];return found[0]
for owner,fields in defaults.items():
 for name,proof in fields.items():
  constructor=int(proof['constructor'],16);current=relocate(constructor)
  store=current+int(proof['store'],16)-constructor
  oi=next(md.disasm(old.get_data(int(proof['store'],16)-base,15),int(proof['store'],16)))
  ni=next(md.disasm(new.get_data(store-base,15),store))
  assert oi.mnemonic==ni.mnemonic and oi.op_str==ni.op_str,(owner,name,oi.op_str,ni.op_str)
  matching=[m for m in schema['fields'] if m['field']==name and m['offset']==proof['offset']]
  assert matching,(owner,name,proof['offset'])
  if 'zeroRegisterOrigin' in proof:
   zero=current+int(proof['zeroRegisterOrigin'],16)-constructor
   zi=next(md.disasm(new.get_data(zero-base,15),zero));assert zi.mnemonic=='xor' and zi.op_str=='eax, eax'
   proof['zeroRegisterOrigin']=hex(zero)
  else:
   immediate=next(op.imm for op in ni.operands if op.type==capstone.x86.X86_OP_IMM)
   assert struct.unpack('<f',struct.pack('<I',immediate&0xffffffff))[0]==proof['value']
  proof.update(store=hex(store),constructor=hex(current),clientSha256=source['clientSha256'],schema=matching,
               verification='Current named schema offset and exact constructor store, re-located from independently pinned baseline')
# Newly audited omitted members. Prove the current named offset and constructor
# immediate directly; these are not copied under a newer hash.
drag_schema=json.loads((lab/'evidence'/f'{a.build}-drag-schema.json').read_text())
dash_ctor=next(f for f in json.loads((lab/'evidence'/f'{a.build}-dash-drag-defaults-functions.json').read_text()) if f['start']=='0x180239d90')
for name,offset in [('m_flDuringDrag',0x1818),('m_flPostMaxDrag',0x1824)]:
 matching=[m for m in drag_schema['fields'] if m['field']==name and int(m['offset'],16)==offset];assert matching
 stores=[]
 for row in dash_ctor['instructions']:
  ins=next(md.disasm(new.get_data(int(row['address'],16)-base,15),int(row['address'],16)))
  if ins.mnemonic=='mov' and len(ins.operands)==2 and ins.operands[0].type==capstone.x86.X86_OP_MEM and ins.operands[0].mem.disp==offset and ins.operands[1].type==capstone.x86.X86_OP_IMM:
   stores.append((row,struct.unpack('<f',struct.pack('<I',ins.operands[1].imm&0xffffffff))[0]))
 assert len(stores)==1,(name,stores)
 row,value=stores[0]
 defaults['citadel_ability_dash'][name]=dict(value=value,offset=hex(offset),store=row['address'],constructor=dash_ctor['start'],clientSha256=source['clientSha256'],schema=matching,verification='Current named schema and immediate constructor store')
heroes=loads((ref/'scripts/heroes.vdata').read_text(encoding='utf-8'));abilities=loads((ref/'scripts/abilities.vdata').read_text(encoding='utf-8'))
roles=dict(jump='ESlot_Ability_Jump',dash='ESlot_Ability_Innate_1',slide='ESlot_Ability_Slide',sprint='ESlot_Ability_Innate_2',mantle='ESlot_Ability_Mantle',parry='ESlot_Ability_Innate_3',melee='ESlot_Weapon_Melee')
profiles={};definitions={}
for name,hero in heroes.items():
 if not isinstance(hero,dict) or not name.startswith('hero_') or 'm_mapStartingStats' not in hero:continue
 selected={role:hero['m_mapBoundAbilities'][slot] for role,slot in roles.items() if slot in hero.get('m_mapBoundAbilities',{})}
 for key in selected.values():
  if key not in abilities:continue
  record=copy.deepcopy(abilities[key])
  for field,proof in defaults.get(record.get('_class'),{}).items():record.setdefault(field,proof['value'])
  if record.get('_class')=='citadel_ability_mantle':
   for mantle in record['m_vecMantleTypes']:
    for field,proof in defaults['MantleType_t'].items():mantle.setdefault(field,proof['value'])
  if record.get('_class')=='citadel_ability_hold_melee':
   for attack in record.get('m_mapAttacks',{}).values():
    for field,proof in defaults.get('AttackData_t',{}).items():attack.setdefault(field,proof['value'])
  definitions[key]=record
 profiles[name]=dict(sourceHero=name,sourceBuild=source['build'],startingStats=hero['m_mapStartingStats'],movementBindings=selected,
                     groundDashPositionCurve=hero.get('m_groundDashPositionCurve'),unresolvedBindings=[v for v in selected.values() if v not in abilities])
generic=loads((ref/'scripts/generic_data.vdata').read_text(encoding='utf-8'))
for field,proof in defaults['CitadelGenericData_t'].items():generic.setdefault(field,proof['value'])
cvars=json.loads((lab/'evidence'/f'{a.build}-package-cvar-defaults.json').read_text())
assert all(v['sourceSha256']==source['clientSha256'] for v in cvars.values())
for name,data in [('native-defaults',defaults),('profiles',profiles),('shared-definitions',definitions),('generic-data',generic),('cvars',cvars),('provenance',source)]:
 (out/(name+'.json')).write_text(json.dumps(data,indent=2))
print({'build':a.build,'defaults':sum(map(len,defaults.values())),'characters':len(profiles),'cvars':len(cvars),'out':str(out)})
