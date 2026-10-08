@file:Suppress("UnstableApiUsage")

rootProject.name = "Xaeros Maps Multiplayer"

includeBuild("build-logic")
includeBuild("core")

include(":standalone", ":plugin")

pluginManagement {
    repositories {
        maven("https://maven.neoforged.net/releases") // NeoForged
        maven("https://maven.fabricmc.net/") // Fabric
        maven("https://maven.minecraftforge.net/") // Forge
        maven("https://maven.kikugie.dev/releases") // Stonecutter
        maven("https://maven.kikugie.dev/snapshots") // Fletching Table
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        id("com.gradleup.shadow") version "9.3.1"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.loom-back-compat") version "0.4.3"
    id("dev.kikugie.stonecutter") version "0.9.8"
}

stonecutter {
    create(rootProject) {
        mapBuilds { _, node -> "build.${node.project.substringAfterLast('-')}.gradle.kts" }
        load(file("versions.json"))
        vcsVersion = "1.21.1-neoforge"
    }
}
