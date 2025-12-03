package ru.mirtomsk.server.data.config

import java.io.File

/**
 * Configuration for local Git repository integration
 *
 * These values should be set via environment variables or system properties:
 * - GIT_REPO_PATH: Path to the local Git repository directory
 *
 * Example:
 * - GIT_REPO_PATH=/path/to/your/repo
 * - GIT_REPO_PATH=/Users/username/projects/my-repo
 */
object GitConfig {
    /**
     * Get Git repository path from environment variable or system property
     * Priority: System property > Environment variable
     */
    fun getRepoPath(): String? {
        return System.getProperty("git.repo.path")
            ?: System.getenv("GIT_REPO_PATH")
    }

    /**
     * Get Git repository directory as File
     * @return File object representing the repository directory, or null if not configured
     */
    fun getRepoDirectory(): File? {
        val path = requireNotNull(getRepoPath()) { "Git repository path is not configured" }
        val dir = File(path)
        require(dir.exists()) { "Git repository directory does not exist" }
        require(dir.isDirectory) { "Git repository path is not a directory" }
        return dir
    }
}

