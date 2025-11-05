package ru.mirtomsk.shared.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.mirtomsk.shared.chat.ChatViewModel
import ru.mirtomsk.shared.chat.repository.ChatRepository
import ru.mirtomsk.shared.chat.repository.ChatRepositoryImpl
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.config.ApiConfigImpl
import ru.mirtomsk.shared.config.ApiConfigReader
import ru.mirtomsk.shared.coroutines.DispatchersProvider
import ru.mirtomsk.shared.coroutines.DispatchersProviderImpl
import ru.mirtomsk.shared.network.ChatApiService
import ru.mirtomsk.shared.network.NetworkModule
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.settings.SettingsViewModel

/**
 * Configuration module for API keys
 */
val configModule = module {
    single<ApiConfig> {
        ApiConfigImpl(
            apiKey = ApiConfigReader.readApiKey(),
            keyId = ApiConfigReader.readKeyId()
        )
    }
}

/**
 * Network module for Koin dependency injection
 */
val networkModule = module {
    single { NetworkModule.createHttpClient(enableLogging = true) }

    single {
        ChatApiService(
            httpClient = get(),
            apiConfig = get(),
        )
    }
}

/**
 * Repository module for Koin dependency injection
 */
val repositoryModule = module {
    single {
        ChatRepositoryImpl(
            chatApiService = get(),
            ioDispatcher = get<DispatchersProvider>().io,
        )
    }.bind<ChatRepository>()
}

/**
 * Settings module for Koin dependency injection
 */
val settingsModule = module {
    single { ResponseFormatProvider() }
}

/**
 * ViewModel module for Koin dependency injection
 */
val viewModelModule = module {
    factory {
        ChatViewModel(
            repository = get<ChatRepository>(),
            formatProvider = get<ResponseFormatProvider>(),
            mainDispatcher = get<DispatchersProvider>().main,
        )
    }
    
    factory {
        SettingsViewModel(
            formatProvider = get<ResponseFormatProvider>(),
            mainDispatcher = get<DispatchersProvider>().main,
        )
    }
}

/**
 * App module combining all modules
 */
val appModule = module {
    singleOf(::DispatchersProviderImpl) {
        bind<DispatchersProvider>()
    }
    includes(
        configModule,
        networkModule,
        repositoryModule,
        settingsModule,
        viewModelModule,
    )
}

