package com.zillit.automation.core;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;

/**
 * Prints WHY a case skipped or failed, on the console, as it happens.
 *
 * <p>Gradle's JUnit-XML writer records an aborted case as a bare {@code <skipped/>} with no
 * message, and {@code testLogging.events("skipped")} prints only the name. So a run where
 * forty cases skipped said nothing at all about the reason, and each diagnosis cost a
 * whole re-run to narrow down. Every {@code assumeTrue} in these suites carries a written
 * reason; this is what makes those reasons reach the log.
 *
 * <p>Registered on {@code MapBaseTest} rather than on {@code BaseTest}, to keep it off the
 * suites that were not written with this in mind.
 */
public class ReasonLogger
        implements TestWatcher, LifecycleMethodExecutionExceptionHandler {

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        print("SKIP", context, cause == null ? null : cause.getMessage());
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        print("FAIL", context, cause == null ? null : cause.getMessage());
    }

    /**
     * Reports an abort or error raised by {@code @BeforeEach}.
     *
     * <p>{@link TestWatcher} covers the test METHOD only. When a suite's entry code gives
     * up — "Never reached a project", say — JUnit skips the case without ever calling
     * {@link #testAborted}, so {@code LocationToolTest} reported six bare skips with no
     * message in the log OR the XML, and the run said nothing about why a whole suite did
     * nothing. The reason is right there in the {@code assumeTrue}; this lets it out.
     */
    @Override
    public void handleBeforeEachMethodExecutionException(
            ExtensionContext context, Throwable throwable) throws Throwable {
        print(isAbort(throwable) ? "SKIP" : "FAIL", context, throwable.getMessage());
        throw throwable;
    }

    /** Same for {@code @BeforeAll}, which takes the whole class down with it. */
    @Override
    public void handleBeforeAllMethodExecutionException(
            ExtensionContext context, Throwable throwable) throws Throwable {
        print(isAbort(throwable) ? "SKIP" : "FAIL", context, throwable.getMessage());
        throw throwable;
    }

    private static boolean isAbort(Throwable t) {
        for (Throwable e = t; e != null; e = e.getCause()) {
            if (e.getClass().getName().equals("org.opentest4j.TestAbortedException")) return true;
        }
        return false;
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        print("OFF ", context, reason.orElse(null));
    }

    /**
     * The last line printed, so one abort is not reported twice.
     *
     * <p>A {@code @BeforeEach} abort reaches BOTH
     * {@link #handleBeforeEachMethodExecutionException} and {@link #testAborted} — JUnit
     * calls the handler, then still marks the case aborted — so every skip in
     * {@code LocationToolTest} printed twice the first time this ran.
     */
    private static String lastPrinted;

    private void print(String tag, ExtensionContext context, String message) {
        String cls = context.getTestClass().map(Class::getSimpleName).orElse("?");
        String name = context.getDisplayName();
        String why = message == null ? "(no reason given)" : message.split("\n")[0];
        why = why.replace("Assumption failed: ", "");
        if (why.length() > 300) why = why.substring(0, 300) + "…";
        String line = ">>> " + tag + "  " + cls + " :: " + name + "\n>>>       " + why;
        if (line.equals(lastPrinted)) return;
        lastPrinted = line;
        System.out.println(line);
    }
}
