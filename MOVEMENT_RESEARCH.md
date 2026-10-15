# Movement research — current source and acceptance criteria

## Scope

Movement only until the system has been investigated and reproduced. Rope, zipline,
fans and trampolines are excluded. Old local match recordings are excluded by the
user's explicit correction; use the current installed game's code and data and the
mod's source. Haze supplies the first character profile, not rules hard-coded into
the movement engine. Other HUD/combat development is deferred except stamina
feedback needed to understand movement.

User-reported regression cases: stamina quantity display, double jump, lateral
wall jumps, and sliding after dash-jump. These are minimum checks, not the entire
scope or evidence that other actions are correct.

## Source version

- Steam build 25939744, Client/Server 6782, SourceRevision 11127865, Oct 14 2026.
- Frozen native reference: external `D:/DeadlockHazeLab/reference/25939744/client.dll`.
- SHA256: `b7deb2d934614b2b9a4e3edf6cf7bfb954b9f1436aadb0bf07b24f07bb7d5515`.
- Frozen authoritative server: external `reference/25939744/server.dll`, SHA256
  `a0a92d06d5f4ffca7f5d604fac4a8b76c5eac3f288e0cdd8d2e7dcd43787a5e8`.
  Server and client have separate address catalogues and decompiler projects.
- Universal Modder scan confirms Source 2/native x64. Investigation is offline/static.
- Current VPK was decompiled to stdout and compared: Haze and the bound jump,
  dash, slide, sprint, mantle, parry and melee definitions match the earlier
  extraction. This comparison does not establish code equivalence.
- 13 common movement function bodies match structurally at previous addresses
  +0x10 after relocation; call targets were normalized during this check and
  must be followed separately.
- Private native output: `D:/DeadlockHazeLab/decompiled/25939744/movement/`.
  Method labels `Movement_<address>` are navigation labels, not guessed semantics.
- Ownership/method catalogue and precise instructions:
  external `evidence/25939744-movement-owners.json` and
  `evidence/25939744-movement-functions.json`.

## Confirmed discrepancies and recovered values

### Dash-jump

The current Java code incorrectly divides distance by dash duration. Native VData
post-load code at 0x180276dd8 computes:

```
flightTime = 2 * dashJumpVerticalSpeed / sv_gravity
horizontalSpeedSource = (dashJumpDistanceMeters / .0254) / flightTime
```

Cached VData field +0x16f4 is used at 0x18024d73a, followed by direction and a pawn
movement multiplier. The gravity handle +0x183875980 is independently identified
by its `sv_gravity` constructor at 0x1800ea360. At the unmodified Haze defaults,
19/(2*425/800) = 17.88235294 m/s; the current code uses 19/.62 = 30.64516129 m/s.
This is a formula discrepancy, not an empirical trajectory comparison.

Schema and constructor also recover omitted defaults:

| Field | Offset | Value | Constructor store |
|---|---:|---:|---|
| m_flDashJumpMantleDisableTime | 0x1700 | .2 s | 0x18023a508 |
| m_flDashJumpExtraAirControlTime | 0x1704 | .25 s | 0x18023a512 |
| m_flDashJumpExtraAirControlPercent | 0x1708 | 30 | 0x18023a51c |
| m_flSlowedSlideJumpFactor | 0x17a8 | .75 | 0x18023a5b2 |

The first three are absent from the current profile. The jump dispatcher at
0x1802892e0 selects distinct functions for normal, air, wall and dash jumps:
0x18024d990 / 0x18024ce20 / 0x18024deb0 / 0x18024d340. The classifier and eligibility
conditions must be ported, not inferred from this list alone.

### Wall jump and corner behavior

Native 0x18024deb0 removes the inward normal component while retaining other
velocity; derives a direction from wall/corner information and input; remaps outward
speed between Haze's 225 and 310 Source units/s; adds vertical impulse and separately
controls lateral velocity. Fatigue scales vertical power, not the entire outward
velocity. Current Java replaces the full vector and scales outward speed by fatigue.

- Current corner boost cvar constructor: strength .6, 0x18004e4f0.
- Wall-jump power remaps elapsed [0,1.25] to [.2,1] then applies rational Bias(.2).
- First wall jump free; later jumps .5 stamina. These values alone are insufficient
  without eligibility, contact normals, input suppression and counter reset rules.
