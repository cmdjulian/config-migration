plugins {
    id("java")
    `java-library`
    alias(libs.plugins.graalvm)
    alias(libs.plugins.versions)
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

graalvmNative {
    agent {
        defaultMode.set("standard")
    }
    toolchainDetection.set(false)
    binaries {
        named("test") {
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
