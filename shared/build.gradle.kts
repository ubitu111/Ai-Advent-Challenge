import java.util.Properties
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "ru.mirtomsk.shared"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Read local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { inputStream ->
        InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
            localProperties.load(reader)
        }
    }
}

val apiKey: String = localProperties.getProperty("secret_key") ?: ""
val keyId: String = localProperties.getProperty("key_id") ?: ""
val mcpgateToken: String = localProperties.getProperty("mcpgate_token") ?: ""
val localModelEnabled: String = localProperties.getProperty("local.model.enabled") ?: ""
val localModelBaseUrl: String = localProperties.getProperty("local.model.base.url") ?: ""
val localModelName: String = localProperties.getProperty("local.model.name") ?: ""

// Task to generate API config file
tasks.register("generateApiConfig") {
    doFirst {
        // Build properties content
        val propertiesContent = buildString {
            appendLine("api.key=$apiKey")
            appendLine("api.key.id=$keyId")
            appendLine("mcpgate.token=$mcpgateToken")
            appendLine("local.model.enabled=$localModelEnabled")
            appendLine("local.model.base.url=$localModelBaseUrl")
            appendLine("local.model.name=$localModelName")
        }
        
        // Generate for commonMain resources (for desktop)
        val apiConfigFile = file("src/commonMain/resources/api.properties")
        apiConfigFile.parentFile.mkdirs()
        apiConfigFile.writeText(propertiesContent, StandardCharsets.UTF_8)
        
        // Generate for Android assets
        val androidAssetsDir = rootProject.file("androidApp/src/main/assets")
        androidAssetsDir.mkdirs()
        val androidApiConfigFile = File(androidAssetsDir, "api.properties")
        androidApiConfigFile.writeText(propertiesContent, StandardCharsets.UTF_8)
    }
}

// Make compilation depend on API config generation
tasks.configureEach {
    if (name.startsWith("compileKotlin")) {
        dependsOn("generateApiConfig")
    }
}

// Configure processResources tasks to handle duplicates
afterEvaluate {
    tasks.withType<org.gradle.api.tasks.Copy>().configureEach {
        if (name.contains("ProcessResources")) {
            duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.INCLUDE
        }
    }
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = libs.versions.jvmTarget.get()
            }
        }
    }

    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = libs.versions.jvmTarget.get().toInt().toString()
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material)
                
                // Ktor
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx.json)
                
                // Serialization
                implementation(libs.kotlinx.serialization.json)
                
                // Koin
                implementation(libs.koin.core)
                
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                
                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            }
            resources.srcDirs("src/commonMain/resources")
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.compose.preview)
                implementation(libs.compose.ui.tooling)
                
                // Ktor Android engine
                implementation(libs.ktor.client.android)
                
                // Coroutines Android Main dispatcher
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

                implementation(libs.androidx.core)
            }
        }

        val desktopMain by getting {
            dependsOn(commonMain)
            
            dependencies {
                // Ktor CIO engine for desktop
                implementation(libs.ktor.client.cio)
                
                // Coroutines Swing Main dispatcher for desktop
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
            }
        }
    }
}

