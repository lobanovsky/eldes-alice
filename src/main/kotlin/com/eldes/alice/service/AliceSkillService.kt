package com.eldes.alice.service

import com.eldes.alice.domain.CommandParser
import com.eldes.alice.domain.GateTarget
import com.eldes.alice.domain.SkillCommand
import com.eldes.alice.model.AliceButton
import com.eldes.alice.model.AliceRequest
import com.eldes.alice.model.AliceResponse
import com.eldes.alice.model.AliceResponseData
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.slf4j.LoggerFactory

class AliceSkillService(
    private val parser: CommandParser,
    private val eldesApiClient: EldesApiClient,
) {
    private val logger = LoggerFactory.getLogger(AliceSkillService::class.java)

    suspend fun handle(request: AliceRequest): AliceResponse =
        when (val command = parser.parse(request.request.command, request.request.payload)) {
            is SkillCommand.Help -> helpResponse()
            is SkillCommand.Open -> openResponse(command.target)
        }

    private suspend fun openResponse(target: GateTarget): AliceResponse =
        runCatching {
            eldesApiClient.open(target)
        }.fold(
            onSuccess = {
                response("${target.title}: открываю.")
            },
            onFailure = { error ->
                logger.error("Failed to open ${target.key}", error)
                response("Не смог открыть ${target.title}. Проверьте доступ к API или попробуйте еще раз.")
            },
        )

    private fun helpResponse(): AliceResponse =
        response(
            text = "Скажите: сим-сим откройся, сим-сим в путь, сим-сим вниз или сим-сим вверх.",
            buttons = GateTarget.all.map { target ->
                AliceButton(
                    title = target.actionTitle,
                    payload = JsonObject(mapOf("target" to JsonPrimitive(target.key))),
                )
            },
        )

    private fun response(
        text: String,
        buttons: List<AliceButton> = emptyList(),
        endSession: Boolean = false,
    ): AliceResponse =
        AliceResponse(
            response = AliceResponseData(
                text = text,
                buttons = buttons,
                endSession = endSession,
            ),
        )
}
