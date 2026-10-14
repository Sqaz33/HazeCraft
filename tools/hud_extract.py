"""Keep exact retail CSS declarations, font mappings and localization for the HUD port."""
from pathlib import Path
import json,re
from PIL import Image,ImageChops
from kv3 import loads
repo=Path(__file__).resolve().parents[1]
lab=Path('D:/DeadlockHazeLab')
out=repo/'src/main/resources/haze/retail'
rules={};defines={};sheets={}
for path in (lab/'retail/panorama/styles').rglob('*.css'):
    text=re.sub(r'/\*.*?\*/','',path.read_text(),flags=re.S)
    defines.update(dict(re.findall(r'@define\s+(\w+)\s*:\s*([^;]+);',text)))
    text=re.sub(r'@define\s+\w+\s*:\s*[^;]+;','',text)
    scoped={}
    # Only flat declaration rules; keyframes are excluded from layout tables.
    for m in re.finditer(r'([^{}]+)\{([^{}]*)\}',text):
        selectors=m[1].strip()
        if '@' in selectors or re.fullmatch(r'[\d%,\s]+',selectors):continue
        props={}
        for field in m[2].split(';'):
            if ':' in field:
                key,value=field.split(':',1);props[key.strip()]=value.strip()
        for selector in selectors.split(','):
            rules.setdefault(selector.strip(),{}).update(props)
            scoped.setdefault(selector.strip(),{}).update(props)
    sheets[path.relative_to(lab/'retail/panorama/styles').as_posix()]=scoped
localization={}
source=Path('D:/SteamLibrary/steamapps/common/Deadlock/game/citadel/resource/localization')
for path in source.rglob('*_english.txt'):
    if not any(x in path.name for x in ('citadel_heroes','citadel_main','citadel_attributes')):continue
    blob=path.read_bytes();text=blob.decode('utf-16') if blob.startswith((b'\xff\xfe',b'\xfe\xff')) else blob.decode('utf-8-sig')
    for key,value in re.findall(r'"((?:\\.|[^"\\])*)"\s*"((?:\\.|[^"\\])*)"',text):
        try:value=json.loads('"'+value+'"')
        except json.JSONDecodeError:continue
        localization[key]=value
skeleton=loads((lab/'skeleton/models/heroes_staging/haze/haze.vnmskel').read_text())
masks={m['m_ID']:dict(zip(m['m_primaryWeightList']['m_boneIDs'],m['m_primaryWeightList']['m_weights'])) for m in skeleton['m_boneMaskSetDefinitions']}
assets=repo/'src/main/resources/assets/haze/retail'
for path in (lab/'retail/panorama/images').rglob('*.png'):
    dest=assets/path.relative_to(lab/'retail/panorama/images');dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(path.read_bytes())
mask=Image.open(assets/'hud/healthbar/healthbar_backer_mask.png').convert('RGBA').resize((66,212),Image.Resampling.LANCZOS)
fill=Image.open(assets/'hud/healthbar/healthbar_fill_texture_png.png').convert('RGBA').resize((66,212),Image.Resampling.LANCZOS)
fill.putalpha(mask.getchannel('A'));fill.save(assets/'hud/healthbar/health_fill_masked.png')
sizes={path.relative_to(assets).as_posix():list(Image.open(path).size) for path in assets.rglob('*.png')}
native_funcs=json.loads((lab/'evidence/upgrades-inventory-functions.json').read_text())[2]['instructions']
def native_constant(address):
    instruction=next(i for i in native_funcs if i['address']==address)
    assert instruction['mnemonic']=='mov' and instruction['operands'].startswith('eax, ')
    return int(instruction['operands'].split(', ')[1],0)
native_ui=dict(baseItemSlots=native_constant('0x181c32909'),lockedItemSlots=native_constant('0x181c32940'))
(out/'hud.json').write_text(json.dumps(dict(rules=rules,sheets=sheets,defines=defines,localization=localization,masks=masks,imageSizes=sizes,nativeUi=native_ui),separators=(',',':')),encoding='utf8')
print(len(rules),'selectors',len(localization),'localized strings',len(masks),'original bone masks')
