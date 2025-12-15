plugins {
	kotlin("jvm") version "2.0.21"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.6"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "1.9.25"
	id("org.sonarqube") version "5.1.0.4882"
}

group = "com.elegantsoftware.blitzpay"
version = "0.0.1-SNAPSHOT"
description = "EU Qrcode payment"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/snapshot") }
}

val postgresqlVersion = "42.7.3"
val springModulithVersion = "1.2.3"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
     implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-starter-jpa")
    implementation("org.postgresql:postgresql:${property("postgresqlVersion")}")
    runtimeOnly("org.springframework.modulith:spring-modulith-actuator")
    runtimeOnly("org.springframework.modulith:spring-modulith-starter-insight")

    // TrueLayer dependencies
    implementation("com.truelayer:truelayer-java:17.4.0")
    implementation("com.truelayer:truelayer-signing:0.2.6")

    // Springdoc - ensure compatible version
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // JWT
    implementation("com.nimbusds:nimbus-jose-jwt:10.5")

    // QR Code generation
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // Dev tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    // If you still OOM at 2g, try "3g"
    maxHeapSize = "2g"
    // Fewer concurrent forks reduces peak memory
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    jvmArgs(
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:MaxMetaspaceSize=512m"
    )
	useJUnitPlatform()
}
tasks.test {
    doFirst {
        val mockitoAgent = configurations.testRuntimeClasspath.get().find { it.name.contains("mockito-inline") }
        if (mockitoAgent != null) {
            jvmArgs("-javaagent:${mockitoAgent.absolutePath}")
        }
    }
}
