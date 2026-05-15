package com.eldes.alice.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SmartHomeDevicesResponse(
    @SerialName("request_id")
    val requestId: String,
    val payload: SmartHomeDevicesPayload,
)

@Serializable
data class SmartHomeDevicesPayload(
    @SerialName("user_id")
    val userId: String,
    val devices: List<SmartHomeDevice>,
)

@Serializable
data class SmartHomeDevice(
    val id: String,
    val name: String,
    val description: String,
    val room: String,
    val type: String = "devices.types.openable",
    val capabilities: List<SmartHomeCapabilityDescription>,
    val properties: List<JsonObject> = emptyList(),
    @SerialName("custom_data")
    val customData: JsonObject? = null,
)

@Serializable
data class SmartHomeCapabilityDescription(
    val type: String = "devices.capabilities.on_off",
    val retrievable: Boolean = true,
    val parameters: SmartHomeOnOffParameters = SmartHomeOnOffParameters(),
)

@Serializable
data class SmartHomeOnOffParameters(
    val split: Boolean = false,
)

@Serializable
data class SmartHomeDevicesRequest(
    val devices: List<SmartHomeRequestedDevice> = emptyList(),
)

@Serializable
data class SmartHomeActionRequest(
    val payload: SmartHomeActionPayload,
)

@Serializable
data class SmartHomeActionPayload(
    val devices: List<SmartHomeActionDevice> = emptyList(),
)

@Serializable
data class SmartHomeRequestedDevice(
    val id: String,
    @SerialName("custom_data")
    val customData: JsonObject? = null,
)

@Serializable
data class SmartHomeActionDevice(
    val id: String,
    val capabilities: List<SmartHomeCapabilityState> = emptyList(),
    @SerialName("custom_data")
    val customData: JsonObject? = null,
)

@Serializable
data class SmartHomeCapabilityState(
    val type: String,
    val state: SmartHomeState,
)

@Serializable
data class SmartHomeState(
    val instance: String,
    val value: Boolean? = null,
)

@Serializable
data class SmartHomeQueryResponse(
    @SerialName("request_id")
    val requestId: String,
    val payload: SmartHomeQueryPayload,
)

@Serializable
data class SmartHomeQueryPayload(
    val devices: List<SmartHomeQueryDevice>,
)

@Serializable
data class SmartHomeQueryDevice(
    val id: String,
    val capabilities: List<SmartHomeCapabilityCurrentState> = emptyList(),
    @SerialName("error_code")
    val errorCode: String? = null,
    @SerialName("error_message")
    val errorMessage: String? = null,
)

@Serializable
data class SmartHomeCapabilityCurrentState(
    val type: String = "devices.capabilities.on_off",
    val state: SmartHomeCurrentState,
)

@Serializable
data class SmartHomeCurrentState(
    val instance: String = "on",
    val value: Boolean = false,
)

@Serializable
data class SmartHomeActionResponse(
    @SerialName("request_id")
    val requestId: String,
    val payload: SmartHomeActionResponsePayload,
)

@Serializable
data class SmartHomeActionResponsePayload(
    val devices: List<SmartHomeActionResultDevice>,
)

@Serializable
data class SmartHomeActionResultDevice(
    val id: String,
    val capabilities: List<SmartHomeCapabilityActionResult> = emptyList(),
    @SerialName("action_result")
    val actionResult: SmartHomeActionResult? = null,
)

@Serializable
data class SmartHomeCapabilityActionResult(
    val type: String = "devices.capabilities.on_off",
    val state: SmartHomeActionResultState,
)

@Serializable
data class SmartHomeActionResultState(
    val instance: String = "on",
    @SerialName("action_result")
    val actionResult: SmartHomeActionResult,
)

@Serializable
data class SmartHomeActionResult(
    val status: String,
    @SerialName("error_code")
    val errorCode: String? = null,
    @SerialName("error_message")
    val errorMessage: String? = null,
)
