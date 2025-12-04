package ru.mirtomsk.server.domain.service

import ru.mirtomsk.server.domain.model.Ticket

/**
 * Service for managing CRM tickets
 */
interface TicketService {
    /**
     * Get all tickets
     */
    suspend fun getAllTickets(): List<Ticket>
    
    /**
     * Create a new ticket
     */
    suspend fun createTicket(ticket: Ticket): Ticket
}
