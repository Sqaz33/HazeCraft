"""Validate and copy the owner's self-contained private preview; never uploads."""
import hashlib
import json
from pathlib import Path
import shutil
import zipfile

root = Path(__file__).resolve().parent.parent
version = next(line.split('=', 1)[1] for line in (root / 'gradle.properties').read_text().splitlines()
               if line.startswith('mod_version='))
source = root / 'build' / 'libs' / f'deadlock-haze-{version}.jar'
with zipfile.ZipFile(source) as jar:
    assert jar.testzip() is None, 'JAR checksum error'
    metadata = json.loads(jar.read('fabric.mod.json'))
    assert metadata['version'] == version
    assert metadata['depends']['minecraft'] == '~1.21.11'
    assert metadata['jars'], 'Bundled Fabric API missing'
    for nested in metadata['jars']:
        jar.getinfo(nested['file'])
    for entry in ['assets/haze/retail/haze.hzm.gz', 'haze/retail/hud.json',
                  'dev/local/haze/sim/movement/MovementClock.class',
                  'dev/local/haze/client/mixin/KeyboardMixin.class',
                  'dev/local/haze/client/mixin/LocalMovementMixin.class']:
        jar.getinfo(entry)
    assert json.loads(jar.read('haze/retail/profile.json'))['build'] == '25950029'
    jar.getinfo('haze/retail/movement-animation.json')
    jar.getinfo('dev/local/haze/sim/movement/JumpImpulse.class')
    jar.getinfo('dev/local/haze/sim/movement/CapsuleMover.class')
    jar.getinfo('dev/local/haze/client/HazeMeshRenderer.class')
    jar.getinfo('dev/local/haze/sim/IndexedTopology.class')
    jar.getinfo('dev/local/haze/client/DebugScreen.class')
    jar.getinfo('dev/local/haze/client/LocalClippedText.class')
    jar.getinfo('dev/local/haze/sim/CameraHeightBlend.class')
    jar.getinfo('dev/local/haze/sim/DashVisuals.class')
    jar.getinfo('dev/local/haze/client/LocalClippedImage.class')
    jar.getinfo('dev/local/haze/sim/LocalImageSlice.class')
    jar.getinfo('dev/local/haze/sim/DebugAction.class')
    jar.getinfo('dev/local/haze/client/DebugTools.class')
    jar.getinfo('dev/local/haze/HazeProtocol$Debug.class')
    jar.getinfo('dev/local/haze/sim/movement/MantleExit.class')
    mantle=json.loads(jar.read('haze/retail/profile.json'))['abilities']['citadel_ability_mantle']['m_vecMantleTypes']
    assert all(float(t['m_flSlideExitBoostOnGround'])==400 for t in mantle)
    jar.getinfo('dev/local/haze/sim/movement/MeleeController.class')
    jar.getinfo('dev/local/haze/sim/movement/MeleeVisuals.class')
    attacks=json.loads(jar.read('haze/retail/profile.json'))['abilities']['ability_melee_haze']['m_mapAttacks']
    assert all('m_flMovementAcc' in a and 'm_flAttackStateTime' in a for a in attacks.values())
destination = root / 'dist' / source.name
destination.parent.mkdir(exist_ok=True)
shutil.copy2(source, destination)
digest = hashlib.sha256(destination.read_bytes()).hexdigest()
print(json.dumps({'jar': str(destination), 'bytes': destination.stat().st_size,
                  'sha256': digest, 'version': version}, indent=2))
