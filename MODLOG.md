# Deadlock Haze in Minecraft — work journal

## Brief
Private, local-only mod; one runtime JAR. Haze toggle, original model and animations, gun, abilities against mobs, Fixation stacks, full movement, camera, HUD excluding roster/timer, default Deadlock keys, infinite boons and Alt upgrade cursor. Source only local installed Deadlock files/code. User requests autonomous decisions and software tests before a large MVP launch. No uploads.

## Source fingerprint
- Deadlock: `D:/SteamLibrary/steamapps/common/Deadlock`, Steam app 1422450, build 25917577.
- steam.inf: Client/ServerVersion 6779, SourceRevision 11123977, Oct 13 2026 13:33:25.
- Source 2, native x64 client.dll/server.dll; VPK content.
- Read files only. No modification, injection, or online service access to Deadlock.
- Private extracted assets and disassembly stay outside Git: `D:/DeadlockHazeLab`.
- Project: `D:/Projects/DeadlockHaze`, own Git repository, no remote.

## Route
Rebuild guest mechanics inside real Minecraft. Pure deterministic simulation; Minecraft provides collision and mob integration, renders the local converted mesh/skeleton/HUD. No separate running Deadlock required. Local asset conversion; no generated substitute art or wiki gameplay numbers. Fabric with bundled API modules in the runtime JAR. Loader/Minecraft are prerequisites.

## Verification policy
Local data provenance and hashes; numerical unit tests; compile; integration tests; then scripted large-MVP client scene. Tests must distinguish verified extracted constants from inferred/unrecovered native behavior. Never describe matching constants as proof of identical gameplay.

## Progress
- Read universal-modder mod-any-game, game-recon, mashup-mods, reverse-engineering, Source/Minecraft engine guides and oracle references.
- `um scan --list` and `um scan Deadlock --json` confirmed install.
- Source2Viewer CLI v20.0 downloaded to external lab (official release).
- No game launched or saves/configs changed.

## Next
Index VPK; extract hero/abilities/movement/camera/default bindings/Panorama/model/animations. Pin Minecraft tooling and create testable simulation.
