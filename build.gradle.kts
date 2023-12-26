plugins {
    id("java")
    alias(libs.plugins.graalvm)
    alias(libs.plugins.versions)
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "de.cmdjulian"
version = "1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    compileOnly(libs.spotbugs)

    implementation(libs.bundles.jackson)
    implementation(libs.jsonPath)
    implementation(libs.resourceResolver)
    implementation(libs.slf4j)

    testImplementation(libs.junit)
}

application {
    mainClass.set("de.cmdjulian.configmigration.Main")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

graalvmNative {
    agent {
        defaultMode.set("standard")
    }
    toolchainDetection.set(false)
    binaries {
        all {
            resources {
                autodetect()
                autodetection {
                    ignoreExistingResourcesConfigFile = true
                }
            }
        }
    }
    metadataRepository {
        enabled.set(true)
    }
}