- Haze lateral input suppression .95 s and max lateral speed 100 are currently unused.

### Input and downward dash

- `in_button_double_press_window` is .22 s, constructor 0x180185c70/load 0x180185c7a.
- Original input stores pressed, released and double-pressed button state and has
  movement processing inside the command's fractional time steps.
- Current client polls at Minecraft's 20 Hz then repeats that input for six physical
  steps. This does not restore button transition timestamps within those 50 ms.
- Downward dash has a separate counter and .5 stamina cost.
- Omitted `m_flDownwardAirDashSpeed` defaults to 500 Source units/s (12.7 m/s),
  VData +0x182c, store 0x180239ffb. Activation sets a downward velocity, cancels the
  running air-dash state and enters post-drag. It is not an ordinary 8 m air dash
  merely pointed down. Precise impulse composition and eligibility remain to audit.

### Slide and air movement

Current adapter always reports slope 0 and uses one simple friction rule. Original
definitions and native methods contain separate paths for:

- dash-slide timing and failed timing (650 versus 450 Source units/s);
- dash-specific friction .175 for 1 s;
- flat ground grace .05 for .6 s, then .8;
- landing grace .75 s and late slide jump .25 s;
- uphill friction 1..1.5, downhill slope-dependent acceleration and speed limits;
- direction/angle limits and multiple ground probes;
- slide-jump air drag 2 for .5 s and leap penalty 100 over .2 s;
- get-up speed curve and .35 s busy period.

Native air acceleration and drag depend on direction, speed and current action.
Uniform `sv_airaccelerate * maxSpeed` plus constant air drag is not equivalent.
Ground dash can leave the ground; its airborne behavior and subsequent slide
lockouts need explicit transitions.

### Stamina and collision

- Current stamina regeneration is unconditional .222222 units/s. Original movement
  has reduction modifiers, jump counts/sessions, cooldowns and refunds to audit.
- Current HUD draws one arc with `min(1, stamina)`, so it cannot represent Haze's
  three available stamina units. Original charge widget has repeated charges,
  radial clips, charging/draining states and dash-jump anticipation.
- Current collision delegates Minecraft movement and zeros blocked axes. Original
  contact probes, plane clipping, stepping, ground adhesion, crouch transitions and
  mantle clearance are not reproduced by that adapter.
- Current crouch height is taken from camera data; it is not a recovered crouch hull.

### Native update order and newly verified boundaries (Oct 15)

The default walk dispatcher 0x18088c0d0, reached through movement-services virtual
33 (0x18087fa90), chooses ground/air drag, then ground/air acceleration, applies
external motion, ground response, gravity, surface response, overlap handling,
ceiling clearance, the physical sweep and final ground classification. Repeating
Minecraft friction/gravity at 120 Hz does not reproduce that sequence.

- 0x18085a710: airborne drag splits horizontal velocity along the pawn's facing and
  lateral axis. It can damp components differently depending on forward/side
  input and the permitted air speed. State 142 suppresses this procedure; state
  138 and move-data +0x10d gate a separate vertical-drag branch. The slow penalty
  helper is 0x180886c40; default enable=1, scale=.35. Property aggregation must be
  kept distinct from the minimum drag cvar (.2).
- 0x18085a190: air acceleration clamps wish speed to a speed-dependent 50..80
  Source units/s control limit (0x18085fcb0), unless a nonzero modifier air-speed
  override applies (0x18086d330). The acceleration also has modifier scaling and
  a separate displacement-controlled path. Current Java accelerates toward full
  hero move speed instead. Assembly proves the blend input is current velocity,
  remapped from 600..800 Source units/s to 0..1; it is not the input vector length.
- 0x180859ff0 / 0x18085b4d0: ground acceleration/friction. The latter reads surface
  friction, stop speed and pawn/attribute friction multipliers. Current Java
  omits those multipliers and slide's modifier override.
- 0x18085b6e0 applies gravity only when four move-data suppression flags are clear;
  it includes pawn gravity scale, move-data scale and a terminal-speed modifier.
  The generic velocity validity cvar is not automatically the fall-speed rule.

