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

## MVP implementation (in progress)
- Host pinned: Minecraft 1.21.11, Fabric Loader 0.19.5, Loom 1.18.4 (build JVM 25, runtime target 21), bundled Fabric API 0.141.6.
- VRF release 20.0 failed on October KV3 type 26. Built official current master externally with portable .NET SDK 10.0.402; it supports UINT32_ZERO/ONE and reads original archives. Initial RED2 omission workaround is documented but final model/data use unmodified VPK through master.
- Extracted Haze hero and ability tables, all common bound movement abilities, default key template, model camera settings, Panorama layout/CSS/icons, weapon/ability audio.
- Static PE analysis extracts named float convar constructor defaults from client.dll (e.g. gravity 800, hero FOV 90). Evidence includes constructor, reference and float-load addresses outside Git.
- Original Haze mesh: 6 primitives, 124 nodes, 3 skins, 444 clips. Lossless animation stream and source textures packed privately (~15.7 MB). No keyframe reduction; decoded all clips in tests.
- Implemented pure weapon, ability, Fixation and movement classes; Fabric payloads; server mob combat; local player controller; original skeletal renderer; custom camera/HUD; Alt upgrades; F6 toggle.
- Build and 13 meaningful headless tests PASS. First integrated-client test is running in disposable run/ world, internal control injection only.

## Known fidelity gaps — do not call this a finished exact port
- Movement formulas are a reconstruction from recovered data, not yet a traced native-equivalence proof. Sprint, corner boosts, down-dash, exact air acceleration/slide slope handling need work.
- Renderer loads all clips but state transitions/blends/aim/shoot overlays are not yet the full original animation graph. Shader/cloth/smoke effects differ from Source 2.
- HUD uses retail icons with provisional composition; full Panorama layout/font/frames remain to port.
- Weapon spread random distribution and Fixation order/proc-at-cap require native logic confirmation.
- Gun origin/aim parallax, native camera operations/soft collisions, damage type/resistance interactions need integration checks.
- Smoke third upgrade lifesteal/dispel and fixation slows are incomplete; bullet evasion and charge timing need completion.
- No real-client verification yet; no claim of identical Deadlock feel.

## Next
Read first client test logs/screenshots, fix actual runtime problems, complete animation overlays/HUD/assets/audio and missing mechanics, rerun relevant oracles. Keep all retail data outside Git and no upload/remote.

## Update 2026-10-14 19:20 — supersedes earlier verification status

- Latest headless build `D:/DeadlockHazeLab/evidence/tooltip-build.log` succeeds: **16 tests**. The previous 13 count was outdated. Tests cover the data, all 444 finite clips, opaque material alpha, original muzzle orientation, post-sleep debuff timing and camera sequence endpoints; these are not native-equivalence proofs.
- Three integrated client runs pass. Latest `client-muzzle-nativehud-run.log` checks rendering, Source mouse counts, Fixation, sleep, upgraded Smoke, Dance, dash, jump and Steve restoration. Real mouse injection confirms 100 raw counts -> 1.65 degrees (.75 sensitivity, .022 yaw) from owned Steam local settings. No OS input driving.
- Current visual screenshots are in project `run/screenshots`. The latest tooltip/background changes compile but still need a client hover/click check.
- Opaque albedo's Source material metadata alpha caused the missing body; only OPAQUE conversion now forces alpha 255. MASK transparency remains original.
- Model MDAT attachments recovered from `haze-model-blocks.txt`; original weighted offsets for muzzles and palm are in profile. Numerical tests establish exported forward **-X**, world/render rotation **90 - Minecraft yaw**. Earlier guessed rotations are superseded. Gun origins and camera aim parallax now use this evidence.
- Original modern idle and additive pitch aim poses, separate additive hand firing layers and original shoot_R/L bone masks are connected. Reload sampling no longer loops past its end. Full graph/IK/cloth/material parity remains unfinished.
- HUD now uses original SVG/PNG masks, frames, fonts and scoped CSS/localization (18,673 selectors). Native constructor defaults supply reticle sizes, opacity, colors and flags. Native UI functions prove **8 base + 4 locked slots**, two rows; provisional 16-slot rendering was corrected. Alt now uses original three upgrade strips and tooltip background/card assets. Tooltip layout/highlights/property icons and several HUD states still require work.
- Original Bullet Dance cast/channel camera fields and air-dash FOV sequence implemented. Camera spring math is a reconstruction; exact native lag, spring, zoom/unzoom and collision parity are not yet proven.
- Sleep resistance reduction and movement slow now start **on wake**, matching m_PostSleep* modifiers. Dagger uses actual Fixation rank for cap/duration. Mob slows use transient attributes and clear on expiry; sleep AI handling is dimension scoped. Smoke cleanse/charge cooldown display and Dance arrow evasion added. Native proc-at-cap/order, resistance and damage semantics remain unverified.
- 48 original audio events converted to OGG with original membership/pitch/levels. Mixer, occlusion, moving loops, precise animation notify scheduling and foley remain incomplete.
- Melee native schema offsets and constructor stores recover omitted defaults: heavy hold **.55s**, light max hold **.2s**, animation delay **.1s**. Evidence `melee-timing-native-refs.json`, `melee-vdata-constructor.json`, `native-ability-defaults.json`; `native_schema_defaults.py` extracts values. Release timing now uses .55 instead of the unrelated .28 parry window. Native charge phases and dash curves still need porting.
- Sprint VData constructor 0x18023aa00 sets m_flSprintAccMS, offset 0x14d8, to **.6**. Sprint virtual methods/combat references were extracted but sprint remains unimplemented until gating is recovered. Sources `sprint-virtuals.json`, `sprint-overrides.json`, `sprint-combat-functions.json`.
- Original particle graphs extracted under external `retail/particles/weapon_fx/haze` and `retail/particles/abilities/haze`; simulation/rendering not implemented yet. Tracers currently have no visual renderer.
- Original reload_idle clip doc has tagged sound events at source frames **32** and **46**, duration **1.7666999s**. Current midpoint sound scheduling is provisional and must be replaced.
- Deadlock local -insecure reference launch did not remain running. September owned Steam screenshots provide visual context only; no October live comparison has been established. Config/userdata backups were created with um beforehand.

