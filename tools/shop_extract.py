"""Build a private enabled-item catalogue from the owner's pinned VData/localization."""
import sys,json,re,shutil,hashlib
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parent))
from kv3 import loads
lab=Path('D:/DeadlockHazeLab');repo=Path(__file__).resolve().parents[1];ref=lab/'reference/25950029'
abilities=loads((ref/'scripts/abilities.vdata').read_text());generic=loads((ref/'scripts/generic_data.vdata').read_text())
loc={}
for path in Path('D:/SteamLibrary/steamapps/common/Deadlock/game/citadel/resource/localization').rglob('*english.txt'):
 if any(s in str(path) for s in ['patch_notes','generated_vo']):continue
 blob=path.read_bytes();text=blob.decode('utf-16') if blob.startswith((b'\xff\xfe',b'\xfe\xff')) else blob.decode('utf-8-sig',errors='replace')
 for key,value in re.findall(r'"([^"\n]+)"\s*"((?:\\.|[^"\\])*)"',text):loc[key]=value.replace('\\n','\n')
def yes(value):return value is True or str(value).lower() in ('1','true')
records=[]
for key,data in abilities.items():
 if not isinstance(data,dict) or data.get('m_eAbilityType')!='EAbilityType_Item' or data.get('m_iItemTier') not in ['EModTier_1','EModTier_2','EModTier_3','EModTier_4'] or data.get('_not_pickable') or yes(data.get('m_bDisabled',False)):continue
 image=data.get('m_strShopIconLarge',data.get('m_strAbilityImage','')).replace('file://{images}/','');image=image.rsplit('.',1)[0]+'_'+image.rsplit('.',1)[1];icon=image+'.png'
 props=data.get('m_mapAbilityProperties',{});stats={}
 for modifier in data.get('m_AutoIntrinsicModifiers',[]):
  for name in modifier.get('m_vecAutoRegisterModifierValueFromAbilityPropertyName',[]):
   try:stats[name]=float(str(props.get(name,{}).get('m_strValue',0)).removesuffix('m'))
   except (ValueError,TypeError):pass
 tier=int(data['m_iItemTier'][-1])
 records.append(dict(id=key,name=loc.get(key,loc.get(key+'_name',key)),description=loc.get(key+'_desc',loc.get(key+'_Description','')),icon=icon,tier=tier,price=generic['m_nItemPricePerTier'][tier],category=data['m_eItemSlotType'],active=data.get('m_eAbilityActivation')!='CITADEL_ABILITY_ACTIVATION_PASSIVE',components=data.get('m_vecComponentItems',[]),stats=stats,data=data))
decoded=ref/'shop-stage7/decompiled/panorama/images';assets=repo/'src/main/resources/assets/haze/retail'
for path in decoded.rglob('*.png'):
 dest=assets/path.relative_to(decoded);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,dest)
for item in records:assert (assets/item['icon']).exists(),item['icon']
needed={k:v for k,v in loc.items() if any(k.startswith(item['id']) for item in records)}
(repo/'src/main/resources/haze/retail/items.json').write_text(json.dumps(dict(build='25950029',items=records,localization=needed),separators=(',',':')),encoding='utf-8')
print('Catalogue:',len(records),'enabled items,',sum(i['active'] for i in records),'active, original icons copied')
