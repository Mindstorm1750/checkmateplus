plugins {
    kotlin("jvm") version "2.3.10"
    id("java-library")
    kotlin("plugin.serialization") version "2.3.10"
}
repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}