package com.zillit.automation.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guard rail for a repo with more than one author.
 *
 * <p>Class-level {@code @Order} is a single global namespace, so two people working
 * on separate branches can each pick the same number and git will merge both without
 * complaint — the clash only surfaces later as suites running in an unexpected order.
 * (That is how {@code InfoTest} and {@code ToolsManagementTest} both ended up on 49.)
 *
 * <p>Two checks, and the second is the one that actually does the work:
 * <ol>
 *   <li>no two suites resolve to the same order value;</li>
 *   <li>no suite uses a RAW NUMBER — it must reference a {@link
 *       com.zillit.automation.core.TestOrder} constant. This is what makes the
 *       convention self-enforcing: adding a suite means adding a constant to that one
 *       file, so two people doing it on separate branches get a real git merge
 *       conflict instead of a silent duplicate.</li>
 * </ol>
 *
 * <p>Pure source inspection — no device, no Appium session, ~1s — so it is cheap
 * enough to run before every suite.
 */
class TestOrderGuardTest {

    private static final Path TESTS_DIR =
            Paths.get("src", "test", "java", "com", "zillit", "automation", "tests");
    private static final Path TEST_ORDER =
            Paths.get("src", "test", "java", "com", "zillit", "automation", "core", "TestOrder.java");

    /** Class-level @Order(TestOrder.NAME) — anchored at column 0, so method-level ones are ignored. */
    private static final Pattern BY_CONST = Pattern.compile("(?m)^@Order\\(TestOrder\\.(\\w+)\\)");
    /** Class-level @Order(123) — the raw literal we want to ban. */
    private static final Pattern BY_LITERAL = Pattern.compile("(?m)^@Order\\((\\d+)\\)");

    /** constant name -> value, read straight out of TestOrder.java. */
    private static Map<String, Integer> constants() throws IOException {
        Map<String, Integer> out = new LinkedHashMap<>();
        Matcher m = Pattern.compile("public static final int\\s+(\\w+)\\s*=\\s*(\\d+)\\s*;")
                .matcher(Files.readString(TEST_ORDER));
        while (m.find()) out.put(m.group(1), Integer.parseInt(m.group(2)));
        return out;
    }

    private static List<Path> testFiles() throws IOException {
        try (Stream<Path> s = Files.walk(TESTS_DIR)) {
            return s.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    @Test
    @DisplayName("Every suite declares its @Order via a TestOrder constant, never a raw number")
    void noRawOrderLiterals() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path f : testFiles()) {
            Matcher m = BY_LITERAL.matcher(Files.readString(f));
            if (m.find())
                offenders.add(f.getFileName().toString().replace(".java", "") + " uses @Order(" + m.group(1) + ")");
        }
        assertTrue(offenders.isEmpty(),
                "These suites hardcode an order value instead of referencing TestOrder, so a "
                        + "duplicate would merge silently: " + offenders
                        + ". Add a constant to TestOrder (in your module's reserved range) and use it.");
    }

    @Test
    @DisplayName("No two test classes resolve to the same class-level @Order value")
    void orderValuesAreUnique() throws IOException {
        Map<String, Integer> consts = constants();
        Map<Integer, List<String>> byOrder = new LinkedHashMap<>();
        List<String> unknown = new ArrayList<>();

        for (Path f : testFiles()) {
            Matcher m = BY_CONST.matcher(Files.readString(f));
            if (!m.find()) continue;
            String cls = f.getFileName().toString().replace(".java", "");
            Integer val = consts.get(m.group(1));
            if (val == null) { unknown.add(cls + " -> TestOrder." + m.group(1)); continue; }
            byOrder.computeIfAbsent(val, k -> new ArrayList<>()).add(cls + " (" + m.group(1) + ")");
        }

        assertTrue(unknown.isEmpty(), "References a constant that does not exist in TestOrder: " + unknown);

        List<String> clashes = new ArrayList<>();
        byOrder.forEach((order, classes) -> {
            if (classes.size() > 1) clashes.add("@Order " + order + " → " + classes);
        });
        if (!clashes.isEmpty()) {
            System.out.println("!!! DUPLICATE @Order VALUES:");
            clashes.forEach(c -> System.out.println("    " + c));
        }
        assertTrue(clashes.isEmpty(),
                "Two or more suites share an order value, so their run order is undefined: " + clashes
                        + ". Pick a free number from your module's range in TestOrder.");
    }

    @Test
    @DisplayName("Two constants in TestOrder never share a value")
    void constantsAreUnique() throws IOException {
        Map<Integer, List<String>> byValue = new LinkedHashMap<>();
        constants().forEach((name, v) -> byValue.computeIfAbsent(v, k -> new ArrayList<>()).add(name));
        List<String> dupes = new ArrayList<>();
        byValue.forEach((v, names) -> { if (names.size() > 1) dupes.add(v + " → " + names); });
        assertTrue(dupes.isEmpty(),
                "TestOrder defines the same value twice — most likely a merge that kept both sides: " + dupes);
    }

    @Test
    @DisplayName("Suites that mutate global state sort AFTER the suites they could break")
    void destructiveSuitesRunLast() throws IOException {
        // ToolsManagementTest disables tools and reorders groups. Running it before the
        // tool suites could switch off a tool they depend on.
        Integer toolsMgmt = orderOf("ToolsManagementTest");
        if (toolsMgmt == null) return;                    // suite not present — nothing to check
        for (String earlier : new String[]{
                "ConfidentialInfoTest", "InfoTest", "CateringTest", "RecceTest"}) {
            Integer o = orderOf(earlier);
            if (o == null) continue;
            assertTrue(toolsMgmt > o,
                    "ToolsManagementTest (order " + toolsMgmt + ") mutates global tool state and must "
                            + "sort AFTER " + earlier + " (order " + o + ").");
        }
    }

    private Integer orderOf(String className) throws IOException {
        Path f = TESTS_DIR.resolve(className + ".java");
        if (!Files.exists(f)) return null;
        Matcher m = BY_CONST.matcher(Files.readString(f));
        return m.find() ? constants().get(m.group(1)) : null;
    }
}
