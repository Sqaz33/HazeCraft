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
