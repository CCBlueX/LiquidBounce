/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, either version 3 of
 * the License, or (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.command.brigadier

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies the thin [ClientCommandSource]-fixed builder aliases ([literal]/[argument]/[arg])
 * used by the script command bindings and legacy call sites.
 */
class ClientCommandDslTest {

    private val dispatcher = CommandDispatcher<ClientCommandSource>()

    private fun execute(input: String): Int {
        val parse = dispatcher.parse(StringReader(input), ClientCommandSource)
        check(!parse.reader.canRead()) { "Parse failed: $input" }
        val context = parse.context.build(input)
        val executorContext = checkNotNull(context.deepestExecutableContext()) {
            "No executable command for: $input"
        }
        return checkNotNull(executorContext.command) { "No executable command for: $input" }
            .run(executorContext)
    }

    @Test
    fun `typed argument is parsed and retrieved by name`() {
        dispatcher.register(
            literal("dsltest1")
                .then(
                    argument("number", IntegerArgumentType.integer())
                        .executes { ctx -> ctx.arg<Int>("number") }
                )
        )

        assertEquals(42, execute("dsltest1 42"))
    }

    @Test
    fun `string argument keeps its value`() {
        dispatcher.register(
            literal("dsltest2")
                .then(
                    argument("name", StringArgumentType.string())
                        .executes { ctx -> if (ctx.arg<String>("name") == "hello") 1 else 0 }
                )
        )

        assertEquals(1, execute("dsltest2 hello"))
    }

    @Test
    fun `optional trailing argument falls back to parent executes`() {
        dispatcher.register(
            literal("dsltest3")
                .then(
                    argument("first", StringArgumentType.string())
                        // No second -> executes first
                        .executes { 1 }
                        .then(
                            argument("second", StringArgumentType.string())
                                .executes { 2 }
                        )
                )
        )

        assertEquals(1, execute("dsltest3 onlyfirst"))
        assertEquals(2, execute("dsltest3 first second"))
    }
}
