group = "land.src"
version = "1.0-SNAPSHOT"

buildscript {
    repositories.mavenCentral()

    val kotlinVersion = property("kotlin.version")
    dependencies.classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}

allprojects {
    repositories.mavenCentral()
}


subprojects {
    plugins.apply("org.jetbrains.kotlin.jvm")
}