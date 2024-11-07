dependencies {
    implementation(kotlin("reflect"))
    implementation(project(":modules:api"))
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
}

kotlin {
    jvmToolchain(17)
}