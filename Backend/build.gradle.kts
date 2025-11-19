plugins {
	java
	id("org.springframework.boot") version "3.5.7"
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

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

// Repositories are defined in the root `settings.gradle.kts` via dependencyResolutionManagement

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.google.firebase:firebase-admin:9.2.0")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	
}


tasks.withType<Test> {
	useJUnitPlatform()
}


