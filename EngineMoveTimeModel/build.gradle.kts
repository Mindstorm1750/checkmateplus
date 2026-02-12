plugins {
    kotlin("jvm") version "2.3.10"
}

group = "org.checkmateplus"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.deeplearning4j:deeplearning4j-core:1.0.0-M2.1")
    implementation("org.nd4j:nd4j-native-platform:1.0.0-M2.1")
    implementation("org.deeplearning4j:deeplearning4j-ui:1.0.0-M2.1")
    implementation("org.deeplearning4j:deeplearning4j-datasets:1.0.0-M2.1")
}

tasks.test {
    useJUnitPlatform()
}