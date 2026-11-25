# Tool Windows: web E2E vs Android Appium gap analysis

Web source: `web-e2e/tool-windows.md` (1 test, 0 sheet bugs).

## Summary

| Class | Web tests |
|---|---|
| COVERED | 0 |
| MISSING | 0 |
| WEB-ONLY | 1 |
| CONFLICT | 0 |
| **Total** | **1** |

## WEB-ONLY

- **"sidebar highlights C&C after Classic → Windowed on C&C"**
  - The Classic view / Windowed view switch and the window strip are a web desktop layout (MDI).
  - Android has a bottom navigation bar and one screen at a time. `res/values/strings.xml` has no "Windowed" or "Classic view" string, and there is no window strip.
  - The closest Android behaviour is the bottom-nav C&C tab staying selected while you are in C&C. The C&C suites already open that tab (for example `CcGroupCreateTest.landingLoads`), so there is nothing worth adding.
