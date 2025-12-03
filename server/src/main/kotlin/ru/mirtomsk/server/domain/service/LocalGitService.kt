package ru.mirtomsk.server.domain.service

/**
 * Domain service interface for local Git operations
 */
interface LocalGitService {
    /**
     * Get repository status (similar to git status)
     * Returns information about working directory status
     */
    suspend fun getRepositoryStatus(): LocalGitStatus?

    /**
     * Get commit history (similar to git log)
     * @param limit Maximum number of commits to return (default: 30)
     * @param branch Branch name (optional, defaults to current branch)
     */
    suspend fun getCommitHistory(limit: Int = 30, branch: String? = null): List<LocalCommitInfo>?

    /**
     * Get list of branches (similar to git branch)
     */
    suspend fun getBranches(): List<LocalBranchInfo>?
}

/**
 * Domain model for local git status
 */
data class LocalGitStatus(
    val currentBranch: String,
    val isClean: Boolean,
    val modifiedFiles: List<String>,
    val untrackedFiles: List<String>,
    val stagedFiles: List<String>,
    val lastCommit: String?,
    val lastCommitMessage: String?
)

/**
 * Domain model for local commit information
 */
data class LocalCommitInfo(
    val sha: String,
    val message: String,
    val author: String,
    val date: String,
    val branch: String?
)

/**
 * Domain model for local branch information
 */
data class LocalBranchInfo(
    val name: String,
    val isCurrent: Boolean,
    val lastCommitSha: String,
    val lastCommitMessage: String?
)

