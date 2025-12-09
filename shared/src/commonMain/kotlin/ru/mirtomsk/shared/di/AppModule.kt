package ru.mirtomsk.shared.di

import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.mirtomsk.shared.chat.ChatViewModel
import ru.mirtomsk.shared.chat.agent.BuildAgent
import ru.mirtomsk.shared.chat.agent.CodeReviewAgent
import ru.mirtomsk.shared.chat.agent.DeveloperAgent
import ru.mirtomsk.shared.chat.agent.DeveloperHelperAgent
import ru.mirtomsk.shared.chat.agent.SimpleChatAgent
import ru.mirtomsk.shared.chat.agent.SupportAgent
import ru.mirtomsk.shared.chat.context.ContextResetProvider
import ru.mirtomsk.shared.chat.repository.ChatRepository
import ru.mirtomsk.shared.chat.repository.ChatRepositoryImpl
import ru.mirtomsk.shared.chat.repository.cache.ChatCache
import ru.mirtomsk.shared.chat.repository.cache.FileChatCache
import ru.mirtomsk.shared.chat.repository.mapper.AiResponseMapper
import ru.mirtomsk.shared.chat.repository.mapper.OpenAiResponseMapper
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.config.ApiConfigImpl
import ru.mirtomsk.shared.config.ApiConfigReader
import ru.mirtomsk.shared.coroutines.DispatchersProvider
import ru.mirtomsk.shared.coroutines.DispatchersProviderImpl
import ru.mirtomsk.shared.dollarRate.DollarRateRepository
import ru.mirtomsk.shared.dollarRate.DollarRateScheduler
import ru.mirtomsk.shared.dollarRate.DollarRateViewModel
import ru.mirtomsk.shared.embeddings.EmbeddingsNormalizer
import ru.mirtomsk.shared.embeddings.EmbeddingsViewModel
import ru.mirtomsk.shared.embeddings.FilePicker
import ru.mirtomsk.shared.embeddings.cache.EmbeddingsCache
import ru.mirtomsk.shared.embeddings.cache.FileEmbeddingsCache
import ru.mirtomsk.shared.embeddings.createFilePicker
import ru.mirtomsk.shared.embeddings.repository.EmbeddingsRepository
import ru.mirtomsk.shared.embeddings.repository.EmbeddingsRepositoryImpl
import ru.mirtomsk.shared.network.ChatApiService
import ru.mirtomsk.shared.network.ILocalChatApiService
import ru.mirtomsk.shared.network.LocalChatApiService
import ru.mirtomsk.shared.network.createLocalChatApiService
import ru.mirtomsk.shared.network.NetworkModule
import ru.mirtomsk.shared.network.compression.ContextCompressionProvider
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.network.mcp.McpApiService
import ru.mirtomsk.shared.network.mcp.McpOrchestrator
import ru.mirtomsk.shared.network.mcp.McpRepository
import ru.mirtomsk.shared.network.mcp.McpRepositoryImpl
import ru.mirtomsk.shared.network.mcp.McpService
import ru.mirtomsk.shared.network.mcp.McpToolsProvider
import ru.mirtomsk.shared.network.rag.OllamaApiService
import ru.mirtomsk.shared.network.rag.RagReranker
import ru.mirtomsk.shared.network.rag.RagRerankingProvider
import ru.mirtomsk.shared.network.rag.RagService
import ru.mirtomsk.shared.network.temperature.TemperatureProvider
import ru.mirtomsk.shared.network.tokens.MaxTokensProvider
import ru.mirtomsk.shared.settings.SettingsViewModel

/**
 * Configuration module for API keys
 */