### Remaining work, do not mark complete

1. Original effects/tracers, smoke/cloth/shader and animation/audio notify timing.
2. Full animation state transitions, direction blends, turns, IK and movement graph integration.
3. Sprint/combat delay, exact air acceleration, down-dash/corner boosts, slope slides, heavy melee phases/curves; usable physical zipline/rope provisioning (current line list is internal only).
4. Camera lag, ADS transitions and native camera math/collision verification.
5. Complete HUD states, original overhead Fixation icon/count and tooltips, modifier displays and minimap adaptation if included. No guessed substitute art or numbers.
6. Native gun RNG/spread and Fixation ordering/cap behavior; multiplayer reconciliation and validation.
7. Broader real-client obstacle/ADS/reload/Alt-click/low-health scenes, private asset manifest, final one-JAR delivery.

### Tool notes

Python with Pillow/numpy: `C:/Users/mset6/miniconda3/python.exe`; external lab Python has pefile/capstone only. `prepare_retail.py --profile-only` updates data without reconverting mesh. `hud_extract.py` packages scoped styles, fonts/masks/image dimensions and native inventory values. New SVGs must first be rasterized with external `tools/svg-raster`.
`native_class.py` reads MSVC RTTI/vtables, `native_fn.py` dumps unwind function ranges (leaf fallback may stop before alternate return branches), `native_vtable_refs.py` finds constructor refs, `native_schema_defaults.py` extracts verified constructor stores. All are offline/read-only against game files.
Never run Gradle builds concurrently. Continue in this same project. There is no Git remote and no upload authorization.

## Update 2026-10-14 — movement and model integrity priority

User explicitly prioritizes movement and an undistorted Haze model during movement and Sleep Dagger, then the remaining port. Keep this priority until the obstacle scenes pass. No claim of complete Deadlock equivalence.

