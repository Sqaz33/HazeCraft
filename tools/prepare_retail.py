"""Package the owner's local retail data and lossless skeletal animation streams.
No data from wikis, third party gameplay APIs or generated art is used.
Output paths are Git-ignored. Do not publish the resulting private JAR.
"""
import pathlib, json, struct, gzip, shutil, hashlib, sys
import numpy as np
from PIL import Image
from kv3 import loads

LAB=pathlib.Path('D:/DeadlockHazeLab')
REPO=pathlib.Path(__file__).resolve().parents[1]
ASSETS=REPO/'src/main/resources/assets/haze/retail'
DATA=REPO/'src/main/resources/haze/retail'
ASSETS.mkdir(parents=True,exist_ok=True); DATA.mkdir(parents=True,exist_ok=True)

heroes=loads((LAB/'retail/scripts/heroes.vdata').read_text(encoding='utf8'))
abilities=loads((LAB/'retail/scripts/abilities.vdata').read_text(encoding='utf8'))
hero=heroes['hero_haze']
names=set(hero['m_mapBoundAbilities'].values())
names.update(('citadel_ability_zipline_boost','citadel_ability_melee_parry'))
native=json.loads((LAB/'evidence/native-float-defaults.json').read_text())
extra=LAB/'evidence/movement-crosshair-defaults.json'
if extra.exists():native.update(json.loads(extra.read_text()))
model_data=(LAB/'evidence/model-data.txt').read_text(encoding='utf8')
start=model_data.index('<!-- kv3')
model_kv=loads(model_data[start:])
model_keys=loads(model_kv['m_modelInfo']['m_keyValueText'])
attachments={}
blocks=LAB/'evidence/haze-model-blocks.txt'
if blocks.exists():
    mesh_text=blocks.read_text().split('--- Data for block "MDAT" ---',1)[1].split('--- Data for block ',1)[0]
    mesh_data=loads(mesh_text[mesh_text.index('<!-- kv3'):])
    attachments={a['key']:a['value']for a in mesh_data['m_attachments']}
settings={}
native_abilities={}
native_ability_file=LAB/'evidence/native-ability-defaults.json'
if native_ability_file.exists():native_abilities=json.loads(native_ability_file.read_text())
for ability,fields in native_abilities.items():
    for key,evidence in fields.items():abilities[ability].setdefault(key,evidence['value'])
settings_path=pathlib.Path('C:/Program Files (x86)/Steam/userdata/1035274860/1422450/local/cfg/user_convars_0_slot0.vcfg')
if settings_path.exists():
    import re
    allowed={'sensitivity','sensitivity_y_scale','zoom_sensitivity_ratio','m_pitch','m_yaw','citadel_camera_hero_fov'}
    for key,value in re.findall(r'"([^"\n]+)"\s*"([^"\n]+)"',settings_path.read_text()):
        if key in allowed or key.startswith('citadel_crosshair_'):
            try:settings[key]=float(value)
            except ValueError:pass
profile=dict(build='25917577',clientVersion=6779,revision=11123977,date='2026-10-13',
             hero=hero,abilities={k:abilities[k] for k in sorted(names) if k in abilities},
             nativeFloatDefaults=native,modelData=model_kv,modelKeys=model_keys,localInputSettings=settings,attachments=attachments,nativeAbilityDefaults=native_abilities)
(DATA/'profile.json').write_text(json.dumps(profile,separators=(',',':')),encoding='utf8')
if '--profile-only' in sys.argv:print('Updated pinned retail profile');sys.exit(0)

g=json.loads((LAB/'model/haze.gltf').read_text())
blob=(LAB/'model'/g['buffers'][0]['uri']).read_bytes()
types={5120:'i1',5121:'u1',5122:'<i2',5123:'<u2',5125:'<u4',5126:'<f4'}
components={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4,'MAT4':16}
def accessor(index):
    a=g['accessors'][index]; v=g['bufferViews'][a['bufferView']]
    n=components[a['type']]; dtype=np.dtype(types[a['componentType']])
    offset=v.get('byteOffset',0)+a.get('byteOffset',0)
    stride=v.get('byteStride',dtype.itemsize*n)
    result=np.ndarray((a['count'],n),dtype=dtype,buffer=blob,offset=offset,strides=(stride,dtype.itemsize)).copy()
    if a.get('normalized'):
        result=result.astype(np.float32)/np.iinfo(dtype).max
    return result

def quat_matrix(q):
    x,y,z,w=q; return np.array([[1-2*y*y-2*z*z,2*x*y-2*z*w,2*x*z+2*y*w,0],
    [2*x*y+2*z*w,1-2*x*x-2*z*z,2*y*z-2*x*w,0],
    [2*x*z-2*y*w,2*y*z+2*x*w,1-2*x*x-2*y*y,0],[0,0,0,1]],dtype=np.float32)

