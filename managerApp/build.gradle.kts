plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.spring") version "1.3.61"
    kotlin("plugin.jpa") version "1.3.61"
    id("org.springframework.boot") version "2.2.5.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

repositories {
    mavenCentral()
}

val developmentOnly by configurations.creating
configurations {
    runtimeClasspath {
        extendsFrom(developmentOnly)
    }
}

dependencies {
    implementation(project(":teamspeakClient")) {
        exclude("org.slf4j", "slf4j-simple")
    }
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.hibernate","hibernate-envers")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:2.6.0")
    runtimeOnly("com.h2database:h2:1.3.176")

    // Password strength estimation:
    implementation("me.gosimple", "nbvcxz", "1.4.3")


    // Webjars (Weblibs)
    implementation("org.webjars:font-awesome:5.13.0")
    implementation("org.webjars:jquery-easing:1.4.1")
    implementation("org.webjars.bower:startbootstrap-sb-admin-2:4.0.7") { // Template
        exclude("org.webjars.bower","jquery.easing")
        exclude("org.webjars.bower","fortawesome__fontawesome-free")

    }

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude("org.junit.vintage", "junit-vintage-engine")
    }



    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}