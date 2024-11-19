plugins {
    kotlin("jvm") version "2.0.0"
}

group = "land.src"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("software.coley:cafedude-core:2.1.2")
    // slf4j
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("org.slf4j:slf4j-simple:1.7.32")
    implementation("net.fornwall:jelf:0.9.0")
    implementation("org.ow2.asm:asm-tree:9.7")
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
    implementation("io.github.spair:imgui-java-app:1.87.5")
    implementation(kotlin("reflect"))
}

kotlin {
    jvmToolchain(17)
}