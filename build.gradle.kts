plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.spring") version "2.2.21" apply false
    kotlin("plugin.serialization") version "2.2.21" apply false
    id("org.springframework.boot") version "4.0.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.graalvm.buildtools.native") version "0.11.3" apply false
}

allprojects {
    group = "uk.ac.kent"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
