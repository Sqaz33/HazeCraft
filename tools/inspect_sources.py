import pathlib, zipfile
root=pathlib.Path('D:/Projects/DeadlockHaze/.gradle/loom-cache')
dest=pathlib.Path('D:/DeadlockHazeLab/minecraft-sources')
for jar in root.rglob('*sources.jar'):
    if '/minecraftMaven/' not in jar.as_posix() and '/remapped_mods/' not in jar.as_posix(): continue
    with zipfile.ZipFile(jar) as z:
        for name in z.namelist():
            if not name.endswith('.java'): continue
            if '/minecraftMaven/' in jar.as_posix() or any(p in name for p in ('rendering/v1/','networking/v1/','lifecycle/v1/','keybinding/','gametest/','event/player/','entity/event/')):
                path=dest/name; path.parent.mkdir(parents=True,exist_ok=True); path.write_bytes(z.read(name))
print(dest)
