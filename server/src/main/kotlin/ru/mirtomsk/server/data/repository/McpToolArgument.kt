package ru.mirtomsk.server.data.repository

/**
 * Enum for MCP tool argument names
 * Centralizes argument names for type safety and easier maintenance
 */
enum class McpToolArgument {
    CITY,
    HOURS,
    EXPRESSION,
    BASE_CURRENCY,
    TARGET_CURRENCY,
    DATE,
    LATITUDE,
    LONGITUDE,
    LIMIT,
    BRANCH,
    FILE_PATH,
    STAGED,
    USERNAME,
    TITLE,
    QUESTION,
    ANSWER;
    
    /**
     * Get argument name as lowercase string (for JSON keys)
     */
    fun key(): String = name.lowercase()
}