val configModule = module {
    single<ApiConfig> {
        ApiConfigImpl(
            apiKey = ApiConfigReader.readApiKey(),
            keyId = ApiConfigReader.readKeyId(),
            mcpgateToken = ApiConfigReader.readMcpgateToken(),
            useLocalModel = ApiConfigReader.readUseLocalModel(),
            localModelBaseUrl = ApiConfigReader.readLocalModelBaseUrl(),
            localModelName = ApiConfigReader.readLocalModelName(),
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
            json = get<Json>(),
        )
    }

    // Local LLM service (platform-specific: on-device for Android, HTTP for desktop)
    // On Android: uses on-device llama.cpp model via JNI (if available)
    // On Desktop: uses HTTP-based Ollama/LM Studio API
    single<ILocalChatApiService> {
        val apiConfig: ApiConfig = get()
        
        if (apiConfig.useLocalModel) {
            // Try to create platform-specific service
            // Android will use on-device model, Desktop will use HTTP
            createLocalChatApiService(
                json = get(),
                baseUrl = apiConfig.localModelBaseUrl,
                modelName = apiConfig.localModelName,
            )
        } else {
            // Use HTTP-based service (Ollama) - works for both platforms
            LocalChatApiService(
                httpClient = get(),
                json = get(),
                baseUrl = apiConfig.localModelBaseUrl,
                modelName = apiConfig.localModelName,
            )
        }
    }

    // Create MCP orchestrator with list of services
    single {
        val apiConfig: ApiConfig = get()
        val token = apiConfig.mcpgateToken
        val services: List<McpService> = listOf(
            McpApiService(
                httpClient = get(),
                json = get(),
                baseUrl = "http://localhost:8080/mcp",
                serviceId = "mcp-localhost-8080",
            ),
            McpApiService(
                httpClient = get(),
                json = get(),
                baseUrl = "https://gateway.mcpgate.ru/open_meteo/mcp?apikey=$token",
                serviceId = "gateway-mcpgate",
            )
        )
        McpOrchestrator(services)
    }

    single {
        OllamaApiService(
            httpClient = get(),
            baseUrl = "http://127.0.0.1:11434",
        )
    }
}

/**
 * Repository module for Koin dependency injection
 */
