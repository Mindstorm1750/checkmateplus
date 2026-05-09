import com.google.protobuf.gradle.id
import java.util.Locale

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.graalvm.buildtools.native")
    id("com.google.protobuf") version "0.9.5"
}


version = "latest"
description = "VoiceCommandService"


configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2025.1.1"
extra["springGrpcVersion"] = "1.0.2"

dependencies {
    implementation(project(":ChessLibrary"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("io.grpc:grpc-services")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
    //implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.grpc:spring-grpc-spring-boot-starter")
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-websocket-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.grpc:spring-grpc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.springframework:spring-web:7.0.7")
    implementation("com.openai:openai-java-spring-boot-starter:4.26.0")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:${property("springGrpcVersion")}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java"
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

tasks.register<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("javaBootBuildImage") {
    group = "build"
    description = "Builds a JVM (non-native) OCI image via Paketo buildpacks."
    archiveFile.set(tasks.bootJar.flatMap { it.archiveFile })
    dependsOn(tasks.bootJar)
    val projectname = project.name.lowercase(Locale.getDefault())
    val projectVersion = project.version.toString().lowercase()
    val commitSha = System.getenv("CI_COMMIT_SHA") ?: ""
    val tag = if (commitSha != "") commitSha else projectVersion
    imageName = "git.cs.kent.ac.uk:5050/jhl28/checkmate/${projectname}:${tag}-jvm"
    environment.set(mapOf(
        "BP_SPRING_AOT_ENABLED" to "true",
        "BP_JVM_CDS_ENABLED" to "true",
        "BP_JVM_VERSION" to "25",
        "BP_BINARY_COMPRESSION_METHOD" to "upx"
    ))
    buildpacks = listOf("docker.io/paketobuildpacks/oracle", "urn:cnb:builder:paketo-buildpacks/java")
}
tasks.withType<Test> {
    useJUnitPlatform()
}
