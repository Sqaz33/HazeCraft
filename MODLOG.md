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


## Update 2026-10-15 — full movement investigation before reimplementation

User narrows work to movement, excluding rope, zipline, fans and trampolines. User explicitly excludes the old local replays: use current game code/data and the mod source. New regression reports: stamina quantity feedback, double jump, lateral wall jumps, slide after dash-jump. Investigate the whole system, not only those failures. The replacement must be reusable across characters, with Haze as the first profile.

Installed Deadlock updated to build25939744 / client6782 / revision11127865. Frozen client is external `reference/25939744/client.dll`, SHA256 b7deb2d934614b2b9a4e3edf6cf7bfb954b9f1436aadb0bf07b24f07bb7d5515. Current VPK hero/movement definitions compare equal to previous extraction. Detailed confirmed facts and open research items are in MOVEMENT_RESEARCH.md.

Current native catalogue initially included 145 movement-related functions, selected from five ability classes and common movement services, with unwind-chain grouping. First targeted Ghidra pass exposed a decoder bug: leaf tail jumps followed neighboring functions and caused overlapping bodies. Corrected leaf traversal to follow conditional branches only; regenerated targets. Second pass and helper expansion succeed. Further targeted batches cover collision, native HUD charge update, sprint/slide callbacks, stamina reduction and type corrections. Full-module automatic analysis timed out and saved partial results; it is not a completed full analysis. Decompiled output remains outside Git.

Current reference snapshot now captures and hashes three VData files. Exported 68 character research profiles (67 complete bindings, Haze complete), 68 shared ability definitions and seven schema/constructor defaults. Native metadata yields 348 state, 248 modifier-value and 109 stat enum entries; do not mix their numeric IDs. Recovered native stamina charge geometry: 90-degree total arc starting at 135 degrees, with separate per-charge fill and state. Verified collision-model step/radius/top fields, and traced old/new solver dispatch. Details and remaining research criteria are in MOVEMENT_RESEARCH.md.

Added an offline x64 emulator oracle restricted to selected pure math functions. Successfully generated 198 original-code results with source hash and curves, outside the repository. No original game process was started. No runtime parity claim follows from these kernel cases. Research tooling also rejects out-of-image function addresses, preventing PowerShell's unquoted hexadecimal/scientific-notation ambiguity.

No new physics implementation or new JAR has been delivered at this stage. The 2026-10-14 JAR remains an incomplete baseline. No old replay analysis is used as a movement specification. External DemoFile tool clone was exploratory tooling only and is not part of the mod or reference evidence.

Further current-source audit pins server.dll too (SHA256 a0a92d06d5f4ffca7f5d604fac4a8b76c5eac3f288e0cdd8d2e7dcd43787a5e8), recovers the server jump dispatcher and four paths, verifies the in-combat wall-jump regeneration modifier, melee phase enum and three omitted melee defaults. Eleven defaults now apply by VData class across character profiles. Native mantle type selection and trajectory reveal additional differences from the old approximation. Full investigation remains open.

The 198 original-x64 math cases caught the missing spline y-domain clamp in Curve.java; fixed that shared helper and added native scalar remap. All 198 cases and all 33 software tests pass (25939744-math-regression-test.log). This does not validate the existing movement engine. No game was launched and no replacement JAR was delivered during this research stage.

Added a separate offline native mantle trajectory oracle: 396 cases cover four heights, three headings, three supplied tick intervals, and entry/phase/exit boundaries. The generic MantleTrajectory numeric helper matches them, without any Haze/ability dependency or runtime hookup. Verified GameTime_t's integer-register ABI directly from instructions, correcting the initial emulator call. All 34 software tests pass (25939744-mantle-regression-test.log); collision probing and full engine parity remain unfinished.

Further offline audit verifies the regular match interval .015625s (64 Hz) in CCitadelGameConfiguration::vtable[13], original held/pressed/released/double button planes and fractional command processing. Added reusable ButtonState and PercentageStack helpers with 48 original-x64 transition cases and 77 original-x64 percentage-fold cases. All 36 software tests pass (25939744-state-regression-test.log); 719 original-code cases now cover these helpers, math and mantle trajectory. These checks do not validate the old gameplay controller.

