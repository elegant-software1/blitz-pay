plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.2.21"
}

group = "com.elegant.software.blitzpay"
version = "0.2.2"
description = "BlitzPay"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/snapshot") }
}

extra["springModulithVersion"] = "2.0.1"

dependencies {
    // Core - Spring Boot & Framework
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:${property("springdocVersion")}")
    
    // Core - Kotlin & Reactive
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("io.github.microutils:kotlin-logging-jvm:${property("kotlinLoggingVersion")}")
    
    // Core - Spring Modulith
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-starter-jpa")
    
    // Module: payments.truelayer - Payment Integration
    implementation("com.truelayer:truelayer-java:${property("truelayerJavaVersion")}")
    implementation("com.truelayer:truelayer-signing:${property("truelayerSigningVersion")}")
    implementation("com.nimbusds:nimbus-jose-jwt:${property("nimbusJoseJwtVersion")}")
    
    // Module: invoice - Invoice Generation & PDF
    implementation("org.mustangproject:library:${property("mustangVersion")}")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.xhtmlrenderer:flying-saucer-pdf:${property("flyingSaucerVersion")}")
    
    // Module: invoiceagent - Excel Processing
    implementation("org.apache.poi:poi-ooxml:5.4.1")

    // Runtime
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.springframework.modulith:spring-modulith-actuator")
    runtimeOnly("org.springframework.modulith:spring-modulith-observability")
    
    // Testing - Core
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${property("mockitoKotlinVersion")}")
    
    // Testing - Spring Modulith & Containers
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property","-jvm-target=25",)
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
