package ru.mirtomsk.shared.dollarRate

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Duration.Companion.minutes

/**
 * Scheduler for periodic dollar rate requests
 * Executes requests every minute in the background
 */
class DollarRateScheduler(
    private val repository: DollarRateRepository,
    private val viewModel: DollarRateViewModel,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val schedulerScope = CoroutineScope(ioDispatcher + SupervisorJob())
    private var schedulerJob: Job? = null

    /**
     * Start the scheduler
     * Will execute requests every minute
     */
    fun start() {
        if (schedulerJob?.isActive == true) {
            return // Already running
        }

        schedulerJob = schedulerScope.launch {
            // execute every minute
            while (true) {
                delay(1.minutes)
                executeRequest()
            }
        }
    }

    /**
     * Stop the scheduler
     */
    fun stop() {
        schedulerJob?.cancel()
        schedulerJob = null
    }

    /**
     * Execute a dollar rate request
     */
    private suspend fun executeRequest() {
        try {
            // Calculate dates for the last week
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val dates = (0..6).map { daysAgo ->
                val date = today.minus(daysAgo, DateTimeUnit.DAY)
                date.toString() // Format: YYYY-MM-DD
            }.reversed() // Oldest to newest

            viewModel.updateLoading(true)
            val result = repository.requestDollarRateSummary(dates)
            viewModel.updateDollarRateInfo(result)
        } catch (e: Exception) {
            viewModel.updateError("Ошибка при запросе курса доллара: ${e.message}")
        } finally {
            viewModel.updateLoading(false)
        }
    }
}
