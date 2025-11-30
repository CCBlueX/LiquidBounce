/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.actions

import kotlinx.coroutines.delay

object AutoQueueActionChat : AutoQueueAction("Chat") {
    private val startDelay by intRange("StartDelay", 0..0, 0..2000, "ms")

    private val message by text("Message", "/play solo_normal")

    override suspend fun execute() {
        delay(startDelay.random().toLong())

        if (message.startsWith("/")) {
            network.sendChatCommand(message.substring(1))
        } else {
            network.sendChatMessage(message)
        }
    }
}
