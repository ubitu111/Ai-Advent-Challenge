package ru.mirtomsk.server.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import ru.mirtomsk.server.data.config.GitConfig
import ru.mirtomsk.server.domain.service.BuildApkService
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Implementation of BuildApkService using Gradle commands
 */
class BuildApkGradleService : BuildApkService {

    override suspend fun buildReleaseApk(): String? {
        val repoDir = requireGitRepo()
        val outputDir = getOutputDirectory()

        return try {
            withContext(Dispatchers.IO) {
                // Ensure output directory exists
                outputDir.mkdirs()

                // Build signed release APK using Gradle
                logger.info("Starting APK build in repository: ${repoDir.absolutePath}")
                
                // Execute gradlew assembleRelease or bundleRelease depending on what's available
                // First try to find gradlew
                val gradlew = findGradleWrapper(repoDir)
                    ?: return@withContext null

                // Execute build command
                val buildOutput = executeGradleCommand(
                    repoDir,
                    gradlew,
                    "assembleRelease"
                )

                if (buildOutput == null) {
                    logger.error("Gradle build failed")
                    return@withContext null
                }

                logger.info("Gradle build completed successfully")

                // Find the built APK file
                val apkFile = findBuiltApk(repoDir)
                    ?: return@withContext null

                // Generate new filename with server mark
                val originalName = apkFile.nameWithoutExtension
                val newName = "${originalName}_server.apk"
                val newApkFile = File(outputDir, newName)

                // Copy APK to output directory with new name
                apkFile.copyTo(newApkFile, overwrite = true)

                logger.info("APK copied to: ${newApkFile.absolutePath}")

                newApkFile.absolutePath
            }
        } catch (e: Exception) {
            logger.error("Error building APK: ${e.message}", e)
            null
        }
    }

    /**
     * Find Gradle wrapper script
     */
    private fun findGradleWrapper(repoDir: File): File? {
        val gradlew = when {
            System.getProperty("os.name").lowercase().contains("win") -> {
                File(repoDir, "gradlew.bat")
            }
            else -> {
                File(repoDir, "gradlew")
            }
        }

        return if (gradlew.exists() && gradlew.canExecute()) {
            gradlew
        } else {
            logger.error("Gradle wrapper not found at: ${gradlew.absolutePath}")
            null
        }
    }

    /**
     * Execute Gradle command
     */
    private suspend fun executeGradleCommand(
        repoDir: File,
        gradlew: File,
        vararg args: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val command = mutableListOf(gradlew.absolutePath)
            command.addAll(args)

            val process = ProcessBuilder(*command.toTypedArray())
                .directory(repoDir)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                logger.debug("Gradle command output: $output")
                output
            } else {
                logger.error("Gradle command failed with exit code $exitCode: ${command.joinToString(" ")}")
                logger.error("Output: $output")
                null
            }
        } catch (e: Exception) {
            logger.error("Error executing Gradle command: ${e.message}", e)
            null
        }
    }

    /**
     * Find built APK file in the repository
     */
    private fun findBuiltApk(repoDir: File): File? {
        // Common locations for Android APK files
        val possiblePaths = listOf(
            File(repoDir, "androidApp/build/outputs/apk/release"),
            File(repoDir, "app/build/outputs/apk/release"),
            File(repoDir, "build/outputs/apk/release"),
            File(repoDir, "android/build/outputs/apk/release"),
        )

        for (path in possiblePaths) {
            if (path.exists() && path.isDirectory) {
                val apkFiles = path.listFiles { file ->
                    file.isFile && file.extension.lowercase() == "apk"
                }
                
                // Find release APK (prefer signed release)
                val releaseApk = apkFiles?.firstOrNull { file ->
                    file.name.contains("release", ignoreCase = true) &&
                    !file.name.contains("unsigned", ignoreCase = true)
                } ?: apkFiles?.firstOrNull()

                if (releaseApk != null) {
                    logger.info("Found APK at: ${releaseApk.absolutePath}")
                    return releaseApk
                }
            }
        }

        logger.error("APK file not found in repository")
        return null
    }

    /**
     * Get output directory for built APKs
     */
    private fun getOutputDirectory(): File {
        // Get server module directory
        // Try to get from class location, fallback to user.dir
        val serverDir = try {
            val currentFile = File(javaClass.protectionDomain.codeSource.location.toURI())
            // Navigate up from classes to server/src/main/kotlin/.../server/.../BuildApkGradleService.class
            // We need to go: classes -> main -> kotlin -> ru -> mirtomsk -> server -> data -> service
            // Then up to server root
            currentFile.parentFile?.parentFile?.parentFile?.parentFile?.parentFile?.parentFile?.parentFile?.parentFile
                ?: File(System.getProperty("user.dir"))
        } catch (e: Exception) {
            File(System.getProperty("user.dir"))
        }
        
        // If we're in the server directory, use it, otherwise try to find it
        val actualServerDir = if (File(serverDir, "build.gradle.kts").exists()) {
            serverDir
        } else {
            // Try to find server directory from user.dir
            val userDir = File(System.getProperty("user.dir"))
            if (File(userDir, "server").exists()) {
                File(userDir, "server")
            } else {
                serverDir
            }
        }
        
        // Create builds directory in server folder
        val buildsDir = File(actualServerDir, "builds")
        return buildsDir
    }

    private fun requireGitRepo(): File {
        return requireNotNull(GitConfig.getRepoDirectory()) { "Git repository not found" }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(BuildApkGradleService::class.java)
    }
}
