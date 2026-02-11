plugins {
    kotlin("jvm") version "2.3.10" apply false
    kotlin("plugin.spring") version "2.3.10" apply false
    kotlin("plugin.serialization") version "2.3.10" apply false
    id("org.springframework.boot") version "4.0.2" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.graalvm.buildtools.native") version "0.11.4" apply false
}

allprojects {
    group = "org.checkmateplus"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
