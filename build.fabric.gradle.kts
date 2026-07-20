import buildlogic.prop
import buildlogic.strictMaven

plugins {
    id("dev.kikugie.loom-back-compat")
    id("dev.kikugie.fletching-table.fabric")
    id("me.modmuss50.mod-publish-plugin")
    id("project.common")
}

val minecraft = extra["minecraft"] as String
val loader = extra["loader"] as String

repositories {
    //TODO: use strictMaven(...)
    maven("https://maven.terraformersmc.com/releases") // Mod Menu
    maven("https://maven.terraformersmc.com") // Mod Menu
}

dependencies {
    // TODO: Add parchment mappings
    loomx.applyMojangMappings()

    minecraft("com.mojang:minecraft:$minecraft")
    modImplementation("net.fabricmc:fabric-loader:${prop("deps.fabric_loader.version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric_api.version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${prop("deps.fabric_language_kotlin.version")}")

    modImplementation("com.terraformersmc:modmenu:${prop("deps.modmenu.version")}")

    include(implementation("com.github.luben:zstd-jni:${prop("libs.zstd")}")!!)

    include(implementation("com.electronwill.night-config:toml:${prop("libs.night_config")}")!!)
    include(implementation("com.electronwill.night-config:core:${prop("libs.night_config")}")!!)
}

loom {
    log4jConfigs.from(rootProject.file("log4j2.xml"))

    if (stonecutter.eval(minecraft, "<26"))
        accessWidenerPath = rootProject.file("src/main/resources/${prop("mod.id")}-named.aw")

    runs {
        named("client") {
            programArguments.addAll("--username", "Dev")
            displayName = "${project.name} - Client"
            appendProjectPathToDisplayName = false
        }
        named("server") {
            displayName = "${project.name} - Server"
            appendProjectPathToDisplayName = false
        }

        configureEach {
            generateRunConfig = true
//            vmArgs("-Dsodium.checks.issue2561=false")
        }
    }
}

tasks {
    named<ProcessResources>("processResources") {
        exclude("META-INF/neoforge.mods.toml", "META-INF/mods.toml")
    }
    named<Copy>("buildAndCollect") {
        from(loomx.modJar.map { it.archiveFile }, loomx.modSourcesJar.map { it.archiveFile })
    }
}

publishMods {
    file = loomx.modJar.map { it.archiveFile.get() }
    additionalFiles.from(loomx.modSourcesJar.map { it.archiveFile.get() })

    val slugs = listOf("fabric-api", "fabric-language-kotlin")
    val optionalSlugs = listOf("modmenu")

    modrinth {
        slugs.forEach(::requires)
        optionalSlugs.forEach(::optional)
    }
    curseforge {
        slugs.forEach(::requires)
        optionalSlugs.forEach(::optional)
    }
}
