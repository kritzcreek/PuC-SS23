import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "me.creek"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "19"
    kotlinOptions.languageVersion = "1.9"
}
tasks.withType<JavaCompile> {
    targetCompatibility = "19"
}


application {
    mainClass.set("MainKt")
}