package ru.mirtomsk.server.domain.service

import ru.mirtomsk.server.domain.model.Task
import ru.mirtomsk.server.domain.model.TaskPriority
import ru.mirtomsk.server.domain.model.TaskStatus

/**
 * Service for managing tasks
 */
interface TaskService {
    /**
     * Get all tasks
     */
    suspend fun getAllTasks(): List<Task>
    
    /**
     * Get tasks by priority and status
     */
    suspend fun getTasksByPriorityAndStatus(priority: TaskPriority?, status: TaskStatus?): List<Task>
    
    /**
     * Create a new task
     */
    suspend fun createTask(name: String, description: String, priority: TaskPriority, status: TaskStatus): Task
}
