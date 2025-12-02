package ru.mirtomsk.server.data.config

/**
 * Configuration for GitHub API integration
 *
 * These values should be set via environment variables or system properties:
 * - GITHUB_TOKEN: Personal Access Token for GitHub API authentication
 * - GITHUB_OWNER: Repository owner (username or organization)
 * - GITHUB_REPO: Repository name
 *
 * Example:
 * - GITHUB_TOKEN=ghp_xxxxxxxxxxxxxxxxxxxx
 * - GITHUB_OWNER=username
 * - GITHUB_REPO=my-repo
 */
object GitHubConfig {
    private const val GITHUB_API_BASE_URL = "https://api.github.com"

    /**
     * Get GitHub API base URL
     */
    fun getApiBaseUrl(): String = GITHUB_API_BASE_URL

    /**
     * Get GitHub Personal Access Token from environment variable or system property
     * Priority: System property > Environment variable
     */
    fun getToken(): String? {
        return System.getProperty("github.token")
            ?: System.getenv("GITHUB_TOKEN")
    }

    /**
     * Get repository owner from environment variable or system property
     * Priority: System property > Environment variable
     */
    fun getOwner(): String? {
        return System.getProperty("github.owner")
            ?: System.getenv("GITHUB_OWNER")
    }

    /**
     * Get repository name from environment variable or system property
     * Priority: System property > Environment variable
     */
    fun getRepo(): String? {
        return System.getProperty("github.repo")
            ?: System.getenv("GITHUB_REPO")
    }
}
