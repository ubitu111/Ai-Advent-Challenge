plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("ru.mirtomsk.server.ApplicationKt")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cors)
    
    // Ktor Client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

