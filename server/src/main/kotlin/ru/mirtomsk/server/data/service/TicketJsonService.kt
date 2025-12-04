package ru.mirtomsk.server.data.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.mirtomsk.server.domain.model.Ticket
import ru.mirtomsk.server.domain.service.TicketService
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Implementation of TicketService that stores tickets in a JSON file
 */
class TicketJsonService(
    private val ticketsFilePath: String = "server/tickets.json"
) : TicketService {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    @Serializable
    private data class TicketDto(
        val username: String,
        val date: String,
        val title: String,
        val question: String,
        val answer: String? = null
    )
    
    private fun Ticket.toDto(): TicketDto {
        return TicketDto(
            username = username,
            date = date,
            title = title,
            question = question,
            answer = answer
        )
    }
    
    private fun TicketDto.toDomain(): Ticket {
        return Ticket(
            username = username,
            date = date,
            title = title,
            question = question,
            answer = answer
        )
    }
    
    override suspend fun getAllTickets(): List<Ticket> {
        return try {
            val file = File(ticketsFilePath)
            if (!file.exists() || file.length() == 0L) {
                return emptyList()
            }
            
            val jsonContent = file.readText()
            val tickets = json.decodeFromString<List<TicketDto>>(jsonContent)
            tickets.map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun createTicket(ticket: Ticket): Ticket {
        val file = File(ticketsFilePath)
        val existingTickets = getAllTickets().toMutableList()
        
        // Ensure date is set (use current date if not provided)
        val ticketWithDate = if (ticket.date.isBlank()) {
            ticket.copy(date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
        } else {
            ticket
        }
        
        existingTickets.add(ticketWithDate)
        
        // Write to file
        val ticketsDto = existingTickets.map { it.toDto() }
        val jsonContent = json.encodeToString(ticketsDto)
        file.writeText(jsonContent)
        
        return ticketWithDate
    }
}
