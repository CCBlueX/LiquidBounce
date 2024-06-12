/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.DummyEvent
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat

/**
 * Rotations module
 *
 * Allows you to see server-sided rotations.
 */

object ModuleChatDebug : Module("ChatDebug", Category.RENDER) {

    var sequence: Sequence<DummyEvent>? = null

    // We can receive chat messages before the world is initialized,
    // so we have to handel events even before the that
    override fun handleEvents() = enabled

    private fun removeFormatting(message: String): String {
        return message.replace("§[0-9a-fk-or]", "")
    }

    @Suppress("unused")
    val onChat = handler<ChatReceiveEvent> { event ->
        chat("Message="+removeFormatting(event.message))
        chat("type="+event.type)
    }

}
