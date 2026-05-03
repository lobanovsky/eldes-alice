package com.eldes.alice

import com.eldes.alice.domain.CommandParser
import com.eldes.alice.model.AliceRequest
import com.eldes.alice.model.AliceResponse
import com.eldes.alice.model.AliceResponseData
import com.eldes.alice.service.AliceSkillService
import com.eldes.alice.service.EldesApiClient
import com.eldes.alice.service.OAuthService
import com.eldes.alice.service.SmartHomeService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

fun Application.module() {
    val appLog = environment.log
    val eldesConfig = EldesApiConfig.from(environment.config)
    val smartHomeConfig = SmartHomeConfig.from(environment.config)
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    install(ServerContentNegotiation) {
        json(json)
    }
    install(CallLogging) {
        level = Level.INFO
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            appLog.error("Unhandled request error", cause)
            call.respond(HttpStatusCode.OK, aliceErrorResponse())
        }
    }

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        install(HttpTimeout) {
            requestTimeoutMillis = eldesConfig.timeoutMs
            connectTimeoutMillis = eldesConfig.connectTimeoutMs
            socketTimeoutMillis = eldesConfig.socketTimeoutMs
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    val eldesApiClient = EldesApiClient(
        httpClient = httpClient,
        baseUrl = eldesConfig.baseUrl,
        email = eldesConfig.email,
        password = eldesConfig.password,
    )
    val skillService = AliceSkillService(
        parser = CommandParser(),
        eldesApiClient = eldesApiClient,
    )
    val oauthService = OAuthService(eldesApiClient, smartHomeConfig.authCodeTtlSeconds)
    val smartHomeService = SmartHomeService(eldesApiClient)

    routing {
        get("/ping") {
            call.respondText("pong")
        }
        post("/") {
            call.respond(skillService.handle(call.receive<AliceRequest>()))
        }
        post("/alice") {
            call.respond(skillService.handle(call.receive<AliceRequest>()))
        }

        get("/oauth/authorize") {
            call.respondText(authorizeForm(call), ContentType.Text.Html)
        }
        post("/oauth/authorize") {
            val params = call.receiveParameters()
            val email = params["email"].orEmpty()
            val password = params["password"].orEmpty()
            val redirectUri = params["redirect_uri"].orEmpty()
            val state = params["state"].orEmpty()
            val clientId = params["client_id"].orEmpty()

            if (!smartHomeConfig.isClientAllowed(clientId)) {
                call.respond(HttpStatusCode.BadRequest, "Invalid client_id")
                return@post
            }
            if (redirectUri.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing redirect_uri")
                return@post
            }

            runCatching {
                oauthService.authorize(email, password)
            }.fold(
                onSuccess = { code ->
                    val redirect = buildString {
                        append(redirectUri)
                        append(if (redirectUri.contains("?")) "&" else "?")
                        append("code=")
                        append(code.urlEncode())
                        if (state.isNotBlank()) {
                            append("&state=")
                            append(state.urlEncode())
                        }
                    }
                    call.respondRedirect(redirect)
                },
                onFailure = {
                    call.respondText(authorizeForm(call, "Неверный email или пароль"), ContentType.Text.Html)
                },
            )
        }
        post("/oauth/token") {
            val params = call.receiveParameters()
            val basicClient = call.basicClientCredentials()
            val clientId = params["client_id"] ?: basicClient?.first.orEmpty()
            val clientSecret = params["client_secret"] ?: basicClient?.second.orEmpty()
            val code = params["code"].orEmpty()
            val grantType = params["grant_type"].orEmpty()

            if (!smartHomeConfig.isClientAllowed(clientId) || !smartHomeConfig.isSecretAllowed(clientSecret)) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid_client"))
                return@post
            }
            if (grantType != "authorization_code") {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "unsupported_grant_type"))
                return@post
            }

            val auth = oauthService.exchangeCode(code)
            if (auth == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_grant"))
                return@post
            }

            call.respond(
                mapOf(
                    "access_token" to auth.token,
                    "token_type" to "Bearer",
                    "expires_in" to 31_536_000,
                )
            )
        }

        head("/v1.0/") {
            call.respond(HttpStatusCode.OK)
        }
        get("/v1.0/user/devices") {
            val token = call.bearerToken()
            if (token == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }
            call.respond(smartHomeService.devices(call.requestId(), token))
        }
        post("/v1.0/user/devices/query") {
            val token = call.bearerToken()
            if (token == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            call.respond(smartHomeService.query(call.requestId(), call.receive()))
        }
        post("/v1.0/user/devices/action") {
            val token = call.bearerToken()
            if (token == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            call.respond(smartHomeService.action(call.requestId(), token, call.receive()))
        }
        post("/v1.0/user/unlink") {
            call.respond(HttpStatusCode.OK)
        }
    }
}

