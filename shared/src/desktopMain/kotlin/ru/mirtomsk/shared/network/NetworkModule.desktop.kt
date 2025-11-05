package ru.mirtomsk.shared.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

actual fun createHttpClientEngine(): HttpClientEngine = CIO.create()

