package ru.mirtomsk.server.data.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.mirtomsk.server.domain.model.Task
import ru.mirtomsk.server.domain.model.TaskPriority
import ru.mirtomsk.server.domain.model.TaskStatus
import ru.mirtomsk.server.domain.service.TaskService
import java.io.File
import java.util.UUID

/**
 * Implementation of TaskService that stores tasks in a JSON file
 */
class TaskJsonService(
    private val tasksFilePath: String = "server/tasks.json"
) : TaskService {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    @Serializable
    private data class TaskDto(
        val id: String,
        val name: String,
        val description: String,
        val priority: String,
        val status: String
    )
    
    private fun Task.toDto(): TaskDto {
        return TaskDto(
            id = id,
            name = name,
            description = description,
            priority = priority.name,
            status = status.name
        )
    }
    
    private fun TaskDto.toDomain(): Task {
        val taskPriority = TaskPriority.fromName(priority) 
            ?: TaskPriority.MEDIUM // Default to MEDIUM if invalid priority
        val taskStatus = TaskStatus.fromName(status)
            ?: TaskStatus.NEW // Default to NEW if invalid status
        return Task(
            id = id,
            name = name,
            description = description,
            priority = taskPriority,
            status = taskStatus
        )
    }
    
    override suspend fun getAllTasks(): List<Task> {
        return try {
            val file = File(tasksFilePath)
            if (!file.exists() || file.length() == 0L) {
                return emptyList()
            }
            
            val jsonContent = file.readText()
            val tasks = json.decodeFromString<List<TaskDto>>(jsonContent)
            tasks.map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun getTasksByPriorityAndStatus(priority: TaskPriority?, status: TaskStatus?): List<Task> {
        val allTasks = getAllTasks()
        
        return allTasks.filter { task ->
            val matchesPriority = priority == null || task.priority == priority
            val matchesStatus = status == null || task.status == status
            matchesPriority && matchesStatus
        }
    }
    
    override suspend fun createTask(name: String, description: String, priority: TaskPriority, status: TaskStatus): Task {
        val file = File(tasksFilePath)
        val existingTasks = getAllTasks().toMutableList()
        
        // Generate unique ID
        val taskId = UUID.randomUUID().toString()
        
        val newTask = Task(
            id = taskId,
            name = name,
            description = description,
            priority = priority,
            status = status
        )
        
        existingTasks.add(newTask)
        
        // Write to file
        val tasksDto = existingTasks.map { it.toDto() }
        val jsonContent = json.encodeToString(tasksDto)
        file.writeText(jsonContent)
        
        return newTask
    }
}