Recovered separate positive/negative stamina percentage products, ground acceleration's 400-Source-unit minimum, original support spring and plane-projected collision response. Dash-parry cancellation assigns a ground dash speed but only caps horizontal air speed, preserving vertical velocity. Detailed addresses, rules and open branches are in MOVEMENT_RESEARCH.md. Current extraction fills 17 verified defaults and exports generic combat data; engine2.dll is now pinned too. No old replay, live game process, new JAR or upload was used. Research and the replacement controller remain unfinished.

Research tooling now checks a successful Ghidra batch marker and can replace its own enclosed interior labels with a verified full function. The cvar extractor validates current source bytes, records their hash and tracks zero-valued constructor registers across calls. This recovered default-disabled character proxy and default-zero regular tick override without importing stale values.

Verified dash-parry gravity callback 0x180259870: a present modifier linearly recovers .3 -> 1 during the .4s glide window, then becomes absent. Added generic DashParryGravity and SupportSpring helpers. A new isolated original-x64 oracle verifies 120 gravity cases and 144 support cases, including speed thresholds, grounded/falling modes and support loss. The only injected dependency returns a supplied ground-entity boolean; no movement arithmetic is mocked. Focused parity tests pass (25939744-support-parity-test.log). Corrected support-field interpretation to a cached contact point, not a normal, and distinguished failed-jump error presentation from an actual input buffer. Runtime integration remains pending.

Full software regression passes: 38 tests, 983 original-code helper cases in total (25939744-support-regression-test.log). This is still preparation for replacing gameplay movement.

Added DoublePressTracker and 864 original-code command-history block cases: six bits, 64/128Hz, all bit-plane states, strict timing boundaries and unsigned wrap. Focused command/support comparisons pass (25939744-command-parity-test.log). Added CharacterMovementProfile without Haze/global Retail/host dependencies; its source-data regression resolves all 67 complete profiles with different character stats and melee bindings (25939744-profile-parity-test.log). No gameplay integration yet.

Recovered central action selection at 0x18087dd80 and automatic mantle's blocked-push qualification. Initial numeric slot interpretation was later disproven by schema metadata; see correction below before implementing input/queue behavior. Full details and remaining gating branches are in MOVEMENT_RESEARCH.md. Corrected a prior collision interpretation: there is no second normalization after removing inward contact components; preserve tangential speed without adding a full-speed wall boost. The capsule/contact adapter is still pending.

Schema extraction now rejects RIP-relative metadata as a member offset, accepts decimal member offsets and checks source-name bytes. Refreshed movement catalogue: 168 explicit registrations. Updated docs/CONTRACT.md to distinguish implemented limitations from the intended 64Hz/fractional-input, authoritative movement boundary. No game process, old replay, public upload or replacement JAR during this stage.

Full regression: all 40 software tests pass; 1847 original-code helper/block cases are available (`25939744-command-profile-regression-test.log`). The gameplay controller is still the old baseline; investigation and integration continue.

Correction from current native schema: Jump is slot 12, Mantle 10, ZipLine 9,
ClimbRope 11. Jump and mantle share bit 48; successful mantle runs first and
consumes that bit. Jump slot 12 is in the native .25s busy-queue eligibility mask,
but stamina/air-jump-limit failures do not qualify. The earlier slot-9-as-jump
and no-jump-queue interpretation is superseded. Added source-hashed reproducible
input-table extraction and verified original error-name table. Auto-mantle client
preferences default false in all three modes. Refreshed cvar catalogue has 119
entries with all prior 110 values unchanged. Cvar extraction now respects partial
register widths and stops at the name's registration call.


## 2026-10-15 — 0.2.0 movement preview, user-requested integration checkpoint

User explicitly requested packaging accumulated findings now and finishing the
movement model only on a later instruction. Stopped new native exploration.
Refreshed private movement profile from pinned build 25939744, all three VData
hashes and current source-hashed cvars; retained the prior converted model and
animations without claiming a new model extraction.

Connected reusable CharacterMovementProfile, 64Hz-boundary host clock, native
button-history helper and double-crouch history. Captured short keyboard taps;
blocked vanilla ground jump, auto-jump, crouch edge clipping and push-out in Haze.
Integrated ground minimum acceleration, clamped air acceleration, dash-jump
flight-time speed, falling-only coyote, air-jump directional addition, proactive
side-wall contacts, tangent-preserving flat-wall impulse and vertical fatigue,
stacking combat regen penalties, downward dash, parry cancel horizontal cap and
gravity recovery, slide steering/momentum, closest mantle animation selection
and verified trajectory timing/height correction. The HUD now displays separate
maximum-stamina charge arcs and partial fills.

