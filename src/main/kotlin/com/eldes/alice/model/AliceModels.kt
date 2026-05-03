package com.eldes.alice.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AliceRequest(
    val request: AliceRequestData,
    val session: AliceSession,
    val version: String = "1.0",
)

@Serializable
data class AliceRequestData(
    val type: String,
    val command: String = "",
    @SerialName("original_utterance")
    val originalUtterance: String = "",
    val payload: JsonObject? = null,
)

@Serializable
data class AliceSession(
    @SerialName("message_id")
    val messageId: Int = 0,
    @SerialName("session_id")
    val sessionId: String = "",
    @SerialName("skill_id")
    val skillId: String = "",
    val user: AliceUser? = null,
    val application: AliceApplication? = null,
    val new: Boolean = false,
)

@Serializable
data class AliceUser(
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("access_token")
    val accessToken: String? = null,
)

@Serializable
data class AliceApplication(
    @SerialName("application_id")
    val applicationId: String = "",
)

@Serializable
data class AliceResponse(
    val response: AliceResponseData,
    val version: String = "1.0",
)

@Serializable
data class AliceResponseData(
    val text: String,
    val tts: String = text,
    val buttons: List<AliceButton> = emptyList(),
    @SerialName("end_session")
    val endSession: Boolean = false,
)

@Serializable
data class AliceButton(
    val title: String,
    val payload: JsonObject? = null,
    val hide: Boolean = true,
)
