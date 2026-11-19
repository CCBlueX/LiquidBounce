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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import net.ccbluex.gametest.harness.SingleplayerGameTest
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.brigadier.literal
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinChatComponentAccessor
import net.ccbluex.liquidbounce.interfaces.GuiMessageLineAddition
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.text.RunnableClickEvent
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.chat.GuiMessage
import net.minecraft.network.chat.Component
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.minutes

private const val PROBE = "test_progress"
private const val PROGRESS_ID = "C$PROBE#progress"
private const val MARKER = "progress marker"

/**
 * A running async command owns one progress message: it is refreshed in place (no re-add, no reorder) and a click
 * on its `[✕]` cancels the handler, which frees the command to run again.
 */
class AsyncCommandProgressGameTest : SingleplayerGameTest({
    val cancellations = AtomicInteger()

    // Resolving a translation reads Minecraft's language option, which only the client thread may do
    val cancelledNotice = client { translation("liquidbounce.commandManager.commandCancelled", PROBE).string }
    val alreadyExecuting = client { translation("liquidbounce.commandManager.commandExecuting", PROBE).string }

    val node = literal(PROBE) {
        execSuspend {
            try {
                delay(1.minutes)
            } catch (e: CancellationException) {
                cancellations.incrementAndGet()
                throw e
            }
        }
    }.build()

    client { CommandManager.registerNodes([node]) }

    try {
        context.command(PROBE)
        awaitClient("the progress line") { it.progressLine() != null }

        // A newer message above it: a progress line that is re-added jumps to the newest position instead
        client { chat(MARKER.asPlainText()) }

        val before = client { it.progressLine() }
        awaitClient("the progress line to be refreshed") { it.progressLine() != before }

        check(client { it.progressMessages().size } == 1) { "the progress line was re-added instead of updated" }
        check(client {
            val messages = it.allMessages()
            messages.indexOfFirst { message -> message.messageId() == PROGRESS_ID } >
                messages.indexOfFirst { message -> MARKER in message.content().string }
        }) {
            "the progress line moved to the newest position instead of staying in place"
        }

        val cancel = checkNotNull(client { it.progressMessages().single().content().findCancel() }) {
            "the progress line has no cancel button"
        }

        client { cancel.run() }
        awaitClient("the handler to be cancelled") { cancellations.get() == 1 }
        awaitClient("the progress line to disappear") { it.progressLine() == null }
        check(client { it.chatTexts().any { text -> cancelledNotice in text } }) {
            "cancelling the command said nothing"
        }

        context.command(PROBE)
        awaitClient("the progress line to come back") { it.progressLine() != null }
        check(client { it.chatTexts().none { text -> alreadyExecuting in text } }) {
            "the cancelled command stayed marked as executing"
        }

        client { it.progressMessages().single().content().findCancel()?.run() }
        awaitClient("the second cancellation") { cancellations.get() == 2 }
    } finally {
        client { CommandManager.unregisterNodes([PROBE]) }
    }
})

@Suppress("CAST_NEVER_SUCCEEDS")
private fun Minecraft.allMessages(): List<GuiMessage> = (gui.hud.chat as MixinChatComponentAccessor).allMessages

private fun Minecraft.progressMessages(): List<GuiMessage> = allMessages().filter { it.messageId() == PROGRESS_ID }

private fun Minecraft.progressLine(): String? = progressMessages().singleOrNull()?.content()?.string

private fun Minecraft.chatTexts(): List<String> = allMessages().map { it.content().string }

@Suppress("CAST_NEVER_SUCCEEDS")
private fun GuiMessage.messageId(): String? = (this as? GuiMessageLineAddition)?.`liquid_bounce$getId`()

private fun Component.findCancel(): RunnableClickEvent? {
    (style.clickEvent as? RunnableClickEvent)?.let { return it }

    for (sibling in siblings) {
        sibling.findCancel()?.let { return it }
    }

    return null
}
