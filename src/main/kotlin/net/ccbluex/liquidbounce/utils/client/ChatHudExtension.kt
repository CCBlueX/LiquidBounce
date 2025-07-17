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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.interfaces.ChatHudLineAddition
import net.ccbluex.liquidbounce.interfaces.ChatMessageAddition
import net.minecraft.client.gui.hud.ChatHud
import net.minecraft.client.gui.hud.ChatHudLine
import net.minecraft.client.gui.hud.MessageIndicator
import net.minecraft.text.Text

/**
 * Adds a message and assigns the ID to it.
 */
@Suppress("CAST_NEVER_SUCCEEDS")
fun ChatHud.addMessage(message: Text, id: String?, count: Int) {
    val indicator = if (mc.isConnectedToLocalServer) MessageIndicator.singlePlayer() else MessageIndicator.system()
    val chatHudLine = ChatHudLine(mc.inGameHud.ticks, message, null, indicator)
    (chatHudLine as ChatMessageAddition).`liquid_bounce$setId`(id)
    (chatHudLine as ChatHudLineAddition).`liquid_bounce$setCount`(count)
    this.logChatMessage(chatHudLine)
    this.addVisibleMessage(chatHudLine)
    this.addMessage(chatHudLine)
}

/**
 * Removes all messages with the given ID.
 */
@Suppress("CAST_NEVER_SUCCEEDS")
fun ChatHud.removeMessage(id: String?) {
    messages.removeIf {
        val removable = it as? ChatMessageAddition ?: return@removeIf false
        id == removable.`liquid_bounce$getId`()
    }
    visibleMessages.removeIf {
        val removable = it as? ChatMessageAddition ?: return@removeIf false
        id == removable.`liquid_bounce$getId`()
    }
}