Original capsule construction 0x18086c6a0 calls 0x180869090. Model-schema offsets
are verified: step height +0x110, collision radius +0x114, collision height +0x118.
Haze values are 32, 20 and 80 Source units. 0x18086d210 is the **step-height getter**,
not a collision-height getter. 0x1808695b0 is the collision-height getter. The
capsule centers are z=(step+radius), z=max(top,step+radius), with radius supplied
separately. Forced duck (state 234) makes top=step; tucked legs (235) changes the
lower center by changing step to top-2*radius. The actual sweep and separate ground
probe must still be audited before interpreting these as the Minecraft AABB.

0x180881480 dispatches to the newer physical controller at 0x180882970 when its
cvar is enabled. That controller supplies capsule geometry, origin, combined
velocity and dt to 0x181739380 and consumes contacts afterward. The old and new
solver branches must not be combined into an invented hybrid. Generic view-vector
hull data and camera heights do not establish the physical capsule.
The flag is now identified: `citadel_use_character_proxy`, constructor
0x1800b2130, handle +0x1837da338, **default false**. No override was found in the
installed loose game cfg/vcfg/gi/text data. The custom contact solver in
0x180881480 is therefore the recovered default path, not the proxy controller.

Wall qualification is an independent overlap/shape query at 0x180295490, reached
from 0x180297b50. It expands the capsule by the VData collision distance (omitted
default 8, offset +0x1730, store 0x18023a53a). It filters contacts by
`abs(normal.z) <= .707`, weights normals by distance inside the expanded shell,
then normalizes their sum. With no new contact, it can retain an earlier normal
when at least .2 seconds have elapsed since the previous wall jump and distance
from the stored origin is less than radius/2. A follow-up sweep rejects one entity
type; that type still needs a verified enum name. 0x180297b50 classifies front,
left, right and back relative to the pawn basis; normal acquisition does not
require the player to move into the wall. Current Minecraft adapter does.

Normal jump multiplies the existing velocity by .75 only for state **71**
SLIDING_SLOW_GETUP, then replaces vertical velocity with the jump impulse. Native
jump completion 0x18024dd50 removes the grounded handle and assigns velocity once.
Landing/reset callbacks 0x180288ce0 / 0x180288db0 clear air-jump and wall-jump counts,
the jump-session counters and wall-jump timestamp. Coyote eligibility at
0x1802a0da0 additionally requires downward vertical velocity.

Slide control 0x18029ffe0 tracks crouch pressed time, whether the press occurred
airborne, sticky slide, requested slide, activation delay and a ground-start time.
Ground activation is normally >=350 Source units/s, or >=250 during ground dash;
qualified slopes have a separate entry path. Sustaining normally needs 170.
0x1802970f0 performs the actual enter/exit transition. Its pending dash-slide speed
adjustment is `targetSpeed*movementScale-currentHorizontalSpeed`, applied once by
the slide movement callback, rather than replacing the velocity every frame.
0x18029ea20 updates friction with dash, flat-ground, landing, uphill/downhill and
movement-time modifiers; 0x180253440 rejects probe samples outside their slope and
velocity-difference standard-deviation limits instead of averaging every sample.

The sprint acceleration property at 0x180299500 converts .6 **m/s²** to Source
units using .0254 (not a percentage). Sprint duration at 0x180299530 is elapsed
time since activation. 0x18028df50 gates it on input, combat, disabled state,
crouch/ground conditions and current attacks. 0x180298950 separately tests whether
the actor has reached the speed required for sprint presentation.

Down-dash eligibility 0x180242dd0 recognizes crouch double-press, toggle-crouch
double-press or a dedicated down-dash button. It uses an independent count and .5
cost. Normal dash requires nonzero directional input; the Java fallback to a
forward dash on neutral input is another discrepancy. 0x180283690 refunds the
previous air/dash jump cost when the next mantle (window .1) or excluded zipline
action (window .2) qualifies; the first free wall jump cannot refund a paid charge.
Only the mantle refund is in this port's scope.

The charge updater was traced from the Dash HUD lambda RTTI and verified direct
call site, not from the similarly named overhead widget:

- 0x181a5f4c0 rounds the maximum resource stat to the number of charge panels.
- Its arc starts at 135 degrees and spans **90 degrees total**, with 3-degree
  gaps for up to four charges, 2-degree gaps above four. Per-charge width is
  `(90 - (count-1)*gap)/count`.
