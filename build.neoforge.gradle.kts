@file:Suppress("UnstableApiUsage")

import buildlogic.ifProp
import buildlogic.prop
import buildlogic.strictMaven
import org.slf4j.event.Level

plugins {
    id("net.neoforged.moddev") version "2.0.141"
    id("dev.kikugie.fletching-table.neoforge")
    id("me.modmuss50.mod-publish-plugin")
    id("project.common")
}

val minecraft = extra["minecraft"] as String
val loader = extra["loader"] as String

repositories {
    strictMaven(
        "thedarkcolour",
        "https://thedarkcolour.github.io/KotlinForForge/",
    ) // Kotlin for Forge
}

neoForge {
    version = prop("deps.neoforge.version")

    ifProp("deps.parchment.version") {
        parchment {
            mappingsVersion = prop("deps.parchment.version")
            minecraftVersion = minecraft
        }
    }

    runs {
        create("client") { client() }
        create("server") { server(); programArgument("--nogui") }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = Level.DEBUG
        }
    }

    mods {
        create(prop("mod.id")) {
            sourceSet(sourceSets["main"])
        }
    }
}

val localRuntime: Configuration by configurations.creating

configurations {
    runtimeClasspath {
        extendsFrom(localRuntime)
    }
}

dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:${prop("deps.kff.version")}")

    jarJar(implementation("com.github.luben:zstd-jni:${prop("libs.zstd")}")!!)
    add("additionalRuntimeClasspath", "com.github.luben:zstd-jni:${prop("libs.zstd")}")

    compileOnly("com.electronwill.night-config:toml:${prop("libs.night_config")}")

    listOf(
        "sodium",
        "lithium",
        "immediatelyfast",
        "ferrite-core",
        "modernfix",
        "badoptimizations",
    ).forEach {
        try {
            localRuntime(fletchingTable.modrinth(it))
        } catch (_: NoSuchElementException) {
            println("Mod '$it' not found in modrinth dependencies, skipping...")
        }
    }
}

tasks {
    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }
    named<ProcessResources>("processResources") {
        exclude("fabric.mod.json5", "META-INF/mods.toml")

        if (stonecutter.eval(minecraft, "<=1.20.4")) {
            rename("""neoforge\.mods\.toml""", "mods.toml")
        }
    }
    named<Copy>("buildAndCollect") {
        from(jar.map { it.archiveFile }, sourcesJar.map { it.archiveFile })
    }
}

publishMods {
    file = tasks.jar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.named<Jar>("sourcesJar").map { it.archiveFile.get() })

    val slugs = listOf("kotlin-for-forge")

    modrinth {
        slugs.forEach(::requires)
    }
    curseforge {
        slugs.forEach(::requires)
    }
}
