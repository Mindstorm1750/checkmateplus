plugins {
    kotlin("jvm") version "2.3.20" apply false
    kotlin("plugin.spring") version "2.3.20" apply false
    kotlin("plugin.serialization") version "2.3.20" apply false
    id("org.springframework.boot") version "4.0.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.graalvm.buildtools.native") version "1.0.0"
}

allprojects {
    group = "org.checkmateplus"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
