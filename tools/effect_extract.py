"""Keep original Source particle graphs and selected decoded texture dependencies private."""
import json,shutil
from pathlib import Path
from kv3 import loads
repo=Path(__file__).resolve().parents[1];lab=Path('D:/DeadlockHazeLab');definitions={}
for path in (lab/'retail/particles').rglob('*.vpcf'):
    definitions[path.relative_to(lab/'retail').as_posix()]=loads(path.read_text())
(repo/'src/main/resources/haze/retail/particles.json').write_text(json.dumps(definitions,separators=(',',':')))
for name in ['materials/particle/beam_generic_2.png','materials/particle/sparks/spark_cone.png']:
    dest=repo/'src/main/resources/assets/haze/retail'/name;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(lab/'retail'/name,dest)
print(len(definitions),'original particle graphs packaged for the local renderer')
