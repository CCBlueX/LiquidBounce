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
package net.ccbluex.liquidbounce.features.command.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import net.ccbluex.liquidbounce.features.command.arguments.BooleanArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.Vec3ArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.commands.client.CommandPanic
import net.ccbluex.liquidbounce.features.command.commands.client.CommandTargets
import net.ccbluex.liquidbounce.features.command.preset.pagedList
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.ccbluex.liquidbounce.utils.text.asText
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Parse-level checks for migrated command trees: required chains, aliases,
 * Vec3 + optional HighTP, and `list` paging subcommands.
 *
 * These tests only assert that input fully parses onto an executable node. Handlers
 * that need a running client are not executed.
 */
class CommandTreeTest {

    private val dispatcher = CommandDispatcher<ClientCommandSource>()

    @BeforeTest
    fun bootstrapMinecraft() {
        MinecraftBootstrap.ensureInitialized()
        CommandPanic.register(dispatcher)
        CommandTargets.register(dispatcher)
        dispatcher.register("teleport") {
            argument("pos", Vec3ArgumentType()) {
                optional("highTp", BooleanArgumentType("highTp"), default = false) {
                    exec { 1 }
                }
            }
        }
        dispatcher.register("hidelike") {
            pagedList(
                header = { "".asText() },
                items = { emptyList<String>() },
                eachRow = { _, _ -> "".asText() },
            )
        }
    }

    private fun parses(input: String) {
        val parse = dispatcher.parse(StringReader(input), ClientCommandSource)
        assertTrue(
            !parse.reader.canRead(),
            "Expected '$input' to fully parse (remaining: '${parse.reader.remaining}')"
        )
    }

    private fun doesNotParse(input: String) {
        val parse = dispatcher.parse(StringReader(input), ClientCommandSource)
        assertFalse(
            !parse.reader.canRead() && parse.exceptions.isEmpty() && hasExecutableCommand(parse),
            "Expected '$input' to fail parsing"
        )
    }

    private fun hasExecutableCommand(parse: com.mojang.brigadier.ParseResults<ClientCommandSource>): Boolean {
        var context = parse.context
        while (context != null) {
            if (context.command != null) return true
            context = context.child
        }
        return false
    }

    @Test
    fun `required argument chains need their full input`() {
        parses("panic all")
        // panic root itself executes without arguments
        doesNotParse("panic all extra")
    }

    @Test
    fun `aliases execute through redirects`() {
        parses("target combat Players")
        parses("enemies visual Players")
    }

    @Test
    fun `teleport requires a vec3 and accepts an optional highTp flag`() {
        parses("teleport 1 2 3")
        parses("teleport 1 2 3 true")
        doesNotParse("teleport 1 2")
    }

    @Test
    fun `hide lists through the list subcommand`() {
        parses("hidelike list")
        parses("hidelike list 1")
        doesNotParse("hidelike 1")
    }

}
