plugins {
    kotlin("jvm") version "2.2.20"
}

group = "uk.ac.kent"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}