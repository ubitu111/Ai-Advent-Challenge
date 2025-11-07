package ru.mirtomsk.shared.chat.repository.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Custom serializer for MessageContent
 * API always returns text as String, so we deserialize as String first
 * and then convert to MessageContent.Text (parsing to Json happens later)
 */
object MessageContentSerializer : KSerializer<AiMessage.MessageContent> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MessageContent", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AiMessage.MessageContent) {
        when (value) {
            is AiMessage.MessageContent.Text -> {
                encoder.encodeString(value.value)
            }
            is AiMessage.MessageContent.Json -> {
                // Serialize JSON as stringified JSON
                val jsonString = kotlinx.serialization.json.Json.encodeToString(JsonResponse.serializer(), value.value)
                encoder.encodeString(jsonString)
            }
        }
    }

    override fun deserialize(decoder: Decoder): AiMessage.MessageContent {
        val textValue = decoder.decodeString()
        return AiMessage.MessageContent.Text(textValue)
    }
}

