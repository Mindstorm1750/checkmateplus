import com.google.protobuf.gradle.id
import java.util.Locale

plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.spring") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.graalvm.buildtools.native") version "0.11.4"
    id("com.google.protobuf") version "0.9.5"
}
version = "latest"
description = "MatchmakingService"


configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2025.1.0"
val springGrpcVersion by extra("1.0.2")

dependencies {
    implementation(project(":ChessLibrary"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
    //implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    testImplementation("org.springframework.security:spring-security-test")
    compileOnly("org.projectlombok:lombok")
    implementation("org.springframework.grpc:spring-grpc-spring-boot-starter")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    runtimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("io.grpc:grpc-services")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-messaging")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:$springGrpcVersion")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
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
tasks.withType<Test> {
    useJUnitPlatform()
}
