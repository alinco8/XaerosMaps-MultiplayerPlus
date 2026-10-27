plugins {
    id("xyz.jpenilla.run-paper") version "3.1.0"
    kotlin("jvm")
    id("com.gradleup.shadow")
}

fun propOrNull(key: String) = project.findProperty(key)?.toString()
fun prop(key: String) = propOrNull(key) ?: error("Property $key is missing!")

group = "dev.alinco8.xmmp"

base.archivesName = "xmmp-plugin"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("io.papermc.paper:paper-api:${prop("plugin.deps.paper_api")}")

    implementation("dev.alinco8.xmmp:core")
    compileOnly(libs.netty.buffer)
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        dependencies {
            exclude(dependency("org.jetbrains.kotlin:.*:.*"))
        }
    }

    processResources {
        val props = mapOf(
            "version" to project.version.toString(),
            "kotlinVersion" to libs.versions.kotlin.get()
        )
        inputs.property("props", props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    runServer {
        minecraftVersion(prop("plugin.deps.minecraft"))
    }
}
