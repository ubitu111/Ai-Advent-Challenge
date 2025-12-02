package ru.mirtomsk.server.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO models for GitHub API responses
 */

@Serializable
data class GitHubCommitResponse(
    val sha: String,
    val commit: GitHubCommit,
    val author: GitHubUser? = null,
    val committer: GitHubUser? = null,
    val url: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null
)

@Serializable
data class GitHubCommit(
    val message: String,
    val author: GitHubCommitAuthor,
    val committer: GitHubCommitAuthor,
    val url: String? = null
)

@Serializable
data class GitHubCommitAuthor(
    val name: String,
    val email: String,
    val date: String
)

@Serializable
data class GitHubUser(
    val login: String,
    val id: Long? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val url: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null
)

@Serializable
data class GitHubBranchResponse(
    val name: String,
    val commit: GitHubBranchCommit,
    @SerialName("protected") val isProtected: Boolean? = null
)

@Serializable
data class GitHubBranchCommit(
    val sha: String,
    val url: String? = null
)

@Serializable
data class GitHubRepositoryResponse(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val description: String? = null,
    @SerialName("default_branch") val defaultBranch: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("pushed_at") val pushedAt: String? = null,
    val owner: GitHubUser? = null,
    @SerialName("html_url") val htmlUrl: String? = null
)
