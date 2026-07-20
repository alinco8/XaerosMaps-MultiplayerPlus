import buildlogic.ifProp
import buildlogic.modImplementation
import buildlogic.prop
import buildlogic.propOrNull
import buildlogic.propList
import buildlogic.strictExpand
import buildlogic.strictMaven

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("me.modmuss50.mod-publish-plugin")
    id("dev.kikugie.fletching-table")
}

repositories {
    maven("https://maven.isxander.dev/releases")
    strictMaven("org.quiltmc", "https://maven.quiltmc.org/repository/release") // QuiltMC
    strictMaven("maven.modrinth", "https://api.modrinth.com/maven") // Modrinth
    strictMaven("curse.maven", "https://beta.cursemaven.com") // CurseMaven
    strictMaven("org.parchmentmc", "https://maven.parchmentmc.org/") // Parchment
    strictMaven("xaero", "https://chocolateminecraft.com/maven") // Xaero Lib
    mavenCentral()
}

val (minecraft, loader) = project.name.split("-")
extra["minecraft"] = minecraft
extra["loader"] = loader

base.archivesName = prop("mod.id")
version = "${prop("mod.version")}+$minecraft-$loader"

fletchingTable {
    j52j {
        register("main") {
            extension("json", "fabric.mod.json5")
        }

        all {
            prettyPrint = true
        }
    }


    lang {
        create("main") {
            patterns.add("assets/${prop("mod.id")}/lang/**")
        }
        all {
            sortKeys = true
            prettyPrint = true
        }
    }

    mixins {
        create("main") {
            mixin("default", "xmmp.mixins.json") {
                env("CLIENT")
            }
        }
    }
}

dependencies {
    fletchingTable.minecraft = minecraft

    modImplementation(
        "xaero.lib:xaerolib-$loader-${propOrNull("deps.xaero_lib.mc") ?: minecraft}:${
            prop(
                "deps.xaero_lib.version"
            )
        }"
    )
    modImplementation(
        "xaero.map:xaeroworldmap-$loader-${propOrNull("deps.xaero_world_map.mc") ?: minecraft}:${
            prop(
                "deps.xaero_world_map.version"
            )
        }"
    )
    modImplementation(
        "xaero.minimap:xaerominimap-$loader-${propOrNull("deps.xaero_minimap.mc") ?: minecraft}:${
            prop(
                "deps.xaero_minimap.version"
            )
        }"
    )
    modImplementation("dev.isxander:yet-another-config-lib:${prop("deps.yacl.version")}")
}

sourceSets {
    named("main") {
        resources.setSrcDirs(listOf(layout.buildDirectory.dir("generated/stonecutter/main/resources")))
    }
}

java {
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(prop("deps.java.version").toInt())
    }
}

kotlin {
    jvmToolchain(prop("deps.java.version").toInt())
}

tasks {
    named<ProcessResources>("processResources") {
        dependsOn("stonecutterGenerate")

        val loaderValue = loader
        val props = ext.properties
            .filterKeys { it.startsWith("mod.") || it.startsWith("deps.") }
            .filterValues { it != null }
            .mapValues { it.value.toString() } +
                mapOf(
                    "env.minecraft" to minecraft,
                    "env.loader" to loader,
                )

        inputs.property("props", props)
        inputs.property("loader", loaderValue)

        filesMatching(
            listOf(
                "**/*.toml",
                "**/*.json",
                "**/*.json5",
                "**/*.mcmeta",
            )
        ) {

            filteringCharset = "UTF-8"
            strictExpand(loaderValue, props, file.path)
        }
    }
    named("sourcesJar") {
        dependsOn(named("stonecutterGenerate"))
    }
    register<Copy>("buildAndCollect") {
        dependsOn(named("build"))
        group = "build"
        into(rootProject.layout.buildDirectory.file("libs"))
    }
}

publishMods {
    dryRun = propOrNull("DRY_RUN")?.toBoolean() ?: true

    type = STABLE
    version = project.version.toString()
    changelog = propOrNull("CHANGELOG") ?: "No changelog provided."
    modLoaders.add(loader)

    displayName = "${prop("mod.version")} for $loader $minecraft"

    var mcVersions = (propList("publish.minecraft") + minecraft).distinct()

    var slugs = mutableListOf(
        "yacl", "xaeros-world-map", "xaeros-minimap"
    )

    ifProp("MODRINTH_TOKEN") {
        modrinth {
            projectId = prop("publish.modrinth.id")
            accessToken = it
            minecraftVersions.addAll(mcVersions)

            CLIENT_AND_SERVER

            slugs.forEach(::requires)
        }
    }
    ifProp("CURSEFORGE_TOKEN") {
        curseforge {
            projectId = prop("publish.curseforge.id")
            accessToken = it
            minecraftVersions.addAll(mcVersions)

            client = true
            server = true

            slugs.forEach(::requires)
        }
    }
}
