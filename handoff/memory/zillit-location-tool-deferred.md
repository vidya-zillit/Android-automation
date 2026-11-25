---
name: zillit-location-tool-deferred
description: The Location tool is ENABLED on ZL-Android-Automation since 2026-11-23; overlay suites run (no longer skip by design)
metadata:
  type: project
---

Until 2026-11-20 the Location tool was off and 16 map/overlay cases skipped by design. On
**2026-11-23 it was found enabled** (Vidya Testing has view access, Shoot Locations overlay on),
and the user asked for the Location → Map flow to be tested ("last time we missed it").

Now: `MapLocationFlowTest` (`TestOrder.MAP_LOCATION_FLOW = 179`, `--map-location-flow`, in
`--map-all`) creates a ZL_AUTO location, moves Selects → Shortlist → Final and checks the Map
(10/10 on build Zillit_32539-dev). `LocationToolTest` 5 pass / 1 skip (LT-12: app hides that icon).

App facts learned: overlay pins are filed by MAP POSITION (whichever city circle contains the pin),
not by the City text field; "Move" to Shortlist/Final leaves a copy in the previous tab (to confirm
with product). The project is a TV project, so locations sit under "Episode No : 1".

**Why:** the old "skips by design" note would now hide real failures.
**How to apply:** treat Location/overlay failures as real. Map city deletes must target only
ZL_AUTO cities — an early run of the flow suite deleted the Mountain View city by mistake (re-added
via "Add Current City"; its zones may differ from the original). Related: [[zillit-automation-local-env]].
