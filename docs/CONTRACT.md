# Simulation/host contract v1

The dedicated/integrated Minecraft server owns combat, abilities and mob statuses. Client predicts player motion and camera with the same pure simulation. Inputs carry sequence numbers and edge-triggered actions; snapshots reconcile state. Steve mode returns normal controls and rendering.

Source data keeps original units. Conversion to blocks happens only at the host boundary. Unit scale must be derived from retail metadata; do not silently choose a scale. A server tick integrates fixed guest substeps, not one arbitrary 50 ms physics step. UI buttons send commands; HUD is a view of snapshots. Release keys and clear transient actions when leaving Haze, changing worlds, losing focus or opening a blocking screen.

No public Deadlock networking, live process hooking, or external telemetry. Extracted retail content is private and not tracked by Git.
