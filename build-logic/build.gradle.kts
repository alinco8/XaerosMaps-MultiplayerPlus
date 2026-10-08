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
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization.gradle.plugin)
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.mod.publish.gradle.plugin)
    implementation(libs.fletching.table)
    implementation(libs.stonecutter)
}
