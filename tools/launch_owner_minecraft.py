"""Replay the owner's Fabric launch locally, keeping account arguments in memory.

TLauncher logs flatten arguments, including empty properties and account fields.
Never print the reconstructed command line or commit its contents.
"""
from pathlib import Path
import ctypes
import json
import re
import subprocess

home = Path.home()
game = home / 'AppData/Roaming/.tlauncher/legacy/Minecraft/game'
logs = home / 'AppData/Roaming/.tlauncher/logs'
output = Path('D:/HazeCraftLab/launch')
main = 'net.fabricmc.loader.impl.launch.knot.KnotClient'
candidates = []
for file in logs.glob('launcher.log*'):
    for index, line in enumerate(file.read_text(encoding='utf8', errors='replace').splitlines()):
        if main in line and '--gameDir' in line:
            candidates.append((file.stat().st_mtime, index, line))
if not candidates:
    raise SystemExit('No recorded owner Fabric launch found')
line = re.sub(r'\x1b\[[0-9;]*m', '', max(candidates)[2])
match = re.search(r'[A-Za-z]:\\.*?\\javaw\.exe', line)
if not match:
    raise SystemExit('No Java executable in recorded launch')
executable = match.group()
command = '"' + executable + '" ' + line[match.end():].strip()
argc = ctypes.c_int()
parser = ctypes.windll.shell32.CommandLineToArgvW
parser.argtypes = [ctypes.c_wchar_p, ctypes.POINTER(ctypes.c_int)]
parser.restype = ctypes.POINTER(ctypes.c_wchar_p)
pointer = parser(command, ctypes.byref(argc))
arguments = [pointer[i] for i in range(argc.value)]
free = ctypes.windll.kernel32.LocalFree
free.argtypes = [ctypes.c_void_p]
free(ctypes.cast(pointer, ctypes.c_void_p))
fixed = []
index = 0
while index < len(arguments):
    argument = arguments[index]
    if argument == '-XX:' and index + 1 < len(arguments):
        index += 1
        argument += arguments[index]
    # This flattened property otherwise makes Java start vanilla Main first.
    if argument == '-DFabricMcEmu=' and index + 1 < len(arguments) and arguments[index + 1] == 'net.minecraft.client.main.Main':
        index += 1
        argument += arguments[index]
    fixed.append(argument)
    if argument in ['--clientId', '--xuid'] and (index + 1 == len(arguments) or arguments[index + 1].startswith('--')):
        fixed.append('')
    index += 1
if Path(fixed[fixed.index('--gameDir') + 1]).resolve() != game.resolve():
    raise SystemExit('Recorded game directory does not match the owner installation')
classpath = fixed.index('-cp') if '-cp' in fixed else fixed.index('-classpath')
main_index = fixed.index(main)
if any(not argument.startswith('-') for argument in fixed[classpath + 2:main_index]):
    raise SystemExit('Unexpected bare JVM argument before Fabric main; refusing vanilla launch')
if not Path(executable).is_file():
    raise SystemExit('Recorded Java runtime is missing')
output.mkdir(parents=True, exist_ok=True)
try:
    with (output / 'owner-minecraft-console.log').open('wb') as stream:
        process = subprocess.Popen(fixed, cwd=game, stdin=subprocess.DEVNULL, stdout=stream,
                                   stderr=subprocess.STDOUT,
                                   creationflags=subprocess.DETACHED_PROCESS | subprocess.CREATE_NEW_PROCESS_GROUP)
except OSError as error:
    raise SystemExit('Minecraft could not start: ' + type(error).__name__) from None
(output / 'last-launch.json').write_text(json.dumps({'pid': process.pid}), encoding='utf8')
print('Fabric Minecraft started directly; PID', process.pid)
