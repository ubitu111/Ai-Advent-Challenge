import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "ru.mirtomsk.androidapp"
    compileSdk = libs.versions.compileSdk.get().toInt()
    
    defaultConfig {
        applicationId = "ru.mirtomsk.androidapp"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        
        // NDK configuration for llama.cpp native library
        ndk {
            // Specify ABIs for which to build native libraries
            // Uncomment and modify as needed when you compile llama.cpp
            // abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
        
        // External native build configuration (for compiling llama.cpp)
        // Uncomment when ready to compile llama.cpp from source
        /*
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
                version = "3.22.1"
            }
        }
        */
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    // Source sets for native libraries
    // Place compiled libllama.so files in src/main/jniLibs/<abi>/libllama.so
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
    
    // Packaging options to include native libraries
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.activity.compose)
    implementation(libs.compose.preview)
}

// Make Android build depend on API config generation
afterEvaluate {
    tasks.named("mergeDebugAssets").dependsOn(":shared:generateApiConfig")
    tasks.named("mergeReleaseAssets").dependsOn(":shared:generateApiConfig")
}

