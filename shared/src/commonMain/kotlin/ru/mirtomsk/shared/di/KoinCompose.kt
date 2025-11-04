package ru.mirtomsk.shared.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.ParametersHolder
import org.koin.core.qualifier.Qualifier

/**
 * Koin Compose integration for Multiplatform
 * Provides koinInject() function similar to koin-compose
 */
@Composable
inline fun <reified T : Any> koinInject(
    qualifier: Qualifier? = null,
    noinline parameters: (() -> ParametersHolder)? = null
): T {
    val koin = remember { GlobalContext.get() }
    return remember(qualifier, parameters) {
        koin.get<T>(qualifier, parameters)
    }
}

/**
 * Get Koin instance in Composable
 */
@Composable
fun getKoin(): Koin {
    return remember { GlobalContext.get() }
}

