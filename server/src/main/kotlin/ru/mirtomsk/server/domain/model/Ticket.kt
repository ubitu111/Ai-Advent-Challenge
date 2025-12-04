package ru.mirtomsk.server.domain.model

/**
 * Domain model representing a CRM ticket
 */
data class Ticket(
    val username: String,
    val date: String,
    val title: String,
    val question: String,
    val answer: String? = null
)