Validation: all 52 JUnit tests pass; the suite includes the 1847 original-code
helper/block cases and original animation/skin bounds. New behavior checks cover
short taps, 64Hz host phase, strict double-crouch window, separate charge fills,
acceleration, coyote/air jump, tangent preservation, regen expiration, parry and
dash-jump landing into slide. Real Minecraft client smoke test passed with the
new mixins, model render, knife locomotion, dash/slide, normal jump, narrow
corridor, two-block mantle, abilities on mobs and Steve toggle. Logs:
25939744-movement-preview-build.log / 25939744-movement-preview-client.log in the
private lab evidence folder. Real-client scenarios do not establish exact
Deadlock movement parity.

Deferred: native capsule/support solver, authoritative movement reconciliation,
complete air-dash callback execution, wall corners/lateral movement ownership,
full slide/getup/session penalties and slopes, exact fractional input timings,
qualified busy-action queue and native mantle probes. UI radial drawing and host
collision remain adaptations. No upload or public remote was used.


## 2026-10-15 — 0.2.1 dash exit fix and requested fast stamina

User scope: fix stopping in mid-air after dash and enable fast stamina regen only.
Root cause: final partial dash displacement was divided by the whole substep,
turning a clipped movement fragment into a nearly zero exit speed; air exit then
multiplied that already reduced velocity by .2 again. Divide the displacement by
its active dash time, retaining prior velocity for numerically empty fragments,
and remove the extra air-exit impulse. Ordinary gravity/drag resume on release.
This is a focused preview correction, not completion of the native dash callback.

Add an explicit regeneration multiplier to Motion, default 1 for source-profile
simulation. The player client selects 10 as requested: .45s per charge / 1.35s for
three charges before penalties. Source exports, costs, maximum stamina, combat
penalties, model, animations and other movement rules remain unchanged.

Added four regressions covering final dash fragments at 64Hz/120Hz/50ms/small
intervals, the actual host clock partition, a ground dash leaving a ledge, resumed
gravity, retained coast speed and fast regeneration with maximum clamping.

Validation: full build passes all 56 software tests, including four new dash/regen
regressions. Package CRC check passes; exported game data and assets were not
edited. No new game launch was performed for this numeric fix. Log: private
evidence/25939744-dash-fix-build.log. Delivered one 0.2.1 JAR. The old preview
was no longer in dist, so a binary asset comparison against it was unavailable.


## 2026-10-15 — 0.2.2 rapid double jump and infinite slide ammo

User requested a quick double-jump fix, then added infinite ammo in slide.
The host input adapter collapsed two Space down transitions in one 50ms frame
into one pressed bit. Preserve up to two distinct jump presses in Controls and
MovementKeys, and dispatch them on separate MovementClock steps. Holding or
repeated duplicate down events does not add another jump. Pending jumps clear
when the host releases input on focus/screen/mode/world transitions. Jump
impulse, stamina costs and the one-air-jump limit remain unchanged.

Slide ammo logic already existed in Gun, but the server inferred slide from
Minecraft pose/velocity, which does not describe the client movement reliably.
Send actual slide state in a held flag and immediately update its transitions
with a dedicated state-only command. This preserves pending ability edges
rather than replacing or replaying them. Server gun simulation uses that flag
plus crouch, bypasses ammo spending and cancels reload during slide, allowing
fire from zero ammo. Exiting slide restores normal magazine/reload behavior.
HUD displays infinity during slide. Fast regen and the dash exit fix remain.

Added six regressions: rapid released/held double taps within one host frame,
second jump at multiple flight times, hold/duplicate-event rejection, short-frame
pending input, empty-magazine/reload slide firing, and normal ammo use after slide.
Extended the real-client scenario with rapid and delayed double jumps and
server-side firing from an empty, reloading magazine during slide.

