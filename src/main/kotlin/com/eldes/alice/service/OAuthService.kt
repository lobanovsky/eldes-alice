package com.eldes.alice.service

import com.eldes.alice.model.AuthResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.SecureRandom
import java.util.Base64
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class OAuthService(
    private val eldesApiClient: EldesApiClient,
    private val codeTtlSeconds: Long,
) {
    private val mutex = Mutex()
    private val random = SecureRandom()
    private val codes = mutableMapOf<String, StoredCode>()

    @OptIn(ExperimentalTime::class)
    suspend fun authorize(email: String, password: String): String {
        val auth = eldesApiClient.login(email, password)
        val code = secureToken()
        mutex.withLock {
            codes[code] = StoredCode(auth, expiresAt())
        }
        return code
    }

    @OptIn(ExperimentalTime::class)
    suspend fun exchangeCode(code: String): AuthResponse? = mutex.withLock {
        val stored = codes.remove(code) ?: return@withLock null
        if (stored.expiresAt <= Clock.System.now()) return@withLock null
        stored.auth
    }

    private fun secureToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    @OptIn(ExperimentalTime::class)
    private fun expiresAt(): Instant =
        Clock.System.now() + codeTtlSeconds.seconds

    @OptIn(ExperimentalTime::class)
    private data class StoredCode(
        val auth: AuthResponse,
        val expiresAt: Instant,
    )
}
