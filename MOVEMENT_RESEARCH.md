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
cases. Existing 32 tests are stability/regression tests, not proof of native parity.
Research and implementation are still in progress.
