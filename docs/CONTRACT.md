# Simulation/host contract

## Current implementation limits

The existing Motion/HazeClient controller does not implement the contract below:
it polls input at 20 Hz, advances six 120 Hz approximation steps, uses vanilla
AABB movement, and lacks a matching authoritative server movement simulation.
The following describes the replacement boundary, not shipped behavior.

## Replacement movement boundary (2026-10-15)

CharacterMovementProfile selects stats and definitions by movement role. Shared
movement rules take this profile and explicit modifiers; they must not reference
Haze, a model asset, the global Retail singleton or a Minecraft player class.
The physical capsule/model dimensions are a separate character input, never
derived from camera height or the rendered mesh bounding box.

The guest simulation keeps Source lengths and velocities with float32 arithmetic,
Y up in the Java vector representation. The host converts lengths by .0254
metres per Source unit; one Minecraft block is one metre. The native Source Z
component maps to Java Y at the evidence/import boundary. Spatial directions and
camera yaw conversion must be shared by the host collision and animation adapters.

The verified regular guest interval is .015625 seconds (64 Hz). A 50ms host tick
therefore consumes a varying number of guest commands; never round it to three
steps, four steps or six 120Hz steps. Preserve the remainder. Fractional input
events split guest commands at their original order/times, as recovered in
0x1817a8370. Capture key transitions at the client event boundary; held-key polls
cannot recover a press and release that both occurred between host ticks.

ButtonState stores held/changed/history planes. DoublePressTracker stores 64
last-press command numbers with unsigned subtraction and a strict rounded-window
comparison. Native input masks and ability slot selection belong to the source
binding adapter, not to character movement equations. Release/clear queued input
when leaving the mode, losing focus, opening a blocking screen or changing worlds.

MovementEnvironment owns capsule overlap/sweep, contact planes, support probes,
mantle clearance and moving surfaces. A move must return contact geometry, not
just blocked X/Z axes. Guest state owns position, ordinary velocity, spring
velocity, capsule posture, action phases/counters, resource and modifier lifetimes.
Rendering samples guest state; baked animation root travel must not move physics
again. Events notify presentation once per action, independently of rendering.

Minecraft's normal jump, auto-jump, flight toggle, fluid movement, crouch pose,
edge clipping and push-out must not apply a second movement update during this
mode. Canceling travel alone is insufficient: LocalPlayer.aiStep and
LivingEntity.aiStep can act earlier. Reconciliation must restore the full guest
state, including input history/action phase, not just position and velocity.

No public game networking, live process hooks or telemetry is required. All
native evidence and converted private content stays outside tracked source.

## Original broader integration intent

The dedicated/integrated Minecraft server owns combat, abilities and mob statuses. Client predicts player motion and camera with the same pure simulation. Inputs carry sequence numbers and edge-triggered actions; snapshots reconcile state. Steve mode returns normal controls and rendering.

Source data keeps original units. Conversion to blocks happens only at the host boundary. Unit scale must be derived from retail metadata; do not silently choose a scale. A server tick integrates fixed guest substeps, not one arbitrary 50 ms physics step. UI buttons send commands; HUD is a view of snapshots. Release keys and clear transient actions when leaving Haze, changing worlds, losing focus or opening a blocking screen.

No public Deadlock networking, live process hooking, or external telemetry. Extracted retail content is private and not tracked by Git.
