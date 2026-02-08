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

private val SYSTEM_TEXT: Component = Component.translatable("chat.tag.system")
private val SYSTEM_TEXT_SINGLE_PLAYER: Component = Component.translatable("chat.tag.system_single_player")

private val SYSTEM = GuiMessageTag(13684944, null, SYSTEM_TEXT, "System")
private val SYSTEM_SINGLE_PLAYER = GuiMessageTag(13684944, null, SYSTEM_TEXT_SINGLE_PLAYER, "System")

/**
 * Note: I don't know why these static methods will return null, but they do,
 * so we have to use the constants instead.
 */
private fun getTag() = if (mc.isSingleplayer) {
    GuiMessageTag.systemSinglePlayer() ?: SYSTEM_SINGLE_PLAYER
} else {
    GuiMessageTag.system() ?: SYSTEM
}

/**
 * Adds a message and assigns the ID to it.
 *
 * @see ChatComponent.addMessage
 */
@Suppress("CAST_NEVER_SUCCEEDS")
fun ChatComponent.addMessage(message: Component, id: String?, count: Int) = mc.execute {
    val guiMessage = GuiMessage(mc.gui.guiTicks, message, null, getTag())
    (guiMessage as GuiMessageLineAddition).`liquid_bounce$setId`(id)
    (guiMessage as GuiMessageAddition).`liquid_bounce$setCount`(count)
    this.logChatMessage(guiMessage)
    this.addMessageToDisplayQueue(guiMessage)
    this.addMessageToQueue(guiMessage)
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
