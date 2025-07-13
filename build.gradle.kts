plugins {
    id("org.springframework.boot") version "3.1.5" apply false
    id("io.spring.dependency-management") version "1.1.3" apply false
    kotlin("jvm") version "1.9.10" apply false
    kotlin("plugin.spring") version "1.9.10" apply false
    java
}

allprojects {
    group = "com.example"
    version = "0.0.1-SNAPSHOT"
    
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    
    java {
        sourceCompatibility = JavaVersion.VERSION_17
    }
    
    dependencies {
        implementation("org.springframework.boot:spring-boot-starter")
        implementation("org.springframework.boot:spring-boot-starter-webflux")
        implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
        implementation("org.springframework.kafka:spring-kafka")
        implementation("org.springframework.boot:spring-boot-starter-validation")
        implementation("org.springframework.boot:spring-boot-starter-actuator")
        implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.2.0")
        implementation("io.swagger.core.v3:swagger-annotations:2.2.16")
        implementation("io.swagger.core.v3:swagger-core:2.2.16")
        implementation("io.swagger.core.v3:swagger-models:2.2.16")
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        implementation("org.apache.commons:commons-lang3:3.13.0")
        implementation("org.apache.commons:commons-collections4:4.4")
        implementation("com.fasterxml.jackson.core:jackson-databind")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:2.15.2")
        implementation("io.netty:netty-resolver-dns-native-macos:4.1.110.Final:osx-aarch_64")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("io.projectreactor:reactor-test")
        testImplementation("org.springframework.kafka:spring-kafka-test")
        testImplementation("org.testcontainers:junit-jupiter")
        testImplementation("org.testcontainers:kafka")
        testImplementation("org.testcontainers:testcontainers")
    }
    
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}