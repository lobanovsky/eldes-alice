package com.eldes.alice.service

import com.eldes.alice.domain.GateTarget
import com.eldes.alice.model.AuthResponse
import com.eldes.alice.model.DevicesResponse
import com.eldes.alice.model.LoginRequest
import com.eldes.alice.model.OpenGateRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class EldesApiClient(
    private val httpClient: HttpClient,
    baseUrl: String,
    private val email: String,
    private val password: String,
) {
    private val apiBaseUrl = baseUrl.trimEnd('/')
    private val mutex = Mutex()
    private var auth: AuthResponse? = null
    private var devicesCache: CachedDevices? = null

    suspend fun open(target: GateTarget) {
        runCatching {
            openWithCurrentToken(target)
        }.recoverCatching { error ->
            if (error is ClientRequestException && error.response.status == HttpStatusCode.Unauthorized) {
                mutex.withLock {
                    auth = null
                    devicesCache = null
                }
                openWithCurrentToken(target)
            } else {
                throw error
            }
        }.getOrThrow()
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun openWithCurrentToken(target: GateTarget) {
        val authResponse = getAuth()
        val devices = getDevices(authResponse.token)
        val device = devices.zones
            .flatMap { it.devices }
            .firstOrNull { it.id == target.deviceId }
            ?: error("Устройство ${target.title} не найдено в доступных устройствах пользователя")

        httpClient.post("$apiBaseUrl/api/private/devices/${target.deviceId}/open") {
            bearerAuth(authResponse.token)
            setBody(
                OpenGateRequest(
                    key = device.deviceKey,
                    userid = devices.userId,
                )
            )
        }
    }

    private suspend fun getAuth(): AuthResponse = mutex.withLock {
        auth ?: httpClient.post("$apiBaseUrl/api/auth/login") {
            setBody(LoginRequest(email, password))
        }.body<AuthResponse>().also { auth = it }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun getDevices(token: String): DevicesResponse {
        val now = Clock.System.now()
        mutex.withLock {
            val cached = devicesCache
            if (cached != null && cached.expiresAt > now) return cached.devices
        }

        val fresh = httpClient.get("$apiBaseUrl/api/private/devices") {
            bearerAuth(token)
        }.body<DevicesResponse>()

        mutex.withLock {
            devicesCache = CachedDevices(fresh, now + 5.minutes)
        }
        return fresh
    }

    @OptIn(ExperimentalTime::class)
    private data class CachedDevices(
        val devices: DevicesResponse,
        val expiresAt: Instant,
    )
}
