plugins {
    kotlin("jvm") version "2.0.0"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("top.hendrixshen.replace-token") version "1.1.2"
}

group = "land.src"
version = "1.0.0"

val recafVersion = "236cbf2dcc"
val recafSnapshots = true

val pluginMainClass = "land.src.toolbox.plugin.ToolboxPlugin"

kotlin {
    jvmToolchain(22)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
}

configurations.configureEach {
    exclude(group = "org.checkerframework")
    exclude(group = "com.google.code.findbugs")
    exclude(group = "com.google.errorprone")
    exclude(group = "com.google.j2objc")
    exclude(group = "com.android.tools")
    exclude(group = "com.ibm.icu")
}

dependencies {
    if (recafSnapshots) {
        implementation("com.github.Col-E.Recaf:recaf-core:$recafVersion")
        implementation("com.github.Col-E.Recaf:recaf-ui:$recafVersion")
    } else {
        implementation("software.coley:recaf-core:$recafVersion")
        implementation("software.coley:recaf-ui:$recafVersion")
    }

    testImplementation(platform("org.junit:junit-bom:5.11.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

javafx {
    version = "22.0.1"
    modules = listOf("javafx.base", "javafx.graphics", "javafx.controls", "javafx.media")
}

replaceToken {
    targetSourceSets.set(listOf(sourceSets.main.get()))
    replace("##VERSION##", version.toString())
}

tasks.register("setupServiceEntry") {
    outputs.dir(temporaryDir)

    doFirst {
        file("${temporaryDir}/META-INF/services").mkdirs()
        file("${temporaryDir}/META-INF/services/software.coley.recaf.plugin.Plugin").writeText(pluginMainClass)
    }
}

tasks.jar {
    from(tasks.named("setupServiceEntry"))
}

tasks.register("runRecaf", JavaExec::class) {
    dependsOn("build")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("software.coley.recaf.Main")
    args("-r", "build/libs")
}