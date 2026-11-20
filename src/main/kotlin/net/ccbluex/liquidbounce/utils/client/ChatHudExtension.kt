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

import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinChatComponentAccessor
import net.ccbluex.liquidbounce.interfaces.GuiMessageAddition
import net.ccbluex.liquidbounce.interfaces.GuiMessageLineAddition
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.multiplayer.chat.GuiMessage
import net.minecraft.client.multiplayer.chat.GuiMessageSource
import net.minecraft.client.multiplayer.chat.GuiMessageTag
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth

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
    val guiMessage = GuiMessage(mc.gui.hud.guiTicks, message, null, GuiMessageSource.SYSTEM_CLIENT, getTag())
        .withMetadata(id, count)
    this as MixinChatComponentAccessor
    this.invokeLogChatMessage(guiMessage)
    this.invokeAddMessageToDisplayQueue(guiMessage)
    this.invokeAddMessageToQueue(guiMessage)
}

/**
 * Replaces the content of the message carrying `metadata.id`, adding the message when the chat does not hold it
 * anymore. Unlike [addMessage] nothing is written to the log and the chat scroll is left alone.
 */
@Suppress("CAST_NEVER_SUCCEEDS")
fun ChatComponent.updateMessage(message: Component, metadata: MessageMetadata) {
    val id = requireNotNull(metadata.id) { "A chat message can only be updated by its id" }

    mc.execute {
        this as MixinChatComponentAccessor

        val index = allMessages.indexOfFirst { it.messageId() == id }
        if (index < 0) {
            addMessage(message, id, metadata.count)
            return@execute
        }

        val previous = allMessages[index]
        val updated = GuiMessage(
            mc.gui.hud.guiTicks, message, previous.signature(), previous.source(), previous.tag(),
        ).withMetadata(id, metadata.count)
        allMessages[index] = updated

        replaceLines(updated, id)
    }
}

/**
 * Removes all messages with the given ID.
 */
@Suppress("CAST_NEVER_SUCCEEDS")
fun ChatComponent.removeMessage(id: String?) = mc.execute {
    this as MixinChatComponentAccessor
    allMessages.removeIf { it.messageId() == id }
    trimmedMessages.removeIf { it.messageId() == id }
}

/**
 * Re-splits [updated] into the display queue where the lines of its id sit, keeping their order.
 *
 * Lines are prepended one by one on add, so the block of an id is contiguous but reversed; a message whose lines
 * were already pushed out of the history is only kept in [MixinChatComponentAccessor.getAllMessages].
 */
@Suppress("CAST_NEVER_SUCCEEDS")
private fun ChatComponent.replaceLines(updated: GuiMessage, id: String) {
    this as MixinChatComponentAccessor

    val lines = trimmedMessages
    val start = lines.indexOfFirst { it.messageId() == id }
    if (start < 0) {
        return
    }

    var end = start
    while (end < lines.size && lines[end].messageId() == id) {
        end++
    }
    lines.subList(start, end).clear()

    val split = updated.splitLines(mc.font, Mth.floor(invokeGetWidth() / invokeGetScale()))
    split.forEachIndexed { index, sequence ->
        val line = GuiMessage.Line(updated, sequence, index == split.lastIndex)
        (line as GuiMessageLineAddition).`liquid_bounce$setId`(id)
        lines.add(start, line)
    }
}

@Suppress("CAST_NEVER_SUCCEEDS")
private fun GuiMessage.withMetadata(id: String?, count: Int): GuiMessage = apply {
    (this as GuiMessageLineAddition).`liquid_bounce$setId`(id)
    (this as GuiMessageAddition).`liquid_bounce$setCount`(count)
}

private fun Any.messageId(): String? = (this as? GuiMessageLineAddition)?.`liquid_bounce$getId`()
