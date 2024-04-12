import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

val camelVersion: String by extra
val commonsCompressVersion: String by extra
val springCloudAwsVersion: String by extra
val testcontainersVersion: String by extra
val javaVersion = JavaVersion.VERSION_21

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion.majorVersion))
        // vendor.set(JvmVendorSpec.AMAZON)
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencyManagement {
    imports {
        mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:$springCloudAwsVersion")
        mavenBom("org.apache.camel.springboot:camel-spring-boot-bom:$camelVersion")
    }
    dependencies {
        dependencySet("org.testcontainers:$testcontainersVersion") {
            entry("localstack")
            entry("junit-jupiter")
            entry("testcontainers")
        }
        dependency("org.apache.commons:commons-compress:$commonsCompressVersion")
    }
}

dependencies {
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sns")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.apache.camel.springboot:camel-jackson-starter")
    implementation("org.apache.camel.springboot:camel-aws2-s3-starter")
    implementation("org.apache.camel.springboot:camel-aws2-sns-starter")
    implementation("org.apache.camel.springboot:camel-aws2-sqs-starter")
    implementation("org.apache.camel.springboot:camel-spring-boot-starter")
    implementation("org.hibernate.validator:hibernate-validator")

    testImplementation("io.awspring.cloud:spring-cloud-aws-testcontainers")
    testImplementation("org.awaitility:awaitility-kotlin")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:localstack")
    testImplementation("org.testcontainers:testcontainers")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        languageVersion.set(KotlinVersion.KOTLIN_1_9)
        freeCompilerArgs.set(listOf("-Xjsr305=strict"))
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showCauses = true
        showExceptions = true
        showStackTraces = true
    }
    jvmArgs = listOf("-XX:+EnableDynamicAgentLoading")
}
