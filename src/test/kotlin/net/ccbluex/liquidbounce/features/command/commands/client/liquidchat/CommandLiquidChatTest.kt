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

package net.ccbluex.liquidbounce.features.command.commands.client.liquidchat

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class CommandLiquidChatTest {

    private val dispatcher = CommandDispatcher<ClientCommandSource>()

    @BeforeTest
    fun bootstrapMinecraft() {
        MinecraftBootstrap.ensureInitialized()
        CommandLiquidChat.register(dispatcher)
        CommandParty.register(dispatcher)
    }

    private fun parses(input: String) {
        val parse = dispatcher.parse(StringReader(input), ClientCommandSource)
        assertTrue(!parse.reader.canRead(), "Expected '$input' to parse (remaining: '${parse.reader.remaining}')")
    }

    @Test
    fun `social subcommands parse`() {
        parses("liquidchat friend Notch")
        parses("lc friends")
        parses("lc block")
        parses("lc block Spammer")
        parses("lc group create Bed Wars Team")
        parses("lc group invite \"Bed Wars Team\" Notch")
        parses("lc group say Bedwars gg")
        parses("lc server anyone here?")
        parses("lc report Spammer sells coins in chat")
    }

    @Test
    fun `party subcommands parse`() {
        parses("party")
        parses("party Notch")
        parses("p Notch")
        parses("party leader Notch")
        parses("party kick Notch")
        parses("party lock")
        parses("party warp")
    }

}
