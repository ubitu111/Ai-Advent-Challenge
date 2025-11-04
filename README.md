# AI Advent Challenge - Kotlin Multiplatform Project

A Kotlin Multiplatform project with macOS and Android targets, featuring a shared "Hello World" UI built with Compose Multiplatform.

## Project Structure

- `shared/` - Shared Kotlin Multiplatform code with Compose UI (configured as Android library)
  - Supports Android, JVM Desktop, macOS ARM64, and macOS x64 targets
- `androidApp/` - Android application module
- `macosApp/` - macOS desktop application module (JVM-based using Compose Desktop)

## Technology Stack

- **Kotlin**: 2.1.0
- **Compose Multiplatform**: 1.8.0
- **Compose Compiler Plugin**: 2.1.0 (integrated with Kotlin)
- **Gradle**: 8.9
- **Android Gradle Plugin**: 8.7.0
- **Java**: 17 (required for Android and JVM targets)
- **Android SDK**: 35 (compileSdk and targetSdk)

## Requirements

- JDK 17 or higher
- Android Studio (for Android development)
- Gradle 8.9+ (included via wrapper)

## Building the Project

### Build all modules:
```bash
./gradlew build
```

### Run macOS app:
```bash
./gradlew :macosApp:run
```

### Build Android APK:
```bash
./gradlew :androidApp:assembleDebug
```

### Build macOS DMG:
```bash
./gradlew :macosApp:packageDmg
```

## Running

### macOS
The macOS app will open a window displaying "Hello World" in the center. The app runs on JVM and can be packaged as a native DMG using Compose Desktop's native distributions feature.

### Android
Open the project in Android Studio and run the `androidApp` module on an emulator or device.

## Features

- Shared UI code using Compose Multiplatform
- macOS desktop application (JVM-based with Compose Desktop)
- Android mobile application
- Both platforms display "Hello World" text in a centered screen
- Kotlin 2.1 with K2 compiler support
- Compose Compiler Plugin integrated (required for Kotlin 2.0+)

## Configuration Notes

- All modules using Compose must have the `org.jetbrains.kotlin.plugin.compose` plugin applied
- The shared module is configured as an Android library to support the Android target
- The shared module includes JVM desktop target for macOS app compatibility
- macOS app uses JVM desktop target with Compose Desktop (not native targets)
- iOS targets are explicitly disabled in `gradle.properties` to avoid initialization issues

