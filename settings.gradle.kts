plugins {
    // Lets Gradle download the JDK the toolchain asks for (17) when the machine
    // has not got one — auto-download is on by default but needs a repository to
    // resolve against, and without this the build fails to even configure with
    // "No locally installed toolchains match".
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "zillit-automation"
