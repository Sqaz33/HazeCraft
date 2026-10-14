"""Export original clip events and frame counts, without inventing notify times."""
import json
from pathlib import Path
from kv3 import loads
repo=Path(__file__).resolve().parents[1];lab=Path('D:/DeadlockHazeLab')
g=json.loads((lab/'model/haze.gltf').read_text());frames={a['name']:max(g['accessors'][s['input']]['count']for s in a['samplers'])for a in g['animations']}
durations={a['name']:a['duration']for a in json.loads((lab/'evidence/animation-inventory.json').read_text())}
tagged=loads((lab/'retail/scripts/tagged_sounds/haze_anim_sounds.vdata').read_text());clips={}
for path in (lab/'retail/models/heroes_staging/haze/clips').glob('*.vnmclip'):
    name=path.relative_to(lab/'retail').with_suffix('').as_posix()
    if name not in frames:continue
    source=loads(path.read_text());events=[]
    for track in source.get('m_eventTracks',[]):
        for event in track['m_events']:
            converted={'frame':event['m_flStartTime'],'durationFrames':event['m_flDuration'],'class':event['_class']}
            if event.get('m_eventClass')=='AE_TAGGED_SOUND':converted['sound']=tagged.get(event['m_KV']['name'],{}).get('m_soundEvent','')
            if event.get('m_ID'):converted['id']=event['m_ID']
            if event.get('m_rule'):converted['transitionRule']=event['m_rule']
            events.append(converted)
    animation=next(a for a in g['animations']if a['name']==name)
    extras=animation.get('extras',{})
    assert bool(source.get('m_additiveType'))==bool(extras.get('additive')),name
    clips[name]={'frames':frames[name],'duration':durations[name],'events':events,'sourceFile':source['m_sourceFilename'],
                 'additive':bool(extras.get('additive')),'additiveComposed':bool(extras.get('additive_composed')),
                 'additiveBaseFrame':source.get('m_nAdditiveBaseFrameIdx',-1)}
for animation in g['animations']:
    name=animation['name'];extras=animation.get('extras',{})
    if name not in clips:clips[name]={'frames':frames[name],'duration':durations[name],'events':[],
        'additive':bool(extras.get('additive')),'additiveComposed':bool(extras.get('additive_composed')),'sourceFile':''}
(repo/'src/main/resources/haze/retail/clip-metadata.json').write_text(json.dumps(clips,separators=(',',':')))
print(len(clips),'original clip event tracks;',sum(len(c['events'])for c in clips.values()),'events')

def objects(value):
    if isinstance(value,dict):
        yield value
        for child in value.values():yield from objects(child)
    elif isinstance(value,list):
        for child in value:yield from objects(child)
graphs=lab/'graphs/animgraphs/animgraph2/hero'
aim=loads((graphs/'hero_aim.vnmgraph+haze.vnmgraph').read_text())
remaps=[o for o in objects(aim)if o.get('_class')=='CNmGraphDocFloatRemapNode']
assert all(o['m_inputRange']==remaps[0]['m_inputRange']and o['m_outputRange']==remaps[0]['m_outputRange']for o in remaps)
aims={o['m_name']:o['m_pDefaultVariationData']['m_clip'].removesuffix('.vnmclip')for o in objects(aim)if o.get('_class')=='CNmGraphDocAnimationPoseNode'}
mask_graph=loads((graphs/'haze/haze_abilitymask.vnmgraph').read_text())
dagger=next(o for o in objects(mask_graph)if o.get('_class')=='CNmGraphDocStateNode'and o.get('m_name')=='Sleep_Dagger')
dagger_mask=next(o['m_maskID']for o in objects(dagger)if o.get('_class')=='CNmGraphDocBoneMaskNode')
transitions=[o for o in objects(mask_graph)if o.get('_class')=='CNmGraphDocGlobalTransitionNode'and o.get('m_name')=='Sleep_Dagger']
assert len(transitions)==1
skel=loads((lab/'skeleton/models/heroes_staging/haze/haze.vnmskel').read_text())
masks={m['m_ID']:dict(zip(m['m_primaryWeightList']['m_boneIDs'],m['m_primaryWeightList']['m_weights']))for m in skel['m_boneMaskSetDefinitions']}
variants={}
for variant in ['haze','haze_invisible','haze_out_of_combat']:
    data=loads((graphs/('hero_aim.vnmgraph+'+variant+'.vnmgraph')).read_text())
    variants[variant]={o['m_name']:o['m_pDefaultVariationData']['m_clip'].removesuffix('.vnmclip')for o in objects(data)if o.get('_class')=='CNmGraphDocAnimationPoseNode'}
out={'aims':variants,'headingInput':remaps[0]['m_inputRange'],'headingOutput':remaps[0]['m_outputRange'],
     'daggerMask':dagger_mask,'daggerBlendIn':transitions[0]['m_flDurationSeconds'],'masks':masks}
loco=loads((graphs/'haze/haze_abilityset_haspath.vnmgraph').read_text())
bindings={o['m_name']:o['m_pDefaultVariationData']['m_clip'].removesuffix('.vnmclip')for o in objects(loco)if o.get('_class')=='CNmGraphDocClipNode'}
spaces={}
for node in objects(loco):
    if node.get('_class')!='CNmGraphDocBlend2DNode':continue
    space=node['m_blendSpace'];clips_for_space=[bindings[n]for n in space['m_pointNames']]
    key=clips_for_space[0].split('/')[-1].removesuffix('_center')
    spaces[key]={'clips':clips_for_space,'points':space['m_points'],'triangles':space['m_indices'],'hull':space['m_hullIndices']}
out['locomotion']=spaces
(repo/'src/main/resources/haze/retail/animation-graph.json').write_text(json.dumps(out,separators=(',',':')))
print('Original aim graph:',len(aims),'pose nodes; dagger mask',dagger_mask)
