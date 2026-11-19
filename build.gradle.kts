plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.appium:java-client:9.3.0")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // Run classes in @Order sequence (E2E last — it creates real backend data).
    systemProperty(
        "junit.jupiter.testclass.order.default",
        "org.junit.jupiter.api.ClassOrderer\$OrderAnnotation"
    )
    // Forward zillit.* system properties (e.g. -Dzillit.projectCode=...) to the tests.
    systemProperties(System.getProperties()
        .stringPropertyNames()
        .filter { it.startsWith("zillit.") }
        .associateWith { System.getProperty(it) })
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        // Print WHY a test skipped or failed, not just that it did. Every assumeTrue in
        // this suite carries a written reason ("No project", "No chat unit available"),
        // and without these the console shows a bare "SKIPPED" — so a run that skipped
        // everything said nothing about what went wrong, and the reason had to be dug out
        // of the saved page-source dumps afterwards.
        showExceptions = true
        showCauses = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
    }
    // Keep the captured stdout OUT of the JUnit XML.
    //
    // Gradle streams every test's System.out into build/test-results/test/binary/output.bin and
    // reads it back when it writes each class's XML. A long run can leave that file truncated
    // while its index still points into it, and then the XML write fails — "Could not write XML
    // test results for …". The run itself is fine, but the results for those classes are lost,
    // the build is marked FAILED for an I/O reason, and the shareable report silently covers a
    // fraction of what ran. One --home run lost 6 of 9 classes that way: 78 tests executed,
    // 18 reported.
    //
    // Nothing is lost by switching it off: the >>> diagnostics still print to the console via
    // showStandardStreams above, and the <system-out> blocks in the XML were empty anyway.
    reports.junitXml.includeSystemOutLog = false
    reports.junitXml.includeSystemErrLog = false
    outputs.upToDateWhen { false }
}