parents=[-1]*len(g['nodes'])
for i,n in enumerate(g['nodes']):
    for c in n.get('children',[]): parents[c]=i

out=gzip.open(ASSETS/'haze.hzm.gz','wb',compresslevel=6)
def ints(*a):out.write(struct.pack('>'+str(len(a))+'i',*a))
def floats(a):out.write(np.asarray(a,dtype='>f4').tobytes())
def string(s):b=s.encode('utf8'); ints(len(b)); out.write(b)
out.write(b'HZM1')
ints(len(g['nodes']))
for i,n in enumerate(g['nodes']):
    string(n.get('name','')); ints(parents[i]);
    # Matrix-only nodes here are identity within float rounding; retain their matrix.
    floats(n.get('translation',[0,0,0])); floats(n.get('rotation',[0,0,0,1])); floats(n.get('scale',[1,1,1]))
    floats(n.get('matrix',np.eye(4,dtype=np.float32).flatten(order='F')))
ints(len(g['skins']))
for s in g['skins']:
    ints(len(s['joints']),*s['joints']); floats(accessor(s['inverseBindMatrices']))
mesh_nodes={n['mesh']:(i,n['skin']) for i,n in enumerate(g['nodes']) if 'mesh' in n}
ints(sum(len(m['primitives']) for m in g['meshes']))
for mi,m in enumerate(g['meshes']):
    node,skin=mesh_nodes[mi]
    for p in m['primitives']:
        material=g['materials'][p['material']]
        tex=g['images'][g['textures'][material['pbrMetallicRoughness']['baseColorTexture']['index']]['source']]['uri']
        string(tex); ints(skin,node)
        attr=p['attributes']; pos=accessor(attr['POSITION']); norm=accessor(attr['NORMAL']); uv=accessor(attr['TEXCOORD_0'])
        joint=accessor(attr['JOINTS_0']).astype(np.int32); weight=accessor(attr['WEIGHTS_0'])
        index=accessor(p['indices']).astype(np.int32).ravel()
        assert index.max()<len(pos) and np.isfinite(pos).all()
        assert np.max(np.abs(weight.sum(axis=1)-1))<0.015
        ints(len(pos),len(index)); floats(pos); floats(norm); floats(uv)
        out.write(joint.astype('>i4').tobytes()); floats(weight); out.write(index.astype('>i4').tobytes())
        # Source PBR opaque albedo alpha is a packed material channel, not opacity.
        # glTF export retains it; Minecraft entity shaders multiply alpha by it.
        image=Image.open(LAB/'model'/tex).convert('RGBA')
        if material.get('alphaMode','OPAQUE')=='OPAQUE': image.putalpha(255)
        image.save(ASSETS/tex)

ints(len(g['animations']))
summary=[]
for anim in g['animations']:
    string(anim['name']); ints(len(anim['channels']))
    duration=0
    for c in anim['channels']:
        s=anim['samplers'][c['sampler']]; t=accessor(s['input']).ravel(); v=accessor(s['output'])
        assert s.get('interpolation','LINEAR') in ('LINEAR','STEP')
        assert len(t)==len(v) and np.isfinite(v).all() and (np.diff(t)>=0).all()
        ints(c['target']['node'],{'translation':0,'rotation':1,'scale':2}[c['target']['path']],len(t),v.shape[1],int(s.get('interpolation')=='STEP'))
        floats(t); floats(v)
        duration=max(duration,float(t[-1]))
    summary.append(dict(name=anim['name'],duration=duration,channels=len(anim['channels'])))
out.close()
(LAB/'evidence/animation-inventory.json').write_text(json.dumps(summary,indent=2))
for p in (LAB/'retail/panorama/images/hud/abilities/haze').glob('*.png'):
    shutil.copyfile(p,ASSETS/p.name)
manifest=dict(sourceBuild='25917577',meshPrimitives=sum(len(m['primitives']) for m in g['meshes']),
    nodeCount=len(g['nodes']),animationCount=len(summary),
    gltfSha256=hashlib.sha256((LAB/'model/haze.gltf').read_bytes()).hexdigest(),
    files=[dict(path=p.relative_to(ASSETS).as_posix(),sha256=hashlib.sha256(p.read_bytes()).hexdigest(),size=p.stat().st_size) for p in ASSETS.rglob('*') if p.is_file()])
(DATA/'asset-manifest.json').write_text(json.dumps(manifest,indent=2))
print(json.dumps({k:v for k,v in manifest.items() if k!='files'}));print('private assets:',sum(p.stat().st_size for p in ASSETS.rglob('*') if p.is_file()))
