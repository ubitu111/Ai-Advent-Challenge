package ru.mirtomsk.server.domain.model

/**
 * Enum representing task status
 */
enum class TaskStatus(val displayName: String) {
    NEW("новая"),
    IN_PROGRESS("в работе"),
    COMPLETED("завершенная");
    
    companion object {
        /**
         * Find status by enum name (case-insensitive)
         */
        fun fromName(name: String): TaskStatus? {
            return entries.find { it.name.equals(name, ignoreCase = true) }
        }
        
        /**
         * Get all enum names as list
         */
        fun getAllNames(): List<String> {
            return entries.map { it.name }
        }
    }
}