Validation: full build passes all 62 software tests. Final real-client run passes
rapid and delayed double jumps, server-side slide fire from zero ammo with reload
active, existing model/knife locomotion, dash/slide, mantle, abilities and Steve
toggle. Inspected the slide screenshot with the infinity HUD indicator. Logs:
private evidence/25939744-jump-slide-fix-build.log and -client.log. Final one-JAR
package CRC validation passes. No public upload or full movement-model work.


## 2026-10-16 — resumed source investigation and air-jump root cause

User explicitly resumes full authentic movement research/implementation after
rejecting the double-jump fixes. Current Steam source advanced to 25950029/6785.
Pinned new binaries/data; added relocation-candidate tooling and current private
Ghidra function exports. Native air-jump oracle proves +75% bonus converts to
1.75, giving 525 rather than the old 225 Source up speed. Added JumpImpulse and
120-case native parity test; integrated corrected impulse and removed erroneous
air-dash -> dash-jump routing. The first build passes this new parity test but
fails an old test's obsolete 225 expectation; corrected that expectation.

Work is ongoing; no replacement JAR or full-fidelity claim yet. Preserve 10x
stamina regen, slide infinity and the model/root fix. Remaining immediate work:
current data/default migration, real keyboard-handler air-jump verification,
native drag/dash/wall/slide/math oracles, timestamped input, capsule/support
collision and full movement state transitions. Private outputs live under
D:/DeadlockHazeLab/decompiled/25950029 and research/25950029/movement. No live
Deadlock process or network gameplay is used.


Oct 16 checkpoint: new native air drag/acceleration/wall impulse helpers and
oracles, composed air-dash trace parity, current profiles/defaults, capsule plus
support/ground-box adapter, scoped server geometry validation, ordered timed
input, creative-flight isolation, jump event sounds and current graph bindings
are implemented. All 75 software tests pass. First real client passes including
true KeyboardHandler Space and native-strength bounce; expanded lateral wall
case currently fails. Added diagnostics and investigating it before packaging.
See MOVEMENT_RESEARCH.md Oct 16 checkpoint for corrected constants/qualification.
No source assets/binaries committed, no uploads, no live Deadlock attachment.


## 2026-10-16 — 0.3.0 movement preview

User requested immediate packaging before full movement completion. Frame-end
Space taps now apply actions without advancing physics. Lateral contact cannot
incorrectly enter forward mantle; capsule state resets after external teleports.
Preserved 10x regeneration, slide infinite ammo and model/root/knife fixes.
Movement data now uses build 25950029. Full fidelity remains unfinished.

Build and all 76 software tests pass. Final 49-second real Minecraft run passes
rapid/delayed keyboard Space, 13.332899 m/s air bounce, side-wall bounce,
early-air-dash jump, creative air jump without flight, dash-jump landing slide,
empty-magazine server slide fire, capsule corridor, two-block mantle, abilities,
knife locomotion and Steve toggle. Wall/slide screenshots inspected. This
supersedes the preceding failed wall checkpoint. Evidence: private
25950029-movement-final-client.log and client-report.json.

Package: dist/deadlock-haze-0.3.0-movement-preview.jar, 28,312,350 bytes.
SHA256 871e02db0c95bcb3e3c65cd7a3e63df40885c7bc1475e48ff5fb51baa1240c09.
No retail source assets/binaries committed, no uploads or live Deadlock attachment.


## 2026-10-16 — 0.3.1 performance fix and authorized local install

User accepted movement and reported roughly 5x FPS loss. Attached a 20-second
read-only JFR to existing Minecraft PID 28356; did not drive its controls or
close the user's game. Main-thread samples identify full unindexed Haze mesh
submission as the largest mod-side render cost. Replaced repeated triangles
with compact per-material indexed meshes: 314814 -> 49632 vertex submissions,
no triangle/UV/normal/animation reduction. Persistent native buffers are freed
on client shutdown. Bulk scalar emission removes per-corner JOML temporaries.
Movement controller unchanged. Added lab-only old/new renderer A/B measurement.

All 78 tests pass. Both real-client runs pass the full movement/abilities/model
scenario. Final uncapped A/B: draw 6.05 -> .88 ms, frame 8.56 -> 2.87 ms (117 ->
348 equivalent FPS). This is a lab measurement; no claim of 7x total FPS.
See PERFORMANCE.md for evidence, conditions, and backup locations.

