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
    }
    outputs.upToDateWhen { false }
}
