import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.springframework.boot") version "2.4.6"
  id("io.spring.dependency-management") version "1.0.11.RELEASE"

  kotlin("jvm") version "1.5.0"
  kotlin("plugin.spring") version "1.5.0"

  id("eclipse")
}

group = "io.meshcloud"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-hateoas")
  implementation("org.springframework.cloud:spring-cloud-starter-open-service-broker:3.3.0")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  implementation("org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r")
  implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:6.10.0.202406032230-r")

  implementation("commons-io:commons-io:2.4")
  implementation("io.github.microutils:kotlin-logging:1.4.9")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.0.2.RELEASE")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("org.junit.vintage:junit-vintage-engine")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "11"
  }
}

tasks.withType<Test> {
  useJUnit()
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
  launchScript()
}
