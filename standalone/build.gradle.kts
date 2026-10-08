plugins {
    id("com.gradleup.shadow")
    kotlin("jvm")
    application
}

group = "dev.alinco8.xmmp"

repositories {
    mavenCentral()
}

base.archivesName = "xmmp-standalone"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation("dev.alinco8.xmmp:core")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("io.netty:netty-all:4.2.18.Final")
    implementation("it.unimi.dsi:fastutil:8.5.19")
    implementation("org.slf4j:slf4j-api:2.0.19")
    implementation("com.github.luben:zstd-jni:1.5.7-9")
    implementation("com.akuleshov7:ktoml-core:0.7.1")

    runtimeOnly("org.slf4j:slf4j-simple:2.0.19")
}

application {
    mainClass.set("dev.alinco8.xmmp.standalone.MainKt")
}

tasks {
    jar {
        archiveClassifier.set("plain")
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
        manifest {
            attributes["Main-Class"] = application.mainClass.get()
        }
    }
}