- 0x181a865e0's lambda renders the background and a foreground sweep proportional
  to the charge fraction from 0x18029e2a0. That helper distinguishes full charges,
  the current fractional charge, exhausted charges and unavailable slots, with
  charging/draining/disabled classes. Count and fractional amount are separate.
- 0x1804aeeb0 reads resource-max stat 0x32; charge fraction also uses current full
  resource count and fraction. The current one-arc `min(1,stamina)` cannot match.

### Data and tooling audit

The pinned reference now includes the current `heroes`, `abilities` and
`generic_data` VData, with hashes checked before extraction. Research profiles
retain native bound-ability slots and common definitions: 68 character records,
67 with complete bindings; one test hero references an absent melee definition.
Haze's bindings are complete. These are research inputs, not a runtime rewrite.

The current client's native schema contains 348 verified modifier-state enum
entries. State words begin at +0x3d5c and their mirror at +0x3db4; +0x3d60 is word
one, so its bit number must be offset by 32. The schema-member extractor recovered
169 explicit member pointers without inferring names from numeric offsets.
Also recovered 248 modifier-value entries and 109 contiguous stat entries anchored
at `EStamina`. These are different enums: for example modifier-value 90 is
SPRINT_DURATION, while stat 90 is EItemCooldown. Call-site type determines the enum.
In particular normal jump's state argument `0x47` is decimal 71,
SLIDING_SLOW_GETUP; it is not decimal 47's movement activation restriction.

The full-module automatic analysis reached its time limit and saved partial
results. Targeted exports succeeded. A verified Windows stack-probe helper at
0x1824bf190 now uses Ghidra's `alloca_probe` fixup, avoiding false dynamic-stack
expressions in the collision solver. Native prototypes and conditional branches
still require assembly checks; generated C text alone is not verification.

An isolated Unicorn x64 harness executes four allowlisted original math kernels
(Bias, clamped remap, spline evaluation and air-control blend) plus the native
vector-length helper. It produced 198 source-hashed cases outside Git without
loading the DLL into Windows or launching/attaching to the game. Those cases are
an independent oracle for later port tests, not an end-to-end movement pass.
The first Java comparison caught an actual discrepancy: spline output was not
clamped to its declared y-domain. At slide-getup x=.15, the old Java evaluator
returned -20.011478 while original x64 returned -20. Fixed the shared evaluator's
domain clamp and added native clamped remap. All 198 oracle cases now pass, and
all 33 software regression tests pass (`25939744-math-regression-test.log`). This
is a numerical helper correction; the movement engine has not yet been replaced.

### Server cross-check, combat and melee movement

The server Jump execute method is 0x180299c60 (primary vtable index 327),
not the client's index 309. Comparing class-unique methods avoids confusing
inherited base methods with the jump dispatcher. It dispatches classifier
0x180253ce0 to normal 0x180255490, air 0x180254820, wall 0x180255a10,
and dash-jump 0x180254d60, then common velocity bookkeeping 0x180255860.
The air path has its own count, conditionally charges one stamina, and increments
the count; wall uses its separate free/paid count and the same fatigue/lateral rules.
The server additionally emits resource-consumption and presentation events.

Wall jump applies VData `m_DebuffModifier` (+0x1698) only while in combat:
client 0x18024deb0 calls 0x180875b50; server 0x180255a10 calls 0x180b53e40.
Client native registration at 0x180563e21 identifies pawn +0x13e0 as
`m_sInCombat`, type CCitadelRecentDamage. Both wrappers test current time against
the start/end interval at +0xc/+0x10 (client 0x181646b60, server 0x18096f1b0).
The original modifier is hidden, allows MULTIPLE instances, supplies -25% stamina
regeneration and lasts 5 seconds. The common percentage aggregation is now
verified below; the repeated negative-only case does multiply .75 per stack.

Native EMeleeHold_AttackState metadata identifies None=0, Charging=1,
GroundDashing=2, AirDashing=3, Attacking=4, SlideDashing=5. Movement callback
0x180284640 has distinct ground/air/slide paths, original movement-speed curves,
turn-rate and acceleration limits, gravity suppression and collision cancellation.
The old mod's instantaneous light melee does not reproduce these interactions.
Reverified current CAbilityHoldMelee_VData constructor 0x18023a050 and schema
0x180277840: required heavy hold .55s (+0x13f0), maximum light hold .2s
(+0x13f4), charge animation delay .1s (+0x1410). Profile extraction now applies
omitted defaults by original VData class, including all character melee bindings,
instead of tying them to ability_melee_haze. Eleven defaults are source verified.

