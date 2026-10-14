"""Convert owned Source sounds, keeping retail event membership, pitch and decibel levels."""
from pathlib import Path
import json,subprocess,math
from kv3 import loads
repo=Path(__file__).resolve().parents[1];lab=Path('D:/DeadlockHazeLab')
raw=loads((lab/'retail/soundevents/hero/haze.vsndevts').read_text())
player=loads((lab/'retail/soundevents/player.vsndevts').read_text())
raw.update({k:v for k,v in player.items() if any(k.startswith(s)for s in ['Player.Dash','Player.Jump','Player.WallJump','Player.CornerBoost','Player.Melee','Ability.Melee'])})
events={};meta={};missing=set()
for name,event in raw.items():
    identifier=name.lower()
    members=event.get('vsnd_files',event.get('vsnd_files_tail',[]))
    if isinstance(members,str):members=[members]
    sounds=[]
    for member in members:
        path=lab/'retail'/member.replace('.vsnd','.mp3').lower()
        if not path.exists():missing.add(member);continue
        target=repo/'src/main/resources/assets/haze/sounds/retail'/Path(member).relative_to('sounds').with_suffix('.ogg')
        target=Path(str(target).lower());target.parent.mkdir(parents=True,exist_ok=True)
        if not target.exists():subprocess.run(['C:/msys64/ucrt64/bin/ffmpeg.exe','-loglevel','error','-y','-i',str(path),'-c:a','libvorbis','-q:a','6',str(target)],check=True)
        sounds.append({'name':'haze:retail/'+Path(member).relative_to('sounds').with_suffix('').as_posix().lower(),'volume':min(1,10**(event.get('volume',0)/20)),'pitch':event.get('pitch',1)})
    if sounds:events[identifier]={'sounds':sounds};meta[identifier]=event
(repo/'src/main/resources/assets/haze/sounds.json').write_text(json.dumps(events,separators=(',',':')))
(repo/'src/main/resources/haze/retail/sound-events.json').write_text(json.dumps(meta,separators=(',',':')))
(lab/'evidence/audio-missing.json').write_text(json.dumps(sorted(missing),indent=2))
print(len(events),'retail sound events;',len(missing),'external shared/foley references not yet converted')