Built/validated/installed 0.3.1-performance one-JAR to user's explicitly requested
TLauncher mods folder. Old 0.3.0 moved outside mods after Universal Modder backup.
Installed SHA256 04e099a03cc8fb2cb22e626d4aa8d00f85071a8b0ff7b5bc8ff9c5d9edf107d3.
No uploads or changes to the user's running world/settings. Restart loads new JAR.


## 2026-10-16 — 0.3.2 locomotion animation timing

User reports strong walking/running model jitter while other animations are fine.
Explicit instruction: no checks before/after; fix, replace installed mod, launch
directly without the launcher button. No tests, benchmark, QA client, screenshots,
post-launch log validation or package validation run for this version.

Code correction: regular locomotion previously sampled server snapshot time plus
client partial tick. Packet arrival can move that clock backwards/skip ahead.
Local LocomotionPlayback now advances an independent gait phase from the owning
Motion clock and client partial tick. It retains phase across walk/run families
with different clip durations instead of reinterpreting absolute server age.
Previous/current motion velocity is interpolated on the same partial tick as
player position/yaw, avoiding abrupt 20-Hz blend-space weight changes. Non-
locomotion animations retain their existing playback. Physics and indexed mesh
rendering unchanged. Playback caches clear on leaving movement/Haze/world.

Build command deliberately excludes test and check tasks. This is an untested
animation fix at the user's request; visual result is not claimed verified.
Install target: user's .tlauncher/legacy/Minecraft/game/mods, version 0.3.2-locomotion.
Previous 0.3.1 stored outside mods in private lab backups. Direct launch reuses
installed Fabric profile and the existing launch parameters without logging
credentials. No external upload.


## 2026-10-16 — 0.3.3 creative debug menu and ability cooldown toggle

User requests a button-opened creative-only debug menu and then adds ability
cooldown disabling. Explicitly no real-game test launch this turn. F8 is handled
as a consumed keyboard event (including repeat/release), independent of Haze
activation. DebugScreen closes when creative permission is lost and is unpaused.
Live movement/animation/resource/FPS diagnostics, refill, local movement reset,
stationary 10000-HP golem targets and cleanup of only the owner's tagged targets
within 64 blocks are available. Server rechecks creative on every debug command.

Cooldown toggle is authoritative server state, sent in snapshots for the label.
It refills readyAt and smoke charges without changing time, pending cast delays,
active effects or channel restrictions. Cleared automatically on leaving creative.
Added a regression for refill preserving pending dagger/smoke effects and charges.
All 79 software tests pass; no game process was launched or driven. Installed
one-JAR 0.3.3-debug-menu in the previously authorized TLauncher mods folder,
with 0.3.2 moved to private lab backups/tlauncher-before-0.3.3. Restart required.


## 2026-10-16 — requested staged repairs, stage 1: 0.3.4 channel mask

User supplies six issues and explicitly requests per-stage Universal Modder
recon, software tests, individual commit, install, direct user launch and ask
whether OK before continuing. No automated in-game control tests.

UM scan confirms owned Source 2 install. Read/extracted current build 25950029
VPK resources with CRC checks, decoded through Source 2 Viewer. Current XML
panorama/layout/ability_hud_elements/element_progress_bar_text: SplitLabelName
ability_name is mirrored by Image src=panel://ability_name, context=progress,
progress=clip_right under the rotated parent. Current CSS: parent rotateZ(-10deg),
48px Oracle, 16px padding; smoke overrides rotation 0deg / scale70%. Evidence and
hashes private reference/25950029/hud-stage1. Runtime refreshed relevant CSS tables.

Replaced axis-aligned screen scissor for channel fill with local glyph geometry
clipping before parent transform, UV interpolated at the cut. Existing Oracle
font glyphs/pipeline are used; no raster replacement. Uncut glyphs retain native
render path. Clipped polygons triangulate into GUI quads with a repeated corner.
Four regressions cover UV/area, endpoints/padding, rotated/scaled reveal boundary
and an italic partial triangle. All 83 software tests pass. No in-game QA driven.
Stage 1 installation targets usual TLauncher mods folder; old 0.3.3 retained in
private backups/tlauncher-before-0.3.4. Await manual user feedback for stage 2.


## 2026-10-16 — stage 2: 0.3.5 crouch camera transition

