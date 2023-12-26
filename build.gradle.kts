plugins {
    id("java")
    id("org.graalvm.buildtools.native") version "0.9.28"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "de.cmdjulian"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    implementation(libs.bundles.jackson)
    implementation(libs.jsonPath)
    implementation(libs.resourceResolver)
    compileOnly(libs.spotbugs)

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("de.cmdjulian.configmigration.Main")
}

tasks.test {
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
