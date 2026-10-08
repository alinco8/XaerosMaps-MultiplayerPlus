plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
}

group = "dev.alinco8.xmmp"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.ktoml.core)

    compileOnly(libs.netty.buffer)
    compileOnly(libs.fastutil)
    compileOnly(libs.slf4j.api)
    compileOnly(libs.zstd.jni)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}
