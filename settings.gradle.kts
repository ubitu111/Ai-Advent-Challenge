pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ai-advent-challenge"
include(":shared")
include(":androidApp")
include(":macosApp")
include(":server")