- **Important correction to the 19:20 orientation note:** original aim clips are heading sweeps. Neutral heading samples their middle, not frame zero. The frame-zero workaround twisted the torso and led to a false forward-axis conclusion. With neutral aim, original Haze faces **+Z**; world/render rotation is **-Minecraft yaw**. Both muzzle attachments now have verified forward projection.
- Original `hero_aim.vnmgraph+haze*` graphs supply pose bindings and heading remap -90..90 -> 0..1. Original `haze_abilitymask` supplies `Ability_Dagger`, with .2s entry and the original fractional pelvis/spine weights. Dagger is a masked replacement over locomotion; its clock is independent of later gun-shot action notifications. Reload uses original Idle/Haspath/Crouch/Slide/InAir masks.
- Converted animations baked root travel into **both** independent skeleton roots (`root_motion`, `pelvis`). Resetting only `root_motion` caused the visible body to drift several meters away during running/dash and snap each cycle. The converter now subtracts the original trajectory from every skeleton root **before** action masks and direction blending. Physics applies displacement once. The original relative body motion remains intact. Additive auxiliary root basis is also corrected, with constant-basis assertions.
- Original locomotion Blend2D point coordinates, triangle indices, hull and resource bindings are extracted. Runtime samples actual local velocity, not only held keys, and blends the corresponding original clips. This implements the retail topology; quaternion blend and transition parity still need native confirmation.
- Standing movement uses original `m_vCollisionHullMins/Maxs` (32 x 72 Source units), not the model's 40 x 80 collision dimensions. Original 32-unit step height is applied. Crouch hull height still uses the camera-derived adaptation and needs native recovery.
- Native MantleType_t schema fields + constructor prove default vertical/horizontal times .2/.2, offsets 0x14/0x18. The converter fills only omitted values. Mantle now rises before traversing the ledge and cannot be overwritten by ordinary air movement in the entry tick. Evidence: `mantle-type-functions.json`, `mantle-type-constructors.json`, `native-mantle-type-defaults.json`.
- Real Minecraft collision exposed ground loss during horizontal dash: once vertical velocity reached zero, the next collision no longer reported floor contact, blocking slide. Ground dash now advances gravity and maintains floor contact. A headless regression reproduces the actual host collision semantics.
- Sprint now ramps using recovered .6m/s² bonus to Haze's +1.6m/s. Host combat classifications/delays remain an adaptation. Original tracer first slice and native ADS transition data were also connected before the movement-priority correction.
- **30 headless tests pass**, including 945 pose/action combinations, limb rigidity, all 444 tracks, body-origin bounds across dash/run/mantle, original blend-space vertices and hull, 40 dash direction/heading combinations, air dash, dash jump, dash slide, wall jump, blocked dash, staged mantle and floor-contact regression.
- `client-ground-contact-run.log` passes real client rendering, original mouse sensitivity, Fixation, sleep, Smoke, Dance, knife during running and jump, dash mid-frame body bound, dash-to-slide, 1-block-wide / 2-block-tall corridor, and Steve restoration. Latest report `evidence/client-report.json`. Measured ~10.87m includes post-dash walking and is **not** the integral of the dash itself (pure test verifies 10m).
- Jump, slide-start and mantle animation clocks now begin at the physical action start, instead of sampling the global match clock. The modern original zipline loop replaces the legacy clip. **`client-mantle-run.log` passes**, including the real two-block mantle scene and every preceding integration check.

Still unfinished: full transition/inertial blend graph, IK/cloth/materials, native wall-jump bias/air acceleration/corner boosts/down-dash/slope behavior, rope and usable ziplines/boost provisioning, heavy melee phases, native camera/RNG/Fixation equivalence, full effects/audio and remaining HUD states. Preserve the user's requirement to resume these after movement integrity.


## Update 2026-10-14 — no rope or zipline; native wall-jump recovery

Latest explicit scope correction: no rope and no zipline in Minecraft. Removed both movement states, collision hooks, rope attachment logic, zipline registry, render/camera branches and unused zip boost input. The original extracted clip inventory remains a private source reference; it does not enable either mechanic. Earlier rope/zipline TODOs above are superseded.

- Offline PE constructor extraction now handles zero defaults (`xorps`/`pxor`) and stores appearing after the name reference. Original cvars prove one free wall jump, zero initial cost and .5 stamina on later wall jumps. Extractor output is included in the private profile, not guessed constants.
- Native wall-jump fatigue is clamped remap of elapsed time [0,1.25] to [.2,1], then rational Bias(.2). Replaced the earlier linear approximation. Evidence: `wall-jump-whole-functions.json`, `movement-cost-defaults.json` and native helpers 0x18025ae30 / 0x180292b60 / 0x1821e8ad0, in the external private lab.
- Added offline Ghidra target helper; its output stays outside Git in the private lab. `native_fn.py` now groups chained unwind fragments and follows leaf conditional branches. Provisional target names are navigation labels, not proof of function identity.
- `no-rope-build.log`: build passes after removal; **32 headless tests pass**. This check does not establish complete native movement parity.

Continue movement/model priority: down-dash, corner behavior, proper crouch hull, movement transition graph and remaining native air/slide behavior. Then resume remaining HUD/abilities/camera work. No rope/zipline work remains in scope.
