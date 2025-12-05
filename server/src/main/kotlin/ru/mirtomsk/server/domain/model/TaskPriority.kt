package ru.mirtomsk.server.domain.model

/**
 * Enum representing task priority levels
 */
enum class TaskPriority(val displayName: String) {
    LOW("низкий"),
    MEDIUM("средний"),
    HIGH("высокий");
    
    companion object {
        /**
         * Find priority by display name (case-insensitive)
         */
        fun fromDisplayName(displayName: String): TaskPriority? {
            return entries.find { it.displayName.equals(displayName, ignoreCase = true) }
        }
        
        /**
         * Find priority by enum name (case-insensitive)
         */
        fun fromName(name: String): TaskPriority? {
            return entries.find { it.name.equals(name, ignoreCase = true) }
        }
        
        /**
         * Get all display names as list
         */
        fun getAllDisplayNames(): List<String> {
            return entries.map { it.displayName }
        }
        
        /**
         * Get all enum names as list
         */
        fun getAllNames(): List<String> {
            return entries.map { it.name }
        }
    }
}