### Mantle path and clearance

0x180248670 caches a probe by position and heading and calls 0x18029b950.
That path performs capsule clearance and ledge sweeps, then 0x180296550 chooses
the valid mantle type closest to its animation height within overlapping min/max
ranges. Picking the first max-height match, as the current mod does, is wrong.
0x1802435b0 also requires the input direction dot mantle-forward >= .7.
0x1802574c0 evaluates the trajectory: vertical and horizontal durations round to
the native tick interval; a Bias(.7) front/up correction reaches full weight
halfway through the vertical phase; horizontal travel begins after the vertical
phase. 0x1802851d0 applies the difference between two trajectory samples,
suppresses ordinary gravity, checks cancellation input, landing and interruption,
and limits exit speed. Current linear two-phase movement lacks these corrections.
The isolated x64 trajectory oracle now produces 396 original-code cases across
four heights, three headings, three supplied tick intervals and phase boundaries.
The independent reusable MantleTrajectory numeric helper passes all 396 cases;
all 34 software tests pass (`25939744-mantle-regression-test.log`). It is not yet
connected to gameplay. Assembly proved GameTime_t is passed as R8D bits, despite
the provisional decompiler prototype displaying float; the harness now uses the
verified ABI and asserts that time changes the result.
Remaining probe/cancellation branches and host collision are still under audit.

## Investigation checklist — do not mark complete from builds alone

For each mechanism recover input condition, priority, cost, cooldown, velocity
operation, modifier lifetime, contact requirements, cancellation, animation event
and exit/counter reset rules. Track source address/data key and unresolved branches.

1. Ground acceleration, stop, reverse, diagonal input and sprint gating.
2. Air acceleration/steering/drag, falling, terminal speed and loss of ground.
3. Ground jump, air jump, buffering, coyote window, ceiling and counters.
4. Ground dash, air dash, downward dash, steering and end momentum.
5. Dash-jump valid/early/late windows, direction, extra air control and mantle lockout.
6. Wall qualification, lateral/outward jumps, corner blending, suppression and fatigue.
7. Slide activation/boost, slopes, landing, dash and dash-jump entry, steering and exit.
8. Slide-jump, repeated slide jumps, reduced regen and cooldown/refund rules.
9. Mantle probing, auto-mantle, movement stages, clearance and interrupted state.
10. Movement interactions with melee, parry, aiming, firing, slowing and combat.
11. Stamina amounts, fractional regeneration and original charge presentation.
12. Input timing, player hull, collisions and host integration order.

## Planned reusable boundary, after investigation

- CharacterMovementProfile: original character stats plus shared movement definitions.
- MovementRules: common mechanics, no Haze class, model, global Retail singleton or
  ability IDs embedded in equations.
- MovementState: velocities, contacts, action phases, input times, counters and modifiers.
- MovementEnvironment: swept hull/contact/probe interface; Minecraft implements it.
- MovementModifiers: speed, acceleration, gravity, resource and action restrictions.
- MovementEvents: animation/camera/stamina presentation observes state changes.
- Haze integration supplies its profile, action modifiers and animation bindings.

Required checks cover combinations and boundaries, including all four user-reported
cases. The original regression tests are stability checks, not proof of native parity.
Research and implementation are still in progress.

## Further verified current-code findings (2026-10-15)

### Command timing and button edges

CCitadelGameConfiguration primary vtable 0x182725118, index 13, is the complete
function 0x180728d40. With experimental_citadel_tick_rate_override < 1 it returns
the literal .015625 seconds (64 Hz); otherwise it returns 1 / override. The
constructor 0x1800a6a10 registers that cvar with default zero. Botmatch and hideout
override cvars separately default to 32; they do not prove the regular match rate.
Evidence: 25939744-full-tick-getter-functions.json and current cvar constructor
references. engine2.dll is also pinned in source.json, but its tick_interval
serializer is not evidence of the default rate.

