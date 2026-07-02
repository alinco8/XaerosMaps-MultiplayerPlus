@file:Suppress("UnstableApiUsage")

// The project name cannot contain the character ':'
rootProject.name = "Xaero's Maps Multiplayer+"
includeBuild("build-logic")

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
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.loom-back-compat") version "0.3"
    id("dev.kikugie.stonecutter") version "0.9.2"
}

stonecutter {
    create(rootProject) {
        fun mc(version: String, vararg loaders: String) = loaders.forEach {
            version("$version-$it", version).buildscript("build.$it.gradle.kts")
        }

        mc("1.20.1", "forge" /*"fabric"*/) // 1.20(.X)
        mc("1.21.1", "neoforge" /*"fabric"*/) // 1.21(.X)
        mc("26.1.2", /*"neoforge", */"fabric") // 26.1~26.2

        vcsVersion = "1.21.1-neoforge"
    }
}
