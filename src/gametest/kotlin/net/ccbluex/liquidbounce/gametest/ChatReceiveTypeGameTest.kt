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
package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Server messages must reach chat handlers as game messages; AutoChatGame and AutoQueue only read those.
 */
class ChatReceiveTypeGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()

        context.worldBuilder().create().use { world ->
            val types = CopyOnWriteArrayList<ChatReceiveEvent.ChatType>()
            val listener = object : EventListener {}

            listener.on(ChatReceiveEvent::class.java) { event ->
                if (event.message == MESSAGE) {
                    types += event.type
                }
            }.use {
                world.server.runCommand("tellraw @a \"$MESSAGE\"")
                context.waitFor({ types.isNotEmpty() }, TIMEOUT_TICKS)
            }

            check(types.single() == ChatReceiveEvent.ChatType.GAME_MESSAGE) { "a server message arrived as $types" }
        }
    }

    private companion object {
        const val MESSAGE = "A message from the server"
        const val TIMEOUT_TICKS = 100
    }

}
