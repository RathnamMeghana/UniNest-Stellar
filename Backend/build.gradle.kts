plugins {
	java
	id("org.springframework.boot") version "3.2.7"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "UniNest"
version = "0.0.1-SNAPSHOT"
description = "Backend for UniNest"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

// ADD THIS BLOCK - This tells Google Cloud where to download the libraries
repositories {
	mavenCentral()
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

dependencies {
	// Core Spring Boot
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-aop")

	// Firebase
	implementation("com.google.firebase:firebase-admin:9.2.0")

	// Utilities
	implementation("org.apache.commons:commons-text:1.10.0")
	implementation("org.jsoup:jsoup:1.17.2")
	implementation("com.bucket4j:bucket4j-core:8.10.1")

	// Sentry (Monitoring)
	implementation("io.sentry:sentry-spring-boot-starter-jakarta:7.6.0")
	implementation("io.sentry:sentry-logback:7.6.0")

	// Lombok
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.mockito:mockito-core:5.5.0")
	testImplementation("org.mockito:mockito-junit-jupiter:5.5.0")
	testImplementation("org.mockito:mockito-inline:5.2.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}