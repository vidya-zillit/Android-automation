# Wiring a new module into the runner

Every new module must be **runnable from a flag and included in the full sweep** before it
counts as done. A suite that only I know how to start is not automation you can use.

Six places, all verified against the current scripts. Use the Map module as the worked
example — it is wired through all five.

### 1. `src/test/java/com/zillit/automation/core/TestOrder.java`

Add an `@Order` constant so the class runs in a predictable position.

```java
/** Tools → Map (new_map). Sits after the Drive block, before the DocDist 80s. */
public static final int MAP = 70;
```

Leave gaps between numbers. Suites that depend on another suite's fixture must come after
it — `MAP_PIN_BOUNDARY = 75` runs after `MAP_ZONE_CRUD = 74` on purpose, because a pin
verdict needs a zone to be outside of.

### 2. `run-and-report.sh` — a flag of its own

```bash
elif [[ "${1:-}" == "--map-all" ]]; then
  ...
```

If the module renders a **live map, camera, video or anything that animates continuously**,
give each suite its own Appium session rather than one session for the module. UiAutomator
waits for an idle screen before every lookup and these screens never go idle, which is what
kills the on-device UiAutomator2 instrumentation. Also set `waitForIdleTimeout` — already
done globally in `DriverFactory`.

### 3. `run-and-report.sh` — the `MODULES` list, so `--all` includes it

```bash
"Map & Location::MapModuleTest MapLocationPinsTest LocationToolTest ..."
```

**This is the step that gets forgotten.** Without it the module has a flag but never runs in
the full sweep, so a green `--all` means less than it appears to. If its suites need
per-suite sessions, add the label to `PER_SUITE_LABELS` beside it.

### 4. `run-ui.sh` — the help text

The flag must appear in `./run-ui.sh --help`. That listing is how you find out what you can
run; a flag missing from it effectively does not exist.

### 5. `tools/make-report.mjs` — `moduleOf()`

```javascript
if (n.includes('mapcity')) return { label: 'Map · Cities', nav: [...] };
```

The `nav` lines tell whoever reads the report how to reach that screen by hand. Without an
entry the suite lands in a catch-all group and the report cannot be handed to a developer.

### 6. `.run/Zillit_<Module>.run.xml` — the Android Studio run configuration

**This is the one the user actually sees.** A shell flag is not an option in the IDE; the
Run/Debug Configurations dropdown is. Copy any existing file in `.run/` and change two
lines:

```xml
<configuration default="false" name="Zillit ▸ Map (full)" type="ShConfigurationType">
  <option name="SCRIPT_OPTIONS" value="--map-all" />
```

Keep the `Zillit ▸ ` prefix and the `▸` character (U+25B8) so it groups with the rest. Name
the file `Zillit_<Module>.run.xml`; a long module gets two entries, following Drive —
`Zillit ▸ Map` for the quick read-only pass and `Zillit ▸ Map (full)` for everything.

Android Studio picks these up from disk; the user may need to reopen the project to see a
new one.

### And on every suite: register `ReasonLogger`

```java
@ExtendWith(ReasonLogger.class)
```

Without it a skip prints its name and nothing else. Three Map suites went a full session
reporting 16 skips with no reason in the log *or* the JUnit XML, which cost several runs to
work out. `MapBaseTest` has it; a suite extending `BaseTest` directly must add it itself.

---

## Check it the way you would use it

```bash
./run-ui.sh --help | grep <your-flag>
./run-ui.sh --<your-flag>
ls .run/ | grep -i <module>          # the IDE entry exists
```

Then **look at the Run/Debug Configurations dropdown in Android Studio**. The Map module
was fully wired through the scripts and still invisible there, because step 6 was missed —
from the user's seat that is indistinguishable from not having done the work.

The flavour is picked automatically when exactly one Zillit build is installed, so no
`ZILLIT_FLAVOR` is needed for the common case.
