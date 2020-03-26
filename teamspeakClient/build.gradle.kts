plugins {
    kotlin("jvm")
}

group = "eu.redasurc"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven (
            url = "https://jitpack.io"
    )
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    // Teamspeak API's
    implementation("com.github.theholywaffle","teamspeak3-api","1.2.0")
    implementation("com.github.manevolent","ts3j","1.0")
    implementation("org.slf4j","slf4j-simple","1.6.1")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}