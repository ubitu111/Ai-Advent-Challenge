package ru.mirtomsk.server.data.model

import kotlinx.serialization.Serializable
import ru.mirtomsk.server.domain.model.McpTool
import ru.mirtomsk.server.domain.model.McpToolInputSchema
import ru.mirtomsk.server.domain.model.McpToolProperty

/**
 * DTO for MCP Tool (for serialization)
 */
@Serializable
data class McpToolDto(
    val name: String,
    val description: String? = null,
    val inputSchema: McpToolInputSchemaDto? = null,
)

/**
 * DTO for MCP Tool Input Schema
 */
@Serializable
data class McpToolInputSchemaDto(
    val type: String? = null,
    val properties: Map<String, McpToolPropertyDto>? = null,
    val required: List<String>? = null,
)

/**
 * DTO for MCP Tool Property
 */
@Serializable
data class McpToolPropertyDto(
    val type: String? = null,
    val description: String? = null,
)

/**
 * Mapper functions for converting between domain models and DTOs
 */
fun McpTool.toDto(): McpToolDto {
    return McpToolDto(
        name = name,
        description = description,
        inputSchema = inputSchema?.toDto(),
    )
}

fun McpToolDto.toDomain(): McpTool {
    return McpTool(
        name = name,
        description = description,
        inputSchema = inputSchema?.toDomain(),
    )
}

fun McpToolInputSchema.toDto(): McpToolInputSchemaDto {
    return McpToolInputSchemaDto(
        type = type,
        properties = properties?.mapValues { it.value.toDto() },
        required = required,
    )
}

fun McpToolInputSchemaDto.toDomain(): McpToolInputSchema {
    return McpToolInputSchema(
        type = type,
        properties = properties?.mapValues { it.value.toDomain() },
        required = required,
    )
}

fun McpToolProperty.toDto(): McpToolPropertyDto {
    return McpToolPropertyDto(
        type = type,
        description = description,
    )
}

fun McpToolPropertyDto.toDomain(): McpToolProperty {
    return McpToolProperty(
        type = type,
        description = description,
    )
}
