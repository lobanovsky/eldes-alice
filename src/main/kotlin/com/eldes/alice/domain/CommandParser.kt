package com.eldes.alice.domain

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface SkillCommand {
    data class Open(val target: GateTarget) : SkillCommand
    data object Help : SkillCommand
}

class CommandParser {
    fun parse(command: String, payload: JsonObject? = null): SkillCommand {
        val targetFromPayload = payload
            ?.get("target")
            ?.jsonPrimitive
            ?.content
            ?.let(GateTarget::byKey)
        if (targetFromPayload != null) return SkillCommand.Open(targetFromPayload)

        val normalized = command.normalize()
        if (normalized.isBlank() || normalized in helpCommands) return SkillCommand.Help

        val target = commandAliases[normalized]
        return if (target == null) SkillCommand.Help else SkillCommand.Open(target)
    }

    private fun String.normalize(): String =
        lowercase()
            .replace('ё', 'е')
            .replace(Regex("[^а-яa-z0-9\\s-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private companion object {
        val helpCommands = setOf("помощь", "что ты умеешь", "что умеешь", "help")
        val commandAliases = mapOf(
            "сим-сим откройся" to GateTarget.byKey("yard_in"),
            "сим сим откройся" to GateTarget.byKey("yard_in"),
            "открой шлагбаум на въезд" to GateTarget.byKey("yard_in"),
            "открой шлагбаум на вьезд" to GateTarget.byKey("yard_in"),
            "сим-сим в путь" to GateTarget.byKey("yard_out"),
            "сим сим в путь" to GateTarget.byKey("yard_out"),
            "открой шлагбаум на выезд" to GateTarget.byKey("yard_out"),
            "сим-сим вниз" to GateTarget.byKey("parking_in"),
            "сим сим вниз" to GateTarget.byKey("parking_in"),
            "открой ворота паркинга на въезд" to GateTarget.byKey("parking_in"),
            "открой ворота паркинга на вьезд" to GateTarget.byKey("parking_in"),
            "сим-сим вверх" to GateTarget.byKey("parking_out"),
            "сим сим вверх" to GateTarget.byKey("parking_out"),
            "открой ворота паркинга на выезд" to GateTarget.byKey("parking_out"),
        )
    }
}
