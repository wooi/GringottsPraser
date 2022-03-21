import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
//    dependencies{
//        classpath("com.squareup.sqldelight:gradle-plugin:1.5.3")
//    }
}

group = "me.wooi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.squareup.sqldelight:sqlite-driver:1.5.3")
    implementation("org.apache.commons:commons-csv:1.8")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}