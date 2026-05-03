package com.eldes.alice.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: ApiUser,
)

@Serializable
data class ApiUser(
    val id: Int,
    val email: String,
    val phoneNumber: String,
    val isActive: Boolean,
)

@Serializable
data class DevicesResponse(
    val userId: String,
    val zones: List<Zone> = emptyList(),
)

@Serializable
data class Zone(
    val id: Long,
    val name: String,
    val devices: List<Device> = emptyList(),
)

@Serializable
data class Device(
    val id: String,
    val name: String,
    val label: String,
    val phoneNumber: String = "",
    val deviceKey: String,
)

@Serializable
data class OpenGateRequest(
    val key: String,
    val update: String = "device",
    val app: String = "webappAPI",
    val userid: String,
    val method: String = "PROGRESSIVE_WEB_APPS",
)
