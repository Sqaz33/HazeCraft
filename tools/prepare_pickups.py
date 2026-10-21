"""Derive the owner's private permanent-pickup table from decoded retail files."""
import argparse, hashlib, json
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('generic'); p.add_argument('misc'); p.add_argument('output')
a = p.parse_args()
generic = json.loads(Path(a.generic).read_text(encoding='utf8'))
misc = json.loads(Path(a.misc).read_text(encoding='utf8'))
loot = generic['m_BreakablePowerupLootParams']
ids = {key for table in loot['m_mapPickupsByMatchTimeMins'].values() for key in table}
result = {'loot': loot, 'pickups': {key: misc[key]['m_sModifer']['m_vecScriptValues'] for key in sorted(ids)},
          'sources': {Path(path).name: hashlib.sha256(Path(path).read_bytes()).hexdigest() for path in [a.generic, a.misc]}}
Path(a.output).write_text(json.dumps(result, indent=2), encoding='utf8')
print(f'Prepared {len(ids)} private pickup definitions')
