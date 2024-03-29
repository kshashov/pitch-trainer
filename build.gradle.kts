import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.0"
    id("org.openjfx.javafxplugin") version "0.0.8"
    application
}

group = "me.envoy"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test-junit"))

    implementation("no.tornado:tornadofx:1.7.20")
    implementation(kotlin("stdlib-jdk8"))
}

javafx {
//    version = "11.0.2"
    modules("javafx.controls", "javafx.graphics", "javafx.base", "javafx.media")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClassName = "pitch.MyApp"
    mainClass.set("pitch.MyApp")
}

// to "include compile arguments " that replace your VM arguments
tasks.compileJava {
    doFirst {
        options.compilerArgs = listOf(
            "--module-path", classpath.asPath,
            "--add-modules", "javafx.controls,javafx.fxml,javafx.graphics,javafx.media"
        )
        println(options.compilerArgs)
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
