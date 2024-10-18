plugins {
    kotlin("jvm") version "2.0.0"
}

group = "land.src"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm-tree:9.7")
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
    implementation("io.github.spair:imgui-java-app:1.87.5")
}

kotlin {
    jvmToolchain(17)
}