package ru.mirtomsk.server.domain.model

/**
 * Domain model representing a task
 */
data class Task(
    val id: String,
    val name: String,
    val description: String,
    val priority: TaskPriority,
    val status: TaskStatus
)
