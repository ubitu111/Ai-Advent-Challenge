package ru.mirtomsk.server.domain.service

/**
 * Domain service interface for GitHub API operations
 */
interface GitHubService {
    /**
     * Get repository status information (similar to git status)
     * Returns information about the repository including last commit, default branch, etc.
     */
    suspend fun getRepositoryStatus(): RepositoryStatus?

    /**
     * Get commit history (similar to git log)
     * @param limit Maximum number of commits to return (default: 30, max: 100)
     * @param branch Branch name (optional, defaults to default branch)
     */
    suspend fun getCommitHistory(limit: Int = 30, branch: String? = null): List<CommitInfo>?

    /**
     * Get list of branches (similar to git branch)
     */
    suspend fun getBranches(): List<BranchInfo>?
}

/**
 * Domain model for repository status
 */
data class RepositoryStatus(
    val name: String,
    val fullName: String,
    val description: String?,
    val defaultBranch: String?,
    val lastUpdated: String?,
    val lastPushed: String?,
    val owner: String?,
    val url: String?
)

/**
 * Domain model for commit information
 */
data class CommitInfo(
    val sha: String,
    val message: String,
    val author: String,
    val authorEmail: String,
    val date: String,
    val url: String?
)

/**
 * Domain model for branch information
 */
data class BranchInfo(
    val name: String,
    val lastCommitSha: String,
    val isProtected: Boolean
)
