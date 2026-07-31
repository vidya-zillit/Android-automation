package com.zillit.automation.core;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;

/**
 * Captures a screenshot (and the page source) the moment ANY test fails, so the
 * shareable HTML report can show the developer exactly what the screen looked
 * like at the point of failure.
 *
 * <p>Registered once on {@link BaseTest}, so every suite in the project inherits
 * it — no per-test wiring needed.
 *
 * <p>Files are written with a deterministic name the report generator can match
 * back to the failing test:
 * <pre>build/screenshots/FAILED__&lt;TestClass&gt;__&lt;testMethod&gt;.png</pre>
 */
public class FailureScreenshot implements TestWatcher {

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        capture(context, "FAILED");
    }

    /**
     * An aborted test is a SKIP (a failed {@code assumeTrue}), not a defect — but a
     * shot still helps explain WHY the precondition wasn't met.
     */
    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        capture(context, "SKIPPED");
    }

    private void capture(ExtensionContext context, String status) {
        Optional<Object> instance = context.getTestInstance();
        if (instance.isEmpty() || !(instance.get() instanceof BaseTest test)) return;
        String cls = context.getTestClass().map(Class::getSimpleName).orElse("Unknown");
        String method = context.getTestMethod().map(java.lang.reflect.Method::getName).orElse("unknown");
        // The JUnit XML records the @DisplayName, so pass it through for the report
        // to match on — the method name alone cannot be recovered from the XML.
        test.captureDiagnostics(status, cls, method, context.getDisplayName());
    }
}
