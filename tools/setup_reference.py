"""Create a separate Source2 write directory for an unmodified -insecure local reference."""
from pathlib import Path
original=Path('D:/SteamLibrary/steamapps/common/Deadlock/game/citadel/gameinfo.gi')
dest=Path('D:/DeadlockHazeLab/deadlock-sandbox/game/citadel')
text=original.read_text(encoding='utf8')
assert 'Game\t\t\t\tcitadel' in text and 'Game\t\t\t\tcore' in text
text=text.replace('Game\t\t\t\tcitadel',
    'Game "D:/DeadlockHazeLab/deadlock-sandbox/game/citadel"\n'
    '\t\t\tGame "D:/SteamLibrary/steamapps/common/Deadlock/game/citadel"')
text=text.replace('Game\t\t\t\tcore','Game "D:/SteamLibrary/steamapps/common/Deadlock/game/core"')
dest.mkdir(parents=True,exist_ok=True)
(dest/'gameinfo.gi').write_text(text,encoding='utf8')
(dest/'cfg').mkdir(exist_ok=True)
(dest/'cfg/autoexec.cfg').write_text('// isolated local reference; original user autoexec is not used\n')
print(dest/'gameinfo.gi')
