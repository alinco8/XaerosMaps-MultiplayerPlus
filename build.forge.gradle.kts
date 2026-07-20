@file:Suppress("UnstableApiUsage")

import buildlogic.ifProp
import buildlogic.prop
import buildlogic.strictMaven
import org.slf4j.event.Level

plugins {
    id("net.neoforged.moddev.legacyforge") version "2.0.141"
    id("dev.kikugie.fletching-table.lexforge")
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

legacyForge {
    version = "${minecraft}-${prop("deps.forge.version")}"

    ifProp("deps.parchment.version") {
        parchment {
            mappingsVersion = prop("deps.parchment.version")
            minecraftVersion = minecraft
        }
    }

    runs {
        create("client") {
            client()
            ideName = "${project().name} - Client"
        }
        create("server") {
            server();
            programArgument("--nogui")
            ideName = "${project().name} - Server"
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("forge.logging.console.level", "debug")
            programArgument("--mixin.config=${prop("mod.id")}.mixins.json")

            logLevel = Level.DEBUG
        }
    }

    mods {
        create(prop("mod.id")) {
            sourceSet(sourceSets["main"])
        }
    }
}

dependencies {
    modImplementation("thedarkcolour:kotlinforforge:${prop("deps.kff.version")}")
    implementation("thedarkcolour:kfflib:${prop("deps.kff.version")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    jarJar(implementation("com.github.luben:zstd-jni:${prop("libs.zstd")}")!!)
    add("additionalRuntimeClasspath", "com.github.luben:zstd-jni:${prop("libs.zstd")}")

    compileOnly("com.electronwill.night-config:toml:${prop("libs.night_config")}")

    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")
    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
}

mixin {
    add(sourceSets.main.get(), "${prop("mod.id")}.refmap.json")
}

val reobfJar = tasks.named<AbstractArchiveTask>("reobfJar")

tasks {
    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }
    named<ProcessResources>("processResources") {
        exclude("fabric.mod.json5", "META-INF/neoforge.mods.toml")
    }
    named<Copy>("buildAndCollect") {
        from(reobfJar.map { it.archiveFile }, sourcesJar.map { it.archiveFile })
    }
    named<Jar>("jar") {
        manifest {
            attributes(
                "MixinConfigs" to "${prop("mod.id")}.mixins.json"
            )
        }
    }
}

publishMods {
    file = reobfJar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.named<Jar>("sourcesJar").map { it.archiveFile.get() })

    val slugs = listOf("kotlin-for-forge")

    modrinth {
        slugs.forEach(::requires)
    }
    curseforge {
        slugs.forEach(::requires)
    }
}
