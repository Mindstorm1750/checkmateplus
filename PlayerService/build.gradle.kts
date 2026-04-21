import com.google.protobuf.gradle.id
import java.util.Locale

plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.spring") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.netflix.dgs.codegen") version "8.3.0"
    id("org.hibernate.orm") version "7.2.11.Final"
    id("org.graalvm.buildtools.native") version "0.11.4"
    id("com.google.protobuf") version "0.9.6"
    kotlin("plugin.jpa") version "2.3.20"
}

group = "org.checkmateplus"
version = "0.0.1-SNAPSHOT"
description = "PlayerService"


configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["springGrpcVersion"] = "1.0.2"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-session-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("io.grpc:grpc-services")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.security:spring-security-webauthn")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.grpc:spring-grpc-spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-cache-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-graphql-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-session-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.grpc:spring-grpc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(project(":ChessLibrary"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("dev.samstevens.totp:totp-spring-boot-starter:1.7.1") {
        exclude(group = "com.google.zxing", module = "javase") //resolve vulnerability cve 8.8
        //actually care cause this is the TOTP lib
    }
    implementation("com.google.zxing:core:3.5.3")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:${property("springGrpcVersion")}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.generateJava {
    schemaPaths.add("${projectDir}/src/main/resources/graphql-client")
    packageName = "org.checkmateplus.playerservice.codegen"
    generateClient = true
}

hibernate {
    enhancement {
        enableAssociationManagement = true
        enableDirtyTracking.set(true)
        enableLazyInitialization.set(true)
        enableExtendedEnhancement.set(false)
    }
}


protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.33.2"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.77.1"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc") {
                    option("@generated=omit")
                }
            }
        }
    }
}
sourceSets {
    named("main") {
        java {
            srcDirs(
                "build/generated/source/proto/main/grpc",
                "build/generated/source/proto/main/java"
            )
        }
    }
}
tasks.bootBuildImage {
    var tag = ""
    var projectname = project.name.lowercase(Locale.getDefault())
    var projectVersion = project.version.toString().lowercase()
    var commitSha = System.getenv("CI_COMMIT_SHA") ?: ""
    tag = if (commitSha != "") {
        commitSha
    } else projectVersion
    imageName = "git.cs.kent.ac.uk:5050/jhl28/checkmate/${projectname}:${tag}"
    environment.set(mapOf(
        "BP_SPRING_AOT_ENABLED" to "true",
        "BP_JVM_CDS_ENABLED" to "true",
        "BP_NATIVE_IMAGE" to "true", //if you want to disable comment out the native image plugin and set to false here!
        "BP_NATIVE_IMAGE_BUILD_ARGUMENTS" to """
            -march=x86-64-v3
            --gc=G1 
            -O3
            """.trimIndent(),
        "BP_JVM_VERSION" to "25",
        // G1GC for less stop the world pauses, higher throughput
        // -O3 enables higher tier optimizations giving higher throughput and faster startup
        // it seems the default march might be x86-64-v3 but im forcing this to allow for compatibility while retaining
        // the instruction set optimizations
        "BP_BINARY_COMPRESSION_METHOD" to "upx" //*29.97% image compression
    ))
    buildpacks = listOf("docker.io/paketobuildpacks/oracle", "urn:cnb:builder:paketo-buildpacks/java-native-image")
    // we have to use the oracle variant of the buildpack rather than BellSoft Liberica to give us access to g1gc
    // if there is an issue with using the oracle graalvm variant simply comment out the line and remove g1gc from
    // the build args
    // if you want a very quick build use -Ob it will cause less performance as the image will not receive optimization
    // also disables machine-learning PGO!
}
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
