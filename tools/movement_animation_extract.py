"""Read movement bindings from the current owned Haze variation graphs."""
import hashlib,json
from pathlib import Path
from kv3 import loads
root=Path(__file__).resolve().parents[1];lab=Path('D:/DeadlockHazeLab');ref=lab/'reference/25950029'
source=json.loads((ref/'source.json').read_text());graphs=ref/'graphs/animgraphs/animgraph2/hero'
def objects(value):
 if isinstance(value,dict):
  yield value
  for child in value.values():yield from objects(child)
 elif isinstance(value,list):
  for child in value:yield from objects(child)
variants={};hashes={}
for variant in ['haze','haze_invis']:
 name='hero_jump_inair.vnmgraph+'+variant+'.vnmgraph';raw=(graphs/name).read_bytes();hashes[name]=hashlib.sha256(raw).hexdigest();data=loads(raw.decode())
 clips={o['m_name']:o['m_pDefaultVariationData']['m_clip'].removesuffix('.vnmclip')for o in objects(data)if o.get('_class')=='CNmGraphDocClipNode'}
 comparisons=[o['m_values']for o in objects(data)if o.get('_class')=='CNmGraphDocIDComparisonNode']
 for direction in ['forward','right','left','back']:assert ['air_dash_'+direction,'wall_jump_'+direction] in comparisons
 assert ['ground_jump','air_jump'] in comparisons
 variants[variant]=clips
out=root/'src/main/resources/haze/retail/movement-animation.json'
out.write_text(json.dumps(dict(build=source['build'],vpkDirectorySha256=source['vpkDirectorySha256'],graphs=hashes,variants=variants),separators=(',',':')))
print({'build':source['build'],'variants':list(variants),'graphHashes':hashes})