val repositoryModule = module {
    single<Json> {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    // Кеши для каждого агента
    single<ChatCache>(named("simpleChatCache")) {
        FileChatCache(
            cacheFileName = "simple_chat_cache.json",
            json = get()
        )
    }

    single<ChatCache>(named("codeReviewCache")) {
        FileChatCache(
            cacheFileName = "code_review_cache.json",
            json = get()
        )
    }

    single<ChatCache>(named("developerHelperCache")) {
        FileChatCache(
            cacheFileName = "developer_helper_cache.json",
            json = get()
        )
    }

    single<ChatCache>(named("supportCache")) {
        FileChatCache(
            cacheFileName = "support_cache.json",
            json = get()
        )
    }

    single<ChatCache>(named("developerCache")) {
        FileChatCache(
            cacheFileName = "developer_cache.json",
            json = get()
        )
    }

    single<ChatCache>(named("buildCache")) {
        FileChatCache(
            cacheFileName = "build_cache.json",
            json = get()
        )
    }

    single {
        AiResponseMapper(
            json = get()
        )
    }

    single {
        OpenAiResponseMapper(
            json = get()
        )
    }

    // Создание агентов
    single {
        SimpleChatAgent(
            chatApiService = get(),
            apiConfig = get(),
            ioDispatcher = get<DispatchersProvider>().io,
            yandexResponseMapper = get<AiResponseMapper>(),
            formatProvider = get<ResponseFormatProvider>(),
            temperatureProvider = get<TemperatureProvider>(),
            maxTokensProvider = get<MaxTokensProvider>(),
            chatCache = get(named("simpleChatCache")),
            mcpToolsProvider = get<McpToolsProvider>(),
            mcpOrchestrator = get<McpOrchestrator>(),
            json = get<Json>(),
            localChatApiService = get<ILocalChatApiService>(),
            openAiResponseMapper = get<OpenAiResponseMapper>(),
        )
    }

    single {
        CodeReviewAgent(
            chatApiService = get(),
            apiConfig = get(),
            ioDispatcher = get<DispatchersProvider>().io,
            yandexResponseMapper = get<AiResponseMapper>(),
            formatProvider = get<ResponseFormatProvider>(),
            temperatureProvider = get<TemperatureProvider>(),
            maxTokensProvider = get<MaxTokensProvider>(),
            chatCache = get(named("codeReviewCache")),
            mcpToolsProvider = get<McpToolsProvider>(),
            mcpOrchestrator = get<McpOrchestrator>(),
            ragService = get<RagService>(),
            json = get<Json>(),
            localChatApiService = get<ILocalChatApiService>(),
            openAiResponseMapper = get<OpenAiResponseMapper>(),
        )
    }

    single {
        DeveloperHelperAgent(
            chatApiService = get(),
            apiConfig = get(),
            ioDispatcher = get<DispatchersProvider>().io,
            yandexResponseMapper = get<AiResponseMapper>(),
            formatProvider = get<ResponseFormatProvider>(),
            temperatureProvider = get<TemperatureProvider>(),
            maxTokensProvider = get<MaxTokensProvider>(),
            chatCache = get(named("developerHelperCache")),
            mcpToolsProvider = get<McpToolsProvider>(),
            mcpOrchestrator = get<McpOrchestrator>(),
            ragService = get<RagService>(),
            json = get<Json>(),
            localChatApiService = get<ILocalChatApiService>(),
            openAiResponseMapper = get<OpenAiResponseMapper>(),
        )
    }

    single {
        SupportAgent(
            chatApiService = get(),
            apiConfig = get(),
            ioDispatcher = get<DispatchersProvider>().io,
            yandexResponseMapper = get<AiResponseMapper>(),
            formatProvider = get<ResponseFormatProvider>(),
            temperatureProvider = get<TemperatureProvider>(),
            maxTokensProvider = get<MaxTokensProvider>(),
            chatCache = get(named("supportCache")),
            mcpToolsProvider = get<McpToolsProvider>(),
            mcpOrchestrator = get<McpOrchestrator>(),
            ragService = get<RagService>(),
            json = get<Json>(),
            localChatApiService = get<ILocalChatApiService>(),
            openAiResponseMapper = get<OpenAiResponseMapper>(),
        )
    }

    single {
        DeveloperAgent(
            chatApiService = get(),
            apiConfig = get(),
            ioDispatcher = get<DispatchersProvider>().io,
            yandexResponseMapper = get<AiResponseMapper>(),
            formatProvider = get<ResponseFormatProvider>(),
            temperatureProvider = get<TemperatureProvider>(),
            maxTokensProvider = get<MaxTokensProvider>(),
            chatCache = get(named("developerCache")),
            mcpToolsProvider = get<McpToolsProvider>(),
            mcpOrchestrator = get<McpOrchestrator>(),
            ragService = get<RagService>(),
            json = get<Json>(),
            localChatApiService = get<ILocalChatApiService>(),
            openAiResponseMapper = get<OpenAiResponseMapper>(),
        )
    }

    single {
        BuildAgent(
            chatApiService = get(),
            apiConfig = get(),
            ioDispatcher = get<DispatchersProvider>().io,
            yandexResponseMapper = get<AiResponseMapper>(),
            formatProvider = get<ResponseFormatProvider>(),
            temperatureProvider = get<TemperatureProvider>(),
            maxTokensProvider = get<MaxTokensProvider>(),
            chatCache = get(named("buildCache")),
            mcpToolsProvider = get<McpToolsProvider>(),
            mcpOrchestrator = get<McpOrchestrator>(),
            json = get<Json>(),
            localChatApiService = get<ILocalChatApiService>(),
            openAiResponseMapper = get<OpenAiResponseMapper>(),
        )
    }

    // Репозиторий как оркестратор агентов
    single {
        ChatRepositoryImpl(
            ioDispatcher = get<DispatchersProvider>().io,
            contextResetProvider = get<ContextResetProvider>(),
            simpleChatAgent = get<SimpleChatAgent>(),
            codeReviewAgent = get<CodeReviewAgent>(),
            developerHelperAgent = get<DeveloperHelperAgent>(),
            supportAgent = get<SupportAgent>(),
            developerAgent = get<DeveloperAgent>(),
            buildAgent = get<BuildAgent>(),
        )
    }.bind<ChatRepository>()

    single {
        McpRepositoryImpl(
            mcpOrchestrator = get<McpOrchestrator>(),
            ioDispatcher = get<DispatchersProvider>().io,
        )
    }.bind<McpRepository>()

    single {
        DollarRateRepository(
            chatApiService = get(),
            apiConfig = get(),
            ioDispatcher = get<DispatchersProvider>().io,
            yandexResponseMapper = get<AiResponseMapper>(),
            formatProvider = get<ResponseFormatProvider>(),
            temperatureProvider = get<TemperatureProvider>(),
            maxTokensProvider = get<MaxTokensProvider>(),
            mcpToolsProvider = get<McpToolsProvider>(),
            mcpOrchestrator = get<McpOrchestrator>(),
            json = get<Json>(),
        )
    }

    single {
        EmbeddingsRepositoryImpl(
            ollamaApiService = get<OllamaApiService>(),
            embeddingsNormalizer = get<EmbeddingsNormalizer>(),
            ioDispatcher = get<DispatchersProvider>().io,
        )
    }.bind<EmbeddingsRepository>()

    single<EmbeddingsCache> {
        FileEmbeddingsCache(
            json = get()
        )
    }

    single<FilePicker> {
        createFilePicker()
    }

    single {
        RagReranker(
            httpClient = get(),
            baseUrl = "http://127.0.0.1:11434",
            ioDispatcher = get<DispatchersProvider>().io,
        )
    }

    single {
        RagService(
            ollamaApiService = get<OllamaApiService>(),
            embeddingsCache = get<EmbeddingsCache>(),
            embeddingsNormalizer = get<EmbeddingsNormalizer>(),
            ragReranker = get<RagReranker>(),
            ragRerankingProvider = get<RagRerankingProvider>(),
            ioDispatcher = get<DispatchersProvider>().io,
        )
    }

    single {
        EmbeddingsNormalizer()
    }
}

/**
 * Settings module for Koin dependency injection
 */
val settingsModule = module {
    single { ResponseFormatProvider() }
    single { ContextResetProvider() }
    single { TemperatureProvider() }
    single { MaxTokensProvider() }
    single { ContextCompressionProvider() }
    single { RagRerankingProvider() }
    single { McpToolsProvider() }
}

/**
 * ViewModel module for Koin dependency injection
 */
val viewModelModule = module {
    factory {
        ChatViewModel(
            repository = get<ChatRepository>(),
            mcpRepository = get<McpRepository>(),
            mcpToolsProvider = get<McpToolsProvider>(),
            dollarRateScheduler = get<DollarRateScheduler>(),
            mainDispatcher = get<DispatchersProvider>().main,
        )
    }

    factory {
        SettingsViewModel(
            formatProvider = get<ResponseFormatProvider>(),
            contextResetProvider = get<ContextResetProvider>(),
            temperatureProvider = get<TemperatureProvider>(),
            maxTokensProvider = get<MaxTokensProvider>(),
            contextCompressionProvider = get<ContextCompressionProvider>(),
            mainDispatcher = get<DispatchersProvider>().main,
        )
    }

    single {
        DollarRateViewModel()
    }

    single {
        DollarRateScheduler(
            repository = get<DollarRateRepository>(),
            viewModel = get<DollarRateViewModel>(),
            ioDispatcher = get<DispatchersProvider>().io,
        )
    }

    factory {
        EmbeddingsViewModel(
            repository = get<EmbeddingsRepository>(),
            cache = get<EmbeddingsCache>(),
            filePicker = get<FilePicker>(),
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

