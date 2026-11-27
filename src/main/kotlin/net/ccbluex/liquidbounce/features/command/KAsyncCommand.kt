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

package net.ccbluex.liquidbounce.features.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.onClickRun
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.removeMessage
import net.ccbluex.liquidbounce.utils.client.updateChat
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.text.formatAsTime
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

private val LOADING_CHARS = charArrayOf('|', '/', '-', '\\')

private val EXECUTING_COMMANDS: MutableSet<String> = ConcurrentHashMap.newKeySet()

class KAsyncCommand<S : Any> internal constructor(
    val allowParallel: Boolean,
    private val scope: CoroutineScope,
    val handler: Handler<S>,
) : Command<S> {

    override fun run(ctx: CommandContext<S>): Int {
        // The parsed node path serves as the stable display name of this command
        // (e.g. "friend add"), replacing the previous meta-model `command.name`.
        val commandPath = ctx.nodes.joinToString(" ") { it.node.name }

        if (!allowParallel && !EXECUTING_COMMANDS.add(commandPath)) {
            chat(
                markAsError(
                    translation("liquidbounce.commandManager.commandExecuting", commandPath)
                ),
                metadata = MessageMetadata(id = "C$commandPath#info", remove = true)
            )
            return 0
        }

        val progressMessageMetadata = MessageMetadata(id = "C$commandPath#progress", remove = true)

        // Handler job
        val handlerJob = scope.launch(CoroutineName(commandPath)) {
            handler(ctx)
        }

        // Progress message job
        val progressJob = scope.launch(CoroutineName("$commandPath Progress")) {
            val startAt = System.currentTimeMillis()
            var n = 0
            while (isActive) {
                delay(0.25.seconds)
                val duration = (System.currentTimeMillis() - startAt).toInt().formatAsTime()
                val char = LOADING_CHARS[n % LOADING_CHARS.size]
                updateChat(
                    translation(
                        "liquidbounce.commandManager.commandProgress",
                        regular("<$char>"),
                        variable(commandPath),
                        variable(duration),
                    ).append(CommonComponents.SPACE).append(cancelButton(commandPath, handlerJob)),
                    metadata = progressMessageMetadata
                )
                n++
            }
        }

        handlerJob.invokeOnCompletion {
            EXECUTING_COMMANDS.remove(commandPath)
            progressJob.cancel(it as? CancellationException)
            mc.gui.hud.chat.removeMessage(progressMessageMetadata.id)
        }

        return Command.SINGLE_SUCCESS
    }

    private fun cancelButton(commandPath: String, job: Job): MutableComponent = markAsError("[\u2715]")
        .onHover(HoverEvent.ShowText(CommonComponents.GUI_CANCEL))
        .onClickRun {
            if (!job.isActive) {
                return@onClickRun
            }

            val message = translation("liquidbounce.commandManager.commandCancelled", variable(commandPath))

            job.cancel(CancellationException(message.string))
            chat(
                message,
                metadata = MessageMetadata(id = "C$commandPath#cancelled", remove = true)
            )
        }

    fun interface Handler<S : Any> {
        suspend operator fun invoke(ctx: CommandContext<S>)
    }

}
