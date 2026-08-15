/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.CommandErrors
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.argument
import net.ccbluex.liquidbounce.features.command.brigadier.literal
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.minecraft.network.chat.contents.TranslatableContents
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies leftover parse input is classified the same way as vanilla
 * [CommandDispatcher.execute]: unknown command, invalid usage, or the
 * original parse exception (with usage attached).
 */
class CommandParseFailureTest {

    private val dispatcher = CommandDispatcher<ClientCommandSource>()
    private val usage = listOf("ping".asPlainText())
    private val hints = listOf("help".asPlainText())

    @BeforeTest
    fun bootstrapMinecraft() {
        MinecraftBootstrap.ensureInitialized()
    }

    @Test
    fun `unknown command keeps hints`() {
        dispatcher.register(literal("ping").executes { 1 })

        val parse = dispatcher.parse(StringReader("foo"), ClientCommandSource)
        val exception = mapParseFailure(parse, "foo", usage, hints)

        assertEquals("liquidbounce.commandManager.unknownCommand", exception.translationKey())
        assertEquals(hints, exception.usageInfo)
    }

    @Test
    fun `extra arguments are invalid usage`() {
        dispatcher.register(literal("ping").executes { 1 })

        val parse = dispatcher.parse(StringReader("ping extra"), ClientCommandSource)
        val exception = mapParseFailure(parse, "ping", usage, hints)

        assertEquals("liquidbounce.commandManager.invalidUsage", exception.translationKey())
        assertEquals(usage, exception.usageInfo)
    }

    @Test
    fun `vanilla integer failure keeps the raw message`() {
        dispatcher.register(
            literal("stack")
                .then(
                    argument("amount", IntegerArgumentType.integer(1))
                        .executes { 1 }
                )
        )

        val parse = dispatcher.parse(StringReader("stack abc"), ClientCommandSource)
        val exception = mapParseFailure(parse, "stack", usage, hints)

        val message = exception.text.string
        assertTrue(
            message.contains("integer", ignoreCase = true) || message.contains("int", ignoreCase = true),
            "expected integer parse error, got: $message",
        )
        // The cursor context line is appended after the usage lines.
        assertEquals(usage, exception.usageInfo.dropLast(1))
        assertTrue(exception.usageInfo.last().string.contains("<--[HERE]"))
    }

    @Test
    fun `custom parse exception keeps its translation and usage`() {
        dispatcher.register(
            literal("choice")
                .then(
                    argument("value", RejectingArgumentType)
                        .executes { 1 }
                )
        )

        val parse = dispatcher.parse(StringReader("choice nope"), ClientCommandSource)
        val exception = mapParseFailure(parse, "choice", usage, hints)

        assertEquals("liquidbounce.commandManager.invalidChoice", exception.translationKey())
        assertEquals(usage, exception.usageInfo.dropLast(1))
        assertTrue(exception.usageInfo.last().string.contains("<--[HERE]"))
    }

    private fun CommandException.translationKey(): String =
        (text.contents as TranslatableContents).key

    private object RejectingArgumentType : ArgumentType<String> {
        override fun parse(reader: StringReader): String {
            val token = reader.readString()
            throw CommandErrors.INVALID_CHOICE.createWithContext(reader, listOf(token, "value"))
        }
    }
}
