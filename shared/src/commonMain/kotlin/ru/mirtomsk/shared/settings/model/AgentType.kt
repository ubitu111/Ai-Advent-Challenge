package ru.mirtomsk.shared.settings.model

enum class AgentType {
    // Yandex GPT models
    LITE,
    PRO,
    
    // HuggingFace models - Top level
    MISTRAL_7B_INSTRUCT,
    OPENAI_OSS_120B,
    
    // HuggingFace models - Middle level
    GPT_J_6B,
    DIALOGPT_MEDIUM,
    
    // HuggingFace models - Basic level
    TINYLLAMA_1_1B,
    GPT2,
    DISTILGPT2,

    SAO10,
    QWEN05B,
    QWEN7B,
}
