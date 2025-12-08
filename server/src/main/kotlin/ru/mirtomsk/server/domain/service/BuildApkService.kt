package ru.mirtomsk.server.domain.service

/**
 * Domain service interface for building signed release APK
 */
interface BuildApkService {
    /**
     * Build signed release APK from Git repository
     * @return Path to the built APK file, or null if build failed
     */
    suspend fun buildReleaseApk(): String?
}