private fun aliceErrorResponse(): AliceResponse =
    AliceResponse(
        response = AliceResponseData(
            text = "Не смог выполнить команду. Попробуйте еще раз.",
            endSession = false,
        ),
    )

private data class EldesApiConfig(
    val baseUrl: String,
    val email: String,
    val password: String,
    val timeoutMs: Long,
    val connectTimeoutMs: Long,
    val socketTimeoutMs: Long,
) {
    companion object {
        fun from(config: ApplicationConfig): EldesApiConfig =
            EldesApiConfig(
                baseUrl = config.requiredString("eldesApi.baseUrl"),
                email = config.requiredString("eldesApi.email"),
                password = config.requiredString("eldesApi.password"),
                timeoutMs = config.optionalLong("eldesApi.timeoutMs", 4_000),
                connectTimeoutMs = config.optionalLong("eldesApi.connectTimeoutMs", 1_500),
                socketTimeoutMs = config.optionalLong("eldesApi.socketTimeoutMs", 4_000),
            )
    }
}

private data class SmartHomeConfig(
    val oauthClientId: String,
    val oauthClientSecret: String,
    val authCodeTtlSeconds: Long,
) {
    fun isClientAllowed(clientId: String): Boolean =
        oauthClientId.isBlank() || clientId == oauthClientId

    fun isSecretAllowed(clientSecret: String): Boolean =
        oauthClientSecret.isBlank() || clientSecret == oauthClientSecret

    companion object {
        fun from(config: ApplicationConfig): SmartHomeConfig =
            SmartHomeConfig(
                oauthClientId = config.propertyOrNull("smartHome.oauthClientId")?.getString().orEmpty(),
                oauthClientSecret = config.propertyOrNull("smartHome.oauthClientSecret")?.getString().orEmpty(),
                authCodeTtlSeconds = config.optionalLong("smartHome.authCodeTtlSeconds", 300),
            )
    }
}

private fun ApplicationConfig.requiredString(path: String): String =
    property(path).getString()

private fun ApplicationConfig.optionalLong(path: String, default: Long): Long =
    propertyOrNull(path)?.getString()?.toLongOrNull() ?: default

private fun ApplicationCall.bearerToken(): String? =
    request.headers["Authorization"]
        ?.removePrefix("Bearer ")
        ?.takeIf { it.isNotBlank() }

private fun ApplicationCall.requestId(): String =
    request.headers["X-Request-Id"] ?: UUID.randomUUID().toString()

private fun ApplicationCall.basicClientCredentials(): Pair<String, String>? {
    val value = request.headers["Authorization"] ?: return null
    if (!value.startsWith("Basic ")) return null
    val decoded = runCatching {
        String(Base64.getDecoder().decode(value.removePrefix("Basic ")), StandardCharsets.UTF_8)
    }.getOrNull() ?: return null
    val separatorIndex = decoded.indexOf(':')
    if (separatorIndex < 0) return null
    return decoded.substring(0, separatorIndex) to decoded.substring(separatorIndex + 1)
}

private fun authorizeForm(call: ApplicationCall, error: String? = null): String {
    val query = call.request.queryParameters
    fun field(name: String): String = query[name].orEmpty().htmlEscape()
    val errorHtml = error?.let { "<p style=\"color:#b00020\">${it.htmlEscape()}</p>" }.orEmpty()
    return """
        <!doctype html>
        <html lang="ru">
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Вход Сим-Сим</title>
            <style>
                body { font-family: system-ui, sans-serif; max-width: 420px; margin: 40px auto; padding: 0 16px; }
                label { display: block; margin: 14px 0 6px; }
                input, button { box-sizing: border-box; width: 100%; font-size: 16px; padding: 10px; }
                button { margin-top: 18px; cursor: pointer; }
            </style>
        </head>
        <body>
            <h1>Сим-Сим</h1>
            <p>Войдите с учетной записью доступа к шлагбаумам.</p>
            $errorHtml
            <form method="post" action="/oauth/authorize">
                <input type="hidden" name="client_id" value="${field("client_id")}">
                <input type="hidden" name="redirect_uri" value="${field("redirect_uri")}">
                <input type="hidden" name="state" value="${field("state")}">
                <label for="email">Email</label>
                <input id="email" name="email" type="email" autocomplete="username" required>
                <label for="password">Пароль</label>
                <input id="password" name="password" type="password" autocomplete="current-password" required>
                <button type="submit">Войти</button>
            </form>
        </body>
        </html>
    """.trimIndent()
}

private fun String.urlEncode(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8)

private fun String.htmlEscape(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
