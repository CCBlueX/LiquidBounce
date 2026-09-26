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
package net.ccbluex.liquidbounce.features.command.arguments

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.argument
import net.ccbluex.liquidbounce.features.command.brigadier.literal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ClientStringArgumentTypeTest {

    @Test
    fun `word consumes non-ascii and leaves the next token`() {
        val reader = StringReader("Привет extra")
        assertEquals("Привет", ClientStringArgumentType.word().parse(reader))
        assertEquals(" extra", reader.remaining)
    }

    @Test
    fun `word accepts a symbol-only prefix`() {
        val reader = StringReader("!")
        assertEquals("!", ClientStringArgumentType.word().parse(reader))
        assertFalse(reader.canRead())
    }

    @Test
    fun `string strips quotes and keeps inner unicode and spaces`() {
        val reader = StringReader("\"你好 世界\"")
        assertEquals("你好 世界", ClientStringArgumentType.string().parse(reader))
        assertFalse(reader.canRead())
    }

    @Test
    fun `string unquoted non-ascii is a single token`() {
        val reader = StringReader("中文查询")
        assertEquals("中文查询", ClientStringArgumentType.string().parse(reader))
        assertFalse(reader.canRead())
    }

    @Test
    fun `dispatcher fully parses a non-ascii word argument`() {
        val dispatcher = CommandDispatcher<ClientCommandSource>()
        dispatcher.register(
            literal("cmd").then(
                argument("name", ClientStringArgumentType.word()).executes { ctx ->
                    ctx.getArgument("name", String::class.java).length
                }
            )
        )

        val input = "cmd 하나둘셋"
        val parse = dispatcher.parse(StringReader(input), ClientCommandSource)
        assertFalse(parse.reader.canRead(), "remaining: '${parse.reader.remaining}'")

        val context = parse.context.build(input)
        val result = checkNotNull(context.command ?: context.child?.command).run(
            context.child ?: context
        )
        assertEquals(4, result)
    }
}