0x18085d400 selects pressed (kind 0), held (1), released (2), or double-pressed
(3). Button state has three 64-bit planes: held, changed, and within-command
history. The original 16-entry transition table at 0x1832ca600 preserves a press
and release within one command. Original predicates are pressed=(held & changed)
| history, released=(~held & changed) | history, and within-command double press
=held & changed & history. 0x1817b7000 also tracks 64 last-press command numbers;
its strict command-delta comparison uses round(.22 / interval), or 14 at 64 Hz.
0x1817a8370 processes ordered fractional command steps, updating and restoring
time globals around movement callbacks. Polling once at Minecraft's 20 Hz loses
this information. ButtonState reproduces the native bit-plane operations, but
input event capture, timestamps and fractional simulation are not connected yet.

### Stamina percentage aggregation and omitted data

Client stat 67 callback 0x1808bd700 uses 0x1808a4880. Common percentage aggregator
0x181307be0 folds values using 0x1812fc8c0: positive values multiply a separate
product by (1-p/100), negative values multiply another by (1+p/100). The result
is max(0, 1 + negativeProduct - positiveProduct). Thus repeated -25% wall-jump
debuffs give .75^n, while mixed +10/-25 gives .85, not .825. The independent
PercentageStack helper reproduces both raw products and the final formula.

Profile extraction now fills 17 source-verified defaults by VData class, including
mantle times/minimum height and generic combat durations (hero 3s, nonhero .5s,
neutral 3s). Original generic_data.vdata is exported alongside character profiles.
Current default citadel_use_character_proxy is false, verified in 0x1800b2130;
no installed loose config override was found. Do not substitute the newer proxy
collision path for default 0x180881480.

### Ground acceleration, support and collision

0x180859ff0 projects existing velocity onto wish direction and adds only missing
speed. Its ordinary grounded branch uses max(wishSpeed, 400 Source units/s) *
characterAcceleration * dt * surfaceFriction. Haze's 8.2m/s is below the 10.16m/s
floor, so its unmodified acceleration is 40.64m/s², not 32.8. The separate no-ground
branch uses dt * acceleration * wishSpeed * .25. Direction and max wish speed are
prepared by 0x18085fcf0; preserve that separation in the replacement.

0x18085c280 integrates a damped support spring with frequency 10Hz and damping 1,
omega=2*pi*f. Above squared speed 160000 Source units²/s² damping changes to .25.
It uses the prior support normal/height and a separate spring velocity; this is
not a hard snap of the player's feet onto a floor. Original model capsule uses
step 32, radius 20 and top 80 for Haze, with lower center step+radius and upper
center max(top, lower); duck/tuck modify these centers. Earlier 32x72 AABB notes
describe an incomplete host adaptation, not the actual movement capsule.

Default collision 0x180881480 performs up to 16 outer iterations and projects
penetration/contact constraints. At its final velocity correction, it removes
inward plane components from normalized velocity, then renormalizes and restores
the original speed magnitude unless another event overrides it. Zeroing blocked
Minecraft axes loses this behavior. Capsule sweeps, support classification,
ceiling response and host collision ownership remain implementation work.

### Parry cancellation during a dash

Dash registration 0x180291100 binds cast callback 0x1802885e0; casting ability
slot 0x13 routes to 0x180244150. On a ground dash, this computes original dash
distance / adjusted duration * hero speed scale * .4 in dash heading, preserving
vertical velocity. It ends the dash, clears support spring and retains the
modifier for .4s. Friction callback 0x180256960 supplies -90 until that expiry.
On an air dash, it ends the dash and caps only horizontal speed to air-dash
distance / duration * .4; a slower current horizontal speed is preserved, as is
vertical velocity. It records the cancel time and retains the modifier for the
greater of post-dash drag duration and .4s. The .3 parry-air-gravity VData field
still needs its consuming callback verified; do not apply it by name alone.

### Evidence and limitations

The isolated original-x64 state oracle supplies 48 button-transition cases
(including bits 0/31/63 and press/release/double predicates) and 77 percentage
fold cases. Together with the earlier 198 math and 396 mantle cases there are
719 original-code cases. All 36 software tests pass in
evidence/25939744-state-regression-test.log. These are helper-level checks; the
replacement movement engine, Minecraft adapter and gameplay parity are unfinished.
No game was launched, no old replay was used, and no new JAR was delivered.
