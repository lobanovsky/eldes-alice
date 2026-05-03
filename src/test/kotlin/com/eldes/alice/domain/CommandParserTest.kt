package com.eldes.alice.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CommandParserTest {
    private val parser = CommandParser()

    @Test
    fun parsesYardIn() {
        val command = parser.parse("сим-сим откройся")

        val open = assertIs<SkillCommand.Open>(command)
        assertEquals("yard_in", open.target.key)
    }

    @Test
    fun parsesYardInDirectCommand() {
        val command = parser.parse("открой шлагбаум на въезд")

        val open = assertIs<SkillCommand.Open>(command)
        assertEquals("yard_in", open.target.key)
    }

    @Test
    fun parsesYardOut() {
        val command = parser.parse("сим-сим в путь")

        val open = assertIs<SkillCommand.Open>(command)
        assertEquals("yard_out", open.target.key)
    }

    @Test
    fun parsesYardOutDirectCommand() {
        val command = parser.parse("открой шлагбаум на выезд")

        val open = assertIs<SkillCommand.Open>(command)
        assertEquals("yard_out", open.target.key)
    }

    @Test
    fun parsesParkingIn() {
        val command = parser.parse("сим-сим вниз")

        val open = assertIs<SkillCommand.Open>(command)
        assertEquals("parking_in", open.target.key)
    }

    @Test
    fun parsesParkingInDirectCommand() {
        val command = parser.parse("открой ворота паркинга на въезд")

        val open = assertIs<SkillCommand.Open>(command)
        assertEquals("parking_in", open.target.key)
    }

    @Test
    fun parsesParkingOut() {
        val command = parser.parse("сим-сим вверх")

        val open = assertIs<SkillCommand.Open>(command)
        assertEquals("parking_out", open.target.key)
    }

    @Test
    fun parsesParkingOutDirectCommand() {
        val command = parser.parse("открой ворота паркинга на выезд")

        val open = assertIs<SkillCommand.Open>(command)
        assertEquals("parking_out", open.target.key)
    }

    @Test
    fun oldCommandsFallbackToHelp() {
        val command = parser.parse("двор въезд")

        assertIs<SkillCommand.Help>(command)
    }

    @Test
    fun fallsBackToHelp() {
        val command = parser.parse("открой что-нибудь")

        assertIs<SkillCommand.Help>(command)
    }
}
