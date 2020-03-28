plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    // Teamspeak API's
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    api ("com.github.theholywaffle","teamspeak3-api","1.2.0")
    api ("com.github.manevolent","ts3j","1.0")
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