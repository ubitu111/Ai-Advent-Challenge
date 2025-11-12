package ru.mirtomsk.shared.di

import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.mirtomsk.shared.chat.ChatViewModel
import ru.mirtomsk.shared.chat.repository.ChatRepository
import ru.mirtomsk.shared.chat.repository.ChatRepositoryImpl
import ru.mirtomsk.shared.chat.repository.mapper.AiResponseMapper
import ru.mirtomsk.shared.chat.repository.mapper.HuggingFaceResponseMapper
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.config.ApiConfigImpl
import ru.mirtomsk.shared.config.ApiConfigReader
import ru.mirtomsk.shared.coroutines.DispatchersProvider
import ru.mirtomsk.shared.coroutines.DispatchersProviderImpl
import ru.mirtomsk.shared.network.ChatApiService
import ru.mirtomsk.shared.network.NetworkModule
import ru.mirtomsk.shared.chat.context.ContextResetProvider
import ru.mirtomsk.shared.network.agent.AgentTypeProvider
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.network.prompt.SystemPromptProvider
import ru.mirtomsk.shared.network.temperature.TemperatureProvider
import ru.mirtomsk.shared.settings.SettingsViewModel
import ru.mirtomsk.shared.network.huggingface.HuggingFaceApiService
import ru.mirtomsk.shared.chat.repository.HuggingFaceChatRepository

/**
 * Configuration module for API keys
 */
val configModule = module {
    single<ApiConfig> {
        ApiConfigImpl(
            apiKey = ApiConfigReader.readApiKey(),
            keyId = ApiConfigReader.readKeyId(),
            huggingFaceToken = ApiConfigReader.readHuggingFaceToken()
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

    single {
        HuggingFaceApiService(
            httpClient = get(),
            apiConfig = get(),
        )
    }
}

/**
 * Repository module for Koin dependency injection
 */
val repositoryModule = module {
    single<Json> {
        Json { ignoreUnknownKeys = true }
    }

    single {
        AiResponseMapper(
            json = get()
        )
    }

    single {
        HuggingFaceResponseMapper(
            json = get()
        )
    }

    single {
        ChatRepositoryImpl(
            chatApiService = get(),
            apiConfig = get(),
            ioDispatcher = get<DispatchersProvider>().io,
            responseMapper = get(),
            formatProvider = get<ResponseFormatProvider>(),
            agentTypeProvider = get<AgentTypeProvider>(),
            systemPromptProvider = get<SystemPromptProvider>(),
            contextResetProvider = get<ContextResetProvider>(),
            temperatureProvider = get<TemperatureProvider>(),
        )
    }.bind<ChatRepository>()

    single {
        HuggingFaceChatRepository(
            huggingFaceApiService = get(),
            responseMapper = get<HuggingFaceResponseMapper>(),
            ioDispatcher = get<DispatchersProvider>().io,
            agentTypeProvider = get<AgentTypeProvider>(),
            contextResetProvider = get<ContextResetProvider>(),
            temperatureProvider = get<TemperatureProvider>(),
        )
    }
}

/**
 * Settings module for Koin dependency injection
 */
val settingsModule = module {
    single { ResponseFormatProvider() }
    single { AgentTypeProvider() }
    single { SystemPromptProvider() }
    single { ContextResetProvider() }
    single { TemperatureProvider() }
}

/**
 * ViewModel module for Koin dependency injection
 */
val viewModelModule = module {
    factory {
        ChatViewModel(
            repository = get<ChatRepository>(),
            mainDispatcher = get<DispatchersProvider>().main,
        )
    }

    factory {
        SettingsViewModel(
            formatProvider = get<ResponseFormatProvider>(),
            agentTypeProvider = get<AgentTypeProvider>(),
            systemPromptProvider = get<SystemPromptProvider>(),
            contextResetProvider = get<ContextResetProvider>(),
            temperatureProvider = get<TemperatureProvider>(),
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

