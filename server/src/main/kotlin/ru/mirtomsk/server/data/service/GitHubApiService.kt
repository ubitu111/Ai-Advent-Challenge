package ru.mirtomsk.server.data.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import ru.mirtomsk.server.data.config.GitHubConfig
import ru.mirtomsk.server.data.model.GitHubBranchResponse
import ru.mirtomsk.server.data.model.GitHubCommitResponse
import ru.mirtomsk.server.data.model.GitHubRepositoryResponse
import ru.mirtomsk.server.domain.service.BranchInfo
import ru.mirtomsk.server.domain.service.CommitInfo
import ru.mirtomsk.server.domain.service.GitHubService
import ru.mirtomsk.server.domain.service.RepositoryStatus

/**
 * Implementation of GitHubService using GitHub REST API
 */
class GitHubApiService(
    private val httpClient: HttpClient
) : GitHubService {

    companion object {
        private const val API_VERSION = "2022-11-28"
    }

    override suspend fun getRepositoryStatus(): RepositoryStatus? {
        val owner = requireGithubOwner()
        val repo = requireGithubRepo()
        val token = requireGithubToken()

        return try {
            val response: GitHubRepositoryResponse =
                httpClient.get("${GitHubConfig.getApiBaseUrl()}/repos/$owner/$repo") {
                    header("Authorization", "Bearer $token")
                    header("Accept", "application/vnd.github+json")
                    header("X-GitHub-Api-Version", API_VERSION)
                }.body()

            RepositoryStatus(
                name = response.name,
                fullName = response.fullName,
                description = response.description,
                defaultBranch = response.defaultBranch,
                lastUpdated = response.updatedAt,
                lastPushed = response.pushedAt,
                owner = response.owner?.login,
                url = response.htmlUrl
            )
        } catch (e: Exception) {
            println("Error getting repository status: ${e.message}")
            null
        }
    }

    override suspend fun getCommitHistory(limit: Int, branch: String?): List<CommitInfo>? {
        val owner = requireGithubOwner()
        val repo = requireGithubRepo()
        val token = requireGithubToken()
        val actualLimit = limit.coerceIn(1, 100)

        return try {
            val response: List<GitHubCommitResponse> =
                httpClient.get("${GitHubConfig.getApiBaseUrl()}/repos/$owner/$repo/commits") {
                    header("Authorization", "Bearer $token")
                    header("Accept", "application/vnd.github+json")
                    header("X-GitHub-Api-Version", API_VERSION)
                    parameter("per_page", actualLimit)
                    if (branch != null) {
                        parameter("sha", branch)
                    }
                }.body()

            response.map { commit ->
                CommitInfo(
                    sha = commit.sha.take(7), // Short SHA
                    message = commit.commit.message.split("\n")
                        .first(), // First line of commit message
                    author = commit.commit.author.name,
                    authorEmail = commit.commit.author.email,
                    date = commit.commit.author.date,
                    url = commit.htmlUrl
                )
            }
        } catch (e: Exception) {
            println("Error getting commit history: ${e.message}")
            null
        }
    }

    override suspend fun getBranches(): List<BranchInfo>? {
        val owner = requireGithubOwner()
        val repo = requireGithubRepo()
        val token = requireGithubToken()

        return try {
            val response: List<GitHubBranchResponse> =
                httpClient.get("${GitHubConfig.getApiBaseUrl()}/repos/$owner/$repo/branches") {
                    header("Authorization", "Bearer $token")
                    header("Accept", "application/vnd.github+json")
                    header("X-GitHub-Api-Version", API_VERSION)
                    parameter("per_page", 100) // GitHub API max
                }.body()

            response.map { branch ->
                BranchInfo(
                    name = branch.name,
                    lastCommitSha = branch.commit.sha.take(7), // Short SHA
                    isProtected = branch.isProtected ?: false
                )
            }
        } catch (e: Exception) {
            println("Error getting branches: ${e.message}")
            null
        }
    }

    private fun requireGithubOwner(): String {
        return requireNotNull(GitHubConfig.getOwner()) { "GitHub owner is not set" }
    }

    private fun requireGithubRepo(): String {
        return requireNotNull(GitHubConfig.getRepo()) { "GitHub repo is not set" }
    }

    private fun requireGithubToken(): String {
        return requireNotNull(GitHubConfig.getToken()) { "GitHub token is not set" }
    }
}
