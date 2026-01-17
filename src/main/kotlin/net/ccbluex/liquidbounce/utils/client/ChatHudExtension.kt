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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.interfaces.GuiMessageAddition
import net.ccbluex.liquidbounce.interfaces.GuiMessageLineAddition
import net.minecraft.client.GuiMessage
import net.minecraft.client.GuiMessageTag
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.network.chat.Component

/**
 * Adds a message and assigns the ID to it.
 */
@Suppress("CAST_NEVER_SUCCEEDS")
fun ChatComponent.addMessage(message: Component, id: String?, count: Int) = mc.execute {
    val indicator = if (mc.isSingleplayer) GuiMessageTag.systemSinglePlayer() else GuiMessageTag.system()
    val chatHudLine = GuiMessage(mc.gui.guiTicks, message, null, indicator)
    (chatHudLine as GuiMessageLineAddition).`liquid_bounce$setId`(id)
    (chatHudLine as GuiMessageAddition).`liquid_bounce$setCount`(count)
    this.logChatMessage(chatHudLine)
    this.addMessageToDisplayQueue(chatHudLine)
    this.addMessageToQueue(chatHudLine)
}

/**
 * Removes all messages with the given ID.
 */
@Suppress("CAST_NEVER_SUCCEEDS")
fun ChatComponent.removeMessage(id: String?) = mc.execute {
    allMessages.removeIf {
        val removable = it as? GuiMessageLineAddition ?: return@removeIf false
        id == removable.`liquid_bounce$getId`()
    }
    trimmedMessages.removeIf {
        val removable = it as? GuiMessageLineAddition ?: return@removeIf false
        id == removable.`liquid_bounce$getId`()
    }
}
