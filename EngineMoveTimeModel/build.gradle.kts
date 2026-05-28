plugins {
    kotlin("jvm") version "2.3.20"
}

group = "org.checkmateplus"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation ("org.jetbrains.kotlinx:kotlin-deeplearning-tensorflow:0.5.2")
    implementation ("org.jetbrains.kotlinx:kotlin-deeplearning-onnx:0.5.2")
    implementation ("org.jetbrains.kotlinx:kotlin-deeplearning-visualization:0.5.2")
}

tasks.test {
    useJUnitPlatform()
}