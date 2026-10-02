@file:Suppress("UnstableApiUsage")

plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.kikugie.dev/snapshots") // Fletching Table
    maven("https://maven.kikugie.dev/releases") // Stonecutter
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:2.3.0")
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.6")
    implementation("me.modmuss50:mod-publish-plugin:2.1.1")
    implementation("dev.kikugie:fletching-table:0.1.0-alpha.23")
    implementation("dev.kikugie:stonecutter:0.9.2")
}
