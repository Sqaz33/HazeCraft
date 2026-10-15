"""Export character-independent research profiles from a pinned, owned reference.

These files are research inputs, not a declaration that the runtime port is complete.
Every added default has a schema field and a constructor store in the pinned client.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path
import re
import struct

from kv3 import loads

p = argparse.ArgumentParser()
p.add_argument('--reference', default='D:/DeadlockHazeLab/reference/25939744')
p.add_argument('--evidence', default='D:/DeadlockHazeLab/evidence')
p.add_argument('--out', default='D:/DeadlockHazeLab/research/25939744/movement')
a = p.parse_args()
reference, evidence, output = Path(a.reference), Path(a.evidence), Path(a.out)
assert not output.resolve().is_relative_to(Path(__file__).resolve().parents[1])
source = json.loads((reference / 'source.json').read_text())
assert source['build'] == '25939744', 'Schema addresses must be reverified for another build'
assert hashlib.sha256((reference / 'client.dll').read_bytes()).hexdigest() == source['clientSha256']
for name, digest in source['dataHashes'].items():
    assert hashlib.sha256((reference / name).read_bytes()).hexdigest() == digest, name
constructors = json.loads((evidence / '25939744-movement-constructors.json').read_text())
functions = json.loads((evidence / '25939744-movement-functions.json').read_text())
melee_constructor = next(f for f in json.loads((evidence / '25939744-combat-schema-functions.json').read_text())
                         if f['start'] == '0x18023a050')
melee_schema_function = json.loads((evidence / '25939744-melee-vdata-schema.json').read_text())[0]
assert melee_schema_function['sourceSha256'] == source['clientSha256']
melee_schema = melee_schema_function['instructions']
constructors.append(melee_constructor)
constructors.extend(json.loads((evidence / '25939744-generic-and-mantle-default-constructors.json').read_text()))
assert all(f['sourceSha256'] == source['clientSha256'] for f in constructors + functions)
instructions = {i['address']: (f, i) for f in constructors for i in f['instructions']}
jump_schema = next(f for f in functions if f['start'] == '0x180276180')['instructions']

def schema_offset(name, rows=jump_schema):
    index = next(i for i, row in enumerate(rows) if row.get('string') == name)
    for row in rows[index + 1:index + 9]:
        found = re.fullmatch(r'r9, \[r(?:di|si) \+ (0x[0-9a-f]+|[0-9]+)\]', row['operands'])
        if row['mnemonic'] == 'lea' and found:
            return int(found[1], 0)
    raise AssertionError('Missing schema offset: ' + name)

defaults = {}
def literal_float(ability, field, address, offset, schema=True, rows=jump_schema):
    function, row = instructions[address]
    match = re.fullmatch(r'(?:dword|qword) ptr \[r(?:bx|di|cx) \+ (0x[0-9a-f]+)\], (0x[0-9a-f]+)', row['operands'])
    assert row['mnemonic'] == 'mov' and match and int(match[1], 16) == offset, row
    if schema:
        assert schema_offset(field, rows) == offset
    bits = int(match[2], 16) & 0xffffffff
    value = struct.unpack('<f', struct.pack('<I', bits))[0]
    defaults.setdefault(ability, {})[field] = {
        'value': value, 'offset': hex(offset), 'store': address,
        'constructor': function['start'], 'clientSha256': source['clientSha256'],
    }

for field, address, offset in [
    ('m_flMaxSpeedDelta', '0x18023a4f3', 0x16d4),
    ('m_flCollidedWallMaxDist', '0x18023a53a', 0x1730),
    ('m_flDashJumpMantleDisableTime', '0x18023a508', 0x1700),
    ('m_flDashJumpExtraAirControlTime', '0x18023a512', 0x1704),
    ('m_flDashJumpExtraAirControlPercent', '0x18023a51c', 0x1708),
    ('m_flSlowedSlideJumpFactor', '0x18023a5b2', 0x17a8),
]:
    literal_float('citadel_ability_jump', field, address, offset)
literal_float('citadel_ability_dash', 'm_flDownwardAirDashSpeed', '0x180239ffb', 0x182c, False)
literal_float('citadel_ability_sprint', 'm_flSprintAccMS', '0x18023aa15', 0x14d8, False)
for field, address, offset in [
    ('m_flHeavyAttackRequiredHoldTime', '0x18023a098', 0x13f0),
    ('m_flLightAttackMaxHoldTime', '0x18023a0a2', 0x13f4),
    ('m_flChargeAnimDelayTime', '0x18023a0cd', 0x1410),
]:
    literal_float('citadel_ability_hold_melee', field, address, offset, rows=melee_schema)
members = json.loads((evidence / '25939744-mantle-generic-members.json').read_text())
assert members['sourceSha256'] == source['clientSha256']
for owner, field, address, offset in [
    ('CitadelGenericData_t', 'm_flInCombatWithHeroDuration', '0x1806a98d5', 0xd80),
    ('CitadelGenericData_t', 'm_flInCombatWithNonHeroDuration', '0x1806a98df', 0xd84),
    ('CitadelGenericData_t', 'm_flInCombatWithNeutralDuration', '0x1806a98e9', 0xd88),
    ('MantleType_t', 'm_flVerticalTime', '0x18023ccb7', 0x14),
    ('MantleType_t', 'm_flHorizontalTime', '0x18023ccd1', 0x18),
]:
    assert any(m['field'] == field and int(m['offset'], 16) == offset for m in members['fields'])
    literal_float(owner, field, address, offset, False)
mantle_schema_function = json.loads((evidence / '25939744-mantle-type-schema.json').read_text())[0]
assert mantle_schema_function['sourceSha256'] == source['clientSha256']
assert schema_offset('m_flMinHeight', mantle_schema_function['instructions']) == 8
mantle_ctor, zero_store = instructions['0x18023ccc3']
assert instructions['0x18023ccc1'][1]['operands'] == 'eax, eax'
assert zero_store['mnemonic'] == 'mov' and zero_store['operands'] == 'qword ptr [rcx + 8], rax'
defaults['MantleType_t']['m_flMinHeight'] = dict(value=0, offset='0x8', store=zero_store['address'],
    zeroRegisterOrigin='0x18023ccc1', constructor=mantle_ctor['start'], clientSha256=source['clientSha256'])
heroes = loads((reference / 'scripts/heroes.vdata').read_text(encoding='utf-8'))
abilities = loads((reference / 'scripts/abilities.vdata').read_text(encoding='utf-8'))
roles = {
    'jump': 'ESlot_Ability_Jump', 'dash': 'ESlot_Ability_Innate_1',
    'slide': 'ESlot_Ability_Slide', 'sprint': 'ESlot_Ability_Innate_2',
    'mantle': 'ESlot_Ability_Mantle', 'parry': 'ESlot_Ability_Innate_3',
    'melee': 'ESlot_Weapon_Melee',
}
all_abilities = {}
profiles = {}
for name, hero in heroes.items():
    if not isinstance(hero, dict) or not name.startswith('hero_') or 'm_mapStartingStats' not in hero:
        continue
    bindings = hero.get('m_mapBoundAbilities', {})
    selected = {role: bindings[slot] for role, slot in roles.items() if slot in bindings}
    for ability in selected.values():
        if ability not in abilities:
            continue
        if ability not in all_abilities:
            record = copy.deepcopy(abilities[ability])
            for field, proof in defaults.get(record.get('_class'), {}).items():
                record.setdefault(field, proof['value'])
            if record.get('_class') == 'citadel_ability_mantle':
                for mantle_type in record['m_vecMantleTypes']:
                    for field, proof in defaults['MantleType_t'].items():
                        mantle_type.setdefault(field, proof['value'])
            all_abilities[ability] = record
    profiles[name] = {
        'sourceHero': name, 'sourceBuild': source['build'],
        'startingStats': hero['m_mapStartingStats'], 'movementBindings': selected,
        'groundDashPositionCurve': hero.get('m_groundDashPositionCurve'),
        'unresolvedBindings': [v for v in selected.values() if v not in abilities],
    }
assert 'hero_haze' in profiles and not profiles['hero_haze']['unresolvedBindings']
output.mkdir(parents=True, exist_ok=True)
(output / 'profiles.json').write_text(json.dumps(profiles, indent=2), encoding='utf-8')
(output / 'shared-definitions.json').write_text(json.dumps(all_abilities, indent=2), encoding='utf-8')
(output / 'native-defaults.json').write_text(json.dumps(defaults, indent=2), encoding='utf-8')
generic = loads((reference / 'scripts/generic_data.vdata').read_text(encoding='utf-8'))
for field, proof in defaults['CitadelGenericData_t'].items():
    generic.setdefault(field, proof['value'])
(output / 'generic-data.json').write_text(json.dumps(generic, indent=2), encoding='utf-8')
(output / 'provenance.json').write_text(json.dumps(source, indent=2), encoding='utf-8')
print(json.dumps({'characters': len(profiles), 'completeProfiles': sum(not p['unresolvedBindings'] for p in profiles.values()), 'boundDefinitions': len(all_abilities),
                  'verifiedDefaults': sum(map(len, defaults.values())), 'out': str(output)}))
