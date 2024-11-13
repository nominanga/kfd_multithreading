plugins {
    application
    kotlin("jvm") version "2.1.0-RC"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
}

application {
    mainClass.set("MainKt")
}

tasks.withType<JavaExec> {
    standardInput = System.`in`
}

