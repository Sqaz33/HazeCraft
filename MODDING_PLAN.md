# Modding plan

- Host: Minecraft Java + Fabric; isolated lab instance, no existing saves modified.
- Guest: installed Deadlock build 25917577 (2026-10-13), read-only assets and static native analysis.
- Route: in-process Java simulation and custom skeletal renderer, local conversion tools outside retail install.
- Dependencies: Java 21, pinned Fabric toolchain; Source2Viewer CLI for local Source 2 conversion.
- Output: one private runtime JAR; user-owned retail-derived assets excluded from Git and internet.
- First proof: data extraction and provenance; deterministic gun/Fixation/ability/movement tests; compile before launching.
- Unknowns: native movement formulas, default cvars, animation graph decoding, exact camera behavior, Source 2 HUD shader equivalents.
- Lab: `D:/DeadlockHazeLab`; future disposable test world under project `run/`.
- Status: IN PROGRESS. No completed fidelity claim.
