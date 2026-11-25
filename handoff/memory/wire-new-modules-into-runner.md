---
name: wire-new-modules-into-runner
description: Every new automation module must be runnable from a flag AND included in the --all sweep, so the user can run it from Android Studio without being told how
metadata:
  type: feedback
---

On 2026-11-20 the user asked, of the finished Map work: "are you added configuration of map
so that i can run it and see the testing?" and then: "add map on all module so when i run
all then this map will be run and test it. and make sure when we work on new moules then you
have to put this things so that i can see the option on my sutdio and run it".

So writing the suites is not the end of the job. A module is done when the user can run it
themselves, without asking and without being handed a command.

**Why:** the Map suites existed and passed, but `--map-all` handed all ten classes to one
Gradle run — the pattern that loses the UiAutomator2 instrumentation and reports dozens of
failures with no assertion behind them. The user's first `--all` would have hit it. Working
automation that fails when THEY run it reads as broken automation.

The user runs these from the **Run/Debug Configurations dropdown in Android Studio**, not
from a terminal. Shell flags alone are invisible to them: after the Map scripts were fully
wired, they sent a screenshot of that dropdown asking "here not visible map where you put
it". An IDE entry in `.run/` is the deliverable, not a nicety.

**How to apply:** follow `ADDING_A_MODULE.md` in the repo root — six wiring points:
`TestOrder.java` (@Order constant), a `run-and-report.sh` flag, the `MODULES` list in the
`--all` sweep (**the one that gets forgotten**), the `run-ui.sh --help` text, `moduleOf()` in
`tools/make-report.mjs`, and **`.run/Zillit_<Module>.run.xml`** — the IDE entry. Plus `@ExtendWith(ReasonLogger.class)` on any suite
extending `BaseTest` directly, or its skips print no reason.

Verify by running the flag AND by checking the dropdown, not by reading the diff. Related:
[[zillit-automation-local-env]], [[zillit-location-tool-deferred]].
