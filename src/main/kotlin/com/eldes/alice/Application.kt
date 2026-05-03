package com.eldes.alice

import com.eldes.alice.domain.CommandParser
import com.eldes.alice.model.AliceRequest
import com.eldes.alice.model.AliceResponse
import com.eldes.alice.model.AliceResponseData
import com.eldes.alice.service.AliceSkillService
import com.eldes.alice.service.EldesApiClient
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
import io.ktor.server.application.install
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun Application.module() {
    val appLog = environment.log
    val eldesConfig = EldesApiConfig.from(environment.config)
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

    val skillService = AliceSkillService(
        parser = CommandParser(),
        eldesApiClient = EldesApiClient(
            httpClient = httpClient,
            baseUrl = eldesConfig.baseUrl,
            email = eldesConfig.email,
            password = eldesConfig.password,
        ),
    )

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

private fun ApplicationConfig.requiredString(path: String): String =
    property(path).getString()

private fun ApplicationConfig.optionalLong(path: String, default: Long): Long =
    propertyOrNull(path)?.getString()?.toLongOrNull() ?: default
