package ru.mirtomsk.server.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import ru.mirtomsk.server.data.config.GitConfig
import ru.mirtomsk.server.domain.service.LocalBranchInfo
import ru.mirtomsk.server.domain.service.LocalCommitInfo
import ru.mirtomsk.server.domain.service.LocalGitService
import ru.mirtomsk.server.domain.service.LocalGitStatus
import java.io.File

/**
 * Implementation of LocalGitService using local Git commands
 */
class LocalGitCommandService : LocalGitService {

    override suspend fun getRepositoryStatus(): LocalGitStatus? {
        val repoDir = requireGitRepo()

        return try {
            withContext(Dispatchers.IO) {
                // Get current branch
                val currentBranch = executeGitCommand(repoDir, "rev-parse", "--abbrev-ref", "HEAD")
                    ?: return@withContext null

                // Get status
                val statusOutput = executeGitCommand(repoDir, "status", "--porcelain")
                    ?: ""

                // Parse status output
                val lines = statusOutput.lines().filter { it.isNotBlank() }
                val modifiedFiles = mutableListOf<String>()
                val untrackedFiles = mutableListOf<String>()
                val stagedFiles = mutableListOf<String>()

                lines.forEach { line ->
                    when {
                        line.startsWith("??") -> {
                            // Untracked file
                            untrackedFiles.add(line.substring(3).trim())
                        }
                        line.startsWith(" M") || line.startsWith("MM") -> {
                            // Modified but not staged
                            modifiedFiles.add(line.substring(3).trim())
                        }
                        line.startsWith("M ") || line.startsWith("A ") || line.startsWith("D ") -> {
                            // Staged changes
                            stagedFiles.add(line.substring(3).trim())
                        }
                        line.startsWith("MM") -> {
                            // Modified in both index and working tree
                            val fileName = line.substring(3).trim()
                            modifiedFiles.add(fileName)
                            stagedFiles.add(fileName)
                        }
                    }
                }

                // Get last commit
                val lastCommitSha = executeGitCommand(repoDir, "rev-parse", "--short", "HEAD")
                val lastCommitMessage = executeGitCommand(
                    repoDir, "log", "-1", "--pretty=format:%s"
                )

                val isClean = modifiedFiles.isEmpty() && untrackedFiles.isEmpty() && stagedFiles.isEmpty()

                LocalGitStatus(
                    currentBranch = currentBranch.trim(),
                    isClean = isClean,
                    modifiedFiles = modifiedFiles,
                    untrackedFiles = untrackedFiles,
                    stagedFiles = stagedFiles,
                    lastCommit = lastCommitSha?.trim(),
                    lastCommitMessage = lastCommitMessage?.trim()
                )
            }
        } catch (e: Exception) {
            logger.error("Error getting repository status: ${e.message}")
            null
        }
    }

    override suspend fun getCommitHistory(limit: Int, branch: String?): List<LocalCommitInfo>? {
        val repoDir = requireGitRepo()

        return try {
            withContext(Dispatchers.IO) {
                val actualLimit = limit.coerceIn(1, 1000)

                val command = mutableListOf(
                    "log",
                    "--pretty=format:%H|%s|%an|%ad",
                    "--date=iso",
                    "-n", actualLimit.toString()
                )

                if (branch != null) {
                    command.add(branch)
                }

                val output = executeGitCommand(repoDir, *command.toTypedArray())
                    ?: return@withContext emptyList()

                val commits = output.lines()
                    .filter { it.isNotBlank() }
                    .mapNotNull { line ->
                        val parts = line.split("|", limit = 4)
                        if (parts.size >= 4) {
                            LocalCommitInfo(
                                sha = parts[0].take(7), // Short SHA
                                message = parts[1],
                                author = parts[2],
                                date = parts[3],
                                branch = branch
                            )
                        } else {
                            null
                        }
                    }

                commits
            }
        } catch (e: Exception) {
            logger.error("Error getting commit history: ${e.message}")
            null
        }
    }

    override suspend fun getBranches(): List<LocalBranchInfo>? {
        val repoDir = requireGitRepo()

        return try {
            withContext(Dispatchers.IO) {
                // Get current branch
                val currentBranch = executeGitCommand(repoDir, "rev-parse", "--abbrev-ref", "HEAD")
                    ?: return@withContext null

                // Get all branches (simple format, compatible with older git versions)
                val branchesOutput = executeGitCommand(repoDir, "branch")
                    ?: return@withContext emptyList()

                val branches = branchesOutput.lines()
                    .filter { it.isNotBlank() }
                    .mapNotNull { line ->
                        // Remove leading * and spaces
                        val branchName = line.trim().removePrefix("*").trim()
                        if (branchName.isNotBlank()) {
                            // Get last commit SHA for this branch
                            val lastCommitSha = executeGitCommand(
                                repoDir, "rev-parse", "--short", branchName
                            )?.trim() ?: "unknown"

                            // Get last commit message for this branch
                            val lastCommitMessage = executeGitCommand(
                                repoDir, "log", "-1", "--pretty=format:%s", branchName
                            )?.trim()

                            LocalBranchInfo(
                                name = branchName,
                                isCurrent = branchName == currentBranch.trim(),
                                lastCommitSha = lastCommitSha,
                                lastCommitMessage = lastCommitMessage
                            )
                        } else {
                            null
                        }
                    }

                branches
            }
        } catch (e: Exception) {
            logger.error("Error getting branches: ${e.message}")
            null
        }
    }

    /**
     * Execute git command in the repository directory
     */
    private suspend fun executeGitCommand(
        repoDir: File,
        vararg args: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val command = mutableListOf("git")
            command.addAll(args)

            val process = ProcessBuilder(*command.toTypedArray())
                .directory(repoDir)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                output
            } else {
                logger.error("Git command failed with exit code $exitCode: ${command.joinToString(" ")}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error executing git command: ${e.message}")
            null
        }
    }

    override suspend fun getDiff(filePath: String?, staged: Boolean): String? {
        val repoDir = requireGitRepo()

        return try {
            withContext(Dispatchers.IO) {
                val command = mutableListOf("diff")
                
                if (staged) {
                    command.add("--cached")
                }
                
                if (filePath != null) {
                    command.add("--")
                    command.add(filePath)
                }

                // Git diff returns exit code 1 when there are differences (which is normal)
                // Exit code 0 means no differences
                // Exit code > 1 means error
                executeGitDiffCommand(repoDir, *command.toTypedArray())
            }
        } catch (e: Exception) {
            logger.error("Error getting diff: ${e.message}")
            null
        }
    }

    /**
     * Execute git diff command (handles exit code 1 as success, since diff returns 1 when there are differences)
     */
    private suspend fun executeGitDiffCommand(
        repoDir: File,
        vararg args: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val command = mutableListOf("git")
            command.addAll(args)

            val process = ProcessBuilder(*command.toTypedArray())
                .directory(repoDir)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            // Git diff returns:
            // - 0: no differences
            // - 1: has differences (this is normal, not an error)
            // - >1: actual error
            when (exitCode) {
                0, 1 -> output
                else -> {
                    logger.error("Git diff command failed with exit code $exitCode: ${command.joinToString(" ")}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Error executing git diff command: ${e.message}")
            null
        }
    }

    private fun requireGitRepo(): File {
        return requireNotNull(GitConfig.getRepoDirectory()) { "Git repository not found" }
    }

    private companion object {

        private val logger = LoggerFactory.getLogger(LocalGitCommandService::class.java)
    }
}

