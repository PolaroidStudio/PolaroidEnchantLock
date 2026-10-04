import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
}

group = "me.juancayc"
// version comes from gradle.properties — Gradle reads that file's `version` key into
// project.version automatically, and CI's "Get version" step reads the same key without a
// second Gradle invocation. Keep it in exactly one place.
description = "Locks Nexo and MythicMobs item sets so their enchantments cannot be changed."

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "nexo"
        url = uri("https://repo.nexomc.com/releases")
    }
    maven {
        name = "lumine"
        url = uri("https://mvn.lumine.io/repository/maven-public/")
    }
}

dependencies {
    // Paper API floor. api-version in paper-plugin.yml is the real floor (1.21.7, for the Dialog
    // API); this is simply the latest 1.21.x javac target so the jar can use anything shipped up
    // to it.
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // Optional hooks. Never shaded: each real implementation is provided at runtime by that
    // plugin's own jar, guarded behind isPluginEnabled() and declared as a soft dependency in
    // paper-plugin.yml. Not transitive: only the item-lookup classes themselves are referenced,
    // never the libraries those plugins bundle.
    compileOnly("com.nexomc:nexo:1.8.0") {
        isTransitive = false
    }
    compileOnly("io.lumine:Mythic-Dist:5.10.0") {
        isTransitive = false
    }

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // compileOnly isn't visible to the test source set; the sets.yml and lang-file tests use
    // Bukkit's YamlConfiguration, though no test ever starts a live server.
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

tasks.withType<JavaCompile> {
    // The actual portability guarantee: bytecode targets Java 21 (class file major version 65)
    // even when the toolchain above resolves a Java 25 javac to run the compiler itself.
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
    }
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveBaseName.set("PolaroidEnchantLock")
    // CI passes BUILD_NUMBER (the run number) so each build produces a distinct, traceable jar
    // name (PolaroidEnchantLock-1.0.0-b42.jar) that the release step can reference
    // deterministically. Absent locally, so a plain `./gradlew jar` still produces
    // PolaroidEnchantLock-1.0.0.jar.
    System.getenv("BUILD_NUMBER")?.let { buildNumber ->
        archiveVersion.set("${project.version}-b$buildNumber")
    }
}
