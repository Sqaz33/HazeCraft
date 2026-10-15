"""Freeze locally owned movement inputs and their hashes outside the mod repository."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

from kv3 import loads

p = argparse.ArgumentParser()
p.add_argument('--game', default='D:/SteamLibrary/steamapps/common/Deadlock')
p.add_argument('--lab', default='D:/DeadlockHazeLab')
a = p.parse_args()
game, lab = Path(a.game).resolve(), Path(a.lab).resolve()
repo = Path(__file__).resolve().parents[1]
assert not lab.is_relative_to(repo), 'Owned reference data must stay outside Git'
manifest = game.parent.parent / 'appmanifest_1422450.acf'
manifest_before = manifest.read_bytes()
build = re.search(rb'"buildid"\s+"(\d+)"', manifest_before)[1].decode()
out = lab / 'reference' / build
out.mkdir(parents=True, exist_ok=True)
client = game / 'game/citadel/bin/win64/client.dll'
client_before = client.read_bytes()
client_hash = hashlib.sha256(client_before).hexdigest()
existing = out / 'client.dll'
if existing.exists():
    assert hashlib.sha256(existing.read_bytes()).hexdigest() == client_hash, 'Pinned client changed'
else:
    existing.write_bytes(client_before)
server = game / 'game/citadel/bin/win64/server.dll'
server_before = server.read_bytes()
server_hash = hashlib.sha256(server_before).hexdigest()
server_snapshot = out / 'server.dll'
if server_snapshot.exists():
    assert hashlib.sha256(server_snapshot.read_bytes()).hexdigest() == server_hash, 'Pinned server changed'
else:
    server_snapshot.write_bytes(server_before)
engine = game / 'game/bin/win64/engine2.dll'
engine_before = engine.read_bytes()
engine_hash = hashlib.sha256(engine_before).hexdigest()
engine_snapshot = out / 'engine2.dll'
if engine_snapshot.exists():
    assert hashlib.sha256(engine_snapshot.read_bytes()).hexdigest() == engine_hash, 'Pinned engine changed'
else:
    engine_snapshot.write_bytes(engine_before)
shutil.copy2(game / 'game/citadel/steam.inf', out / 'steam.inf')
env = os.environ.copy()
env['DOTNET_ROOT'] = str(lab / 'tools/dotnet')
env['DOTNET_CLI_TELEMETRY_OPTOUT'] = '1'
result = subprocess.run([
    str(lab / 'tools/dotnet/dotnet.exe'),
    str(lab / 'tools/vrf-master/Source2Viewer-CLI.dll'),
    '-i', str(game / 'game/citadel/pak01_dir.vpk'), '-o', '-', '-d',
    '-f', 'scripts/heroes.vdata_c,scripts/abilities.vdata_c,scripts/generic_data.vdata_c',
], capture_output=True, timeout=120, env=env)
assert result.returncode == 0, result.stderr.decode('utf-8', errors='replace')[-2000:]
parts = re.split(r'^--- (scripts/[^\r\n]+)\r?\n', result.stdout.decode('utf-8'), flags=re.M)
files = {}
for i in range(1, len(parts), 2):
    name, text = parts[i], parts[i + 1]
    if name not in ['scripts/heroes.vdata', 'scripts/abilities.vdata', 'scripts/generic_data.vdata']:
        continue
    text = text[text.index('<!-- kv3'):]
    decoded = loads(text)
    assert decoded, name
    path = out / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding='utf-8')
    files[name] = hashlib.sha256(path.read_bytes()).hexdigest()
assert len(files) == 3, files
assert manifest.read_bytes() == manifest_before, 'Steam updated during extraction; retry with a stable build'
assert hashlib.sha256(client.read_bytes()).hexdigest() == client_hash, 'Client changed during extraction'
assert hashlib.sha256(server.read_bytes()).hexdigest() == server_hash, 'Server changed during extraction'
assert hashlib.sha256(engine.read_bytes()).hexdigest() == engine_hash, 'Engine changed during extraction'
source = {
    'build': build, 'source': str(client), 'clientSha256': client_hash,
    'serverSource': str(server), 'serverSha256': server_hash,
    'engineSource': str(engine), 'engineSha256': engine_hash,
    'steamInf': (out / 'steam.inf').read_text(), 'dataHashes': files,
    'vpkDirectorySha256': hashlib.sha256((game / 'game/citadel/pak01_dir.vpk').read_bytes()).hexdigest(),
    'valuesSource': 'Owned installed VPK and native client/server/engine only; no replay or online gameplay dataset',
}
(out / 'source.json').write_text(json.dumps(source, indent=2), encoding='utf-8')
print(json.dumps({'build': build, 'clientSha256': client_hash, 'dataFiles': len(files), 'snapshot': str(out)}))
