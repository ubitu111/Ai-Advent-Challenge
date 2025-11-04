package ru.mirtomsk.shared

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import ru.mirtomsk.shared.chat.ChatScreen
import ru.mirtomsk.shared.di.appModule

private var koinInitialized = false

/**
 * Initialize Koin dependency injection
 * Should be called once at application startup
 */
fun initKoin(additionalModules: List<Module> = emptyList()): Koin {
    if (!koinInitialized) {
        startKoin {
            modules(appModule)
            if (additionalModules.isNotEmpty()) {
                modules(additionalModules)
            }
        }
        koinInitialized = true
    }
    return org.koin.core.context.GlobalContext.get()
}

@Composable
fun App() {
    // Initialize Koin if not already initialized
    initKoin()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        ChatScreen()
    }
}