User authorizes stage 2. Universal Modder recon repeated. Pinned client 25950029:
constructor 18008e710 registers citadel_camera_height_approach_speed=80; sole
runtime global xref in 1805ea030 calls 1821effe0(current,target,strength,dt).
Important correction: 80 is a blend approach strength, not Source height/sec.
Native helper applies square-root remaining-time approach, full 0→1 duration
sqrt(1/80)≈.1118s. Audited stand/crouch pose getter 1805f5c30 and pose composition
1805ffe50, current named schema enum confirms SLIDING=70. Haze model heights
86/55 Source units preserved. Details in REPAIR_STAGES.md.

Added CameraHeightBlend with float operation ordering matching native helper.
CameraMixin advances it once per camera setup from local Motion time/partial;
HUD/FOV queries only read height. Crouch reversals start at current blend, same
or older timestamps do not advance state, owner/reset initializes cleanly.
Physical movement/collision controller unchanged.

Offline Unicorn runs original helper: 192 cases and 227 reversal-trace frames.
Added four regressions including source parity, continuity/reversals, repeated
reads, reset and render-frame partition. All 87 software tests pass. No automated
in-game control tests or QA launch. User installation replaces 0.3.4 with 0.3.5;
old JAR retained in private backups/tlauncher-before-0.3.5. Direct user launch
uses existing installed Fabric profile. Await user feedback before stage 3.


## 2026-10-16 — stage 2 correction: 0.3.6 ground-only crouch

User clarifies no character crouch in air. Repeated Universal Modder scan and
reviewed current native camera predicate: controller duck byte additionally
requires the same support query used in jump classification (1805ea030 /
18024c330). Host had used raw Ctrl regardless of grounded state. Ordinary crouch
now requires ground; camera and animation use Motion.crouchedPose (grounded &&
ducked). Clear ordinary crouch immediately after jumping, including zero-dt
input edges, and leaving ground. Forced compact geometry under low ceilings is
kept for collision safety, independent of airborne visual pose. Final host pose
syncs with motion. Raw Ctrl still drives downward dash and landing slide setup.

Four regressions cover airborne input/capsule/visual pose, zero-time jump from
crouch, ground crouch/downward dash and forced ceiling geometry. All 91 software
tests pass. No automated in-game input tests. Installed 0.3.6 in owner's usual
mods folder; 0.3.5 backed up under lab/backups/tlauncher-before-0.3.6. Direct
user launch for repeat review of stage 2; stages 3–6 remain queued.


## 2026-10-16 — stage 3: 0.3.7 dash visual heading

User authorizes stage 3 and adds diagonal airborne body facing. After asking for
speed, user explicitly reiterates separate stages. Only stage 3 implemented.
Universal Modder recon completed; current VPK graphs decoded with Source 2
Viewer. Haze ground graph binds one forward dash_ground; current in-air variation
binds four cardinal dash_air clips. Private graph evidence in
reference/25950029/dash-stage3/evidence.json.

Render-only DashVisuals derives exact world yaw from immutable dashDirection.
Ground single clip faces travel; directional air clip receives residual angle
(world travel minus that clip's cardinal offset). Cardinal air visuals preserved.
Applies only during active dash modes, leaving wall jump/action visuals intact.
Physics/input/camera unchanged. Four added regressions; all 95 software tests
pass. No gameplay automation. Installed one-JAR 0.3.7 to usual TLauncher mods,
old 0.3.6 saved outside mods under private backups/tlauncher-before-0.3.7.
Direct user launch and request manual confirmation; stages 4–6 remain queued.


## 2026-10-16 — 0.3.8-health-mask, этап 4

Этап 3 принят пользователем. Повторная разведка Universal Modder; текущие
health XML/CSS/SVG извлечены из build 25950029, декодированы Source 2 Viewer.
Данные вне Git: D:/DeadlockHazeLab/reference/25950029/health-stage4/evidence.json.
Исходные размеры 66×212, отступы 4/0/1/1, border margin 30, frame 68×220,
общий поворот −20°. Экранный scissor заменён дробной локальной геометрией
и UV без растяжения текстуры. Деления 30%/100% ×3px используют ту же маску.
Цвет рамки берётся из исходного CSS. Альфа подготовленной заливки сверена
программно с исходной opacity-mask в CSS-размере. 99 тестов прошли.
В игре управление не тестировалось; сборка запускается только пользователю.
