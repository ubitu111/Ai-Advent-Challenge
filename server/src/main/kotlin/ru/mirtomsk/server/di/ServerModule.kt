package ru.mirtomsk.server.di

import ru.mirtomsk.server.data.repository.McpToolRepositoryImpl
import ru.mirtomsk.server.domain.repository.McpToolRepository
import ru.mirtomsk.server.domain.usecase.CallToolUseCase
import ru.mirtomsk.server.domain.usecase.GetToolsUseCase
import ru.mirtomsk.server.presentation.controller.McpController

/**
 * Dependency injection module for server
 * In a real-world scenario, this could use a DI framework like Koin or Kodein
 * For simplicity, we use manual dependency injection
 */
object ServerModule {

    // Repository
    private val mcpToolRepository: McpToolRepository = McpToolRepositoryImpl()

    // Use cases
    val getToolsUseCase: GetToolsUseCase = GetToolsUseCase(mcpToolRepository)
    val callToolUseCase: CallToolUseCase = CallToolUseCase(mcpToolRepository)

    // Controllers
    val mcpController: McpController = McpController(
        getToolsUseCase = getToolsUseCase,
        callToolUseCase = callToolUseCase
    )
}
