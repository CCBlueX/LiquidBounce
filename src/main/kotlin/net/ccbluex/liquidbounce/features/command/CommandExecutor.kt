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

package net.ccbluex.liquidbounce.features.command

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ChatSendEvent
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.bold
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.highlight
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.openChat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.removeMessage
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.util.Formatting
import okio.appendingSink
import okio.buffer
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

/**
 * Links minecraft with the command engine
 */
object CommandExecutor : EventListener {

    private val commandHistoryFile = File(ConfigSystem.rootFolder, "command_history.txt")

    @Volatile
    private var isShuttingDown: Boolean = false

    /**
     * Add a wrapped suspend handler to [net.ccbluex.liquidbounce.features.command.builder.CommandBuilder]
     * if you don't want to block the render thread.
     *
     * @param allowParallel allow or prevent duplicated executions
     * @author MukjepScarlet
     */
    fun CommandBuilder.suspendHandler(
        allowParallel: Boolean = false,
        handler: suspend (command: Command, args: Array<Any>) -> Unit,
    ) = if (allowParallel) {
        this.handler { command, args ->
            commandCoroutineScope.launch {
                handler.invoke(command, args)
            }
        }
    } else {
        val running = AtomicBoolean(false)
        this.handler { command, args ->
            if (!running.compareAndSet(false, true)) {
                chat(
                    markAsError(
                        translation("liquidbounce.commandManager.commandExecuting", command.name)
                    ),
                    command
                )
                return@handler
            }

            // Progress message job
            val progressMessageMetadata = MessageMetadata(id = "C${command.name}#progress", remove = true)
            val progressJob = commandCoroutineScope.launch {
                val startAt = System.currentTimeMillis()
                var n = 0
                val chars = charArrayOf('|', '/', '-', '\\')
                while (isActive) {
                    delay(0.25.seconds)
                    val duration = (System.currentTimeMillis() - startAt) / 1000
                    val char = chars[n % chars.size]
                    chat(
                        regular("<$char> Executing command "),
                        variable(command.name),
                        regular(" ("),
                        variable(duration.toString()),
                        regular("s)"),
                        metadata = progressMessageMetadata
                    )
                    n++
                }
            }

            // Handler job
            commandCoroutineScope.launch {
                handler.invoke(command, args)
            }.invokeOnCompletion {
                running.set(false)
                progressJob.cancel()
                mc.inGameHud.chatHud.removeMessage(progressMessageMetadata.id)
            }
        }
    }

    /**
     * Handling exceptions for suspend handlers
     */
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (isShuttingDown && throwable is CancellationException) {
            // Client shutdown, ignored
        } else {
            handleExceptions(throwable)
        }
    }

    /**
     * Render thread scope
     */
    private val commandCoroutineScope = CoroutineScope(
        mc.asCoroutineDispatcher() + SupervisorJob() + coroutineExceptionHandler + CoroutineName("CommandExecutor")
    )

    private fun handleExceptions(e: Throwable) {
        when (e) {
            is CommandException -> {
                mc.inGameHud.chatHud.removeMessage("CommandManager#error")
                val data = MessageMetadata(id = "CommandManager#error", remove = false)
                chat(e.text.formatted(Formatting.RED), metadata = data)

                if (!e.usageInfo.isNullOrEmpty()) {
                    chat(highlight("Usage: ").bold(true), metadata = data)

                    // Zip the usage info together, e.g.
                    // ⬥ .friend add <name> [<alias>]
                    // ⬥ .friend remove <name>
                    for (usage in e.usageInfo) {
                        chat(
                            "\u2B25 ".asText()
                                .formatted(Formatting.BLUE)
                                .append(regular(CommandManager.Options.prefix + usage))
                                .onClick {
                                    mc.openChat(CommandManager.Options.prefix + usage)
                                },
                            metadata = data
                        )
                    }
                }
            }
            else -> {
                chat(
                    markAsError(
                        translation(
                            "liquidbounce.commandManager.exceptionOccurred",
                            e.javaClass.simpleName ?: "Class name missing", e.message ?: "No message"
                        )
                    ),
                    metadata = MessageMetadata(id = "CommandManager#error")
                )
                logger.error("An exception occurred while executing a command", e)
            }
        }
    }

    @Suppress("unused")
    private val shutdownHandler = handler<ClientShutdownEvent> {
        isShuttingDown = true
        commandCoroutineScope.cancel()
    }

    /**
     * Handles command execution
     */
    @Suppress("unused")
    private val chatEventHandler = handler<ChatSendEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) {
        if (!it.message.startsWith(CommandManager.Options.prefix)) {
            return@handler
        }

        val commandBody = it.message.substring(CommandManager.Options.prefix.length)
        try {
            CommandManager.execute(commandBody)
        } catch (e: Throwable) {
            handleExceptions(e)
        } finally {
            it.cancelEvent()
        }

        commandHistoryFile.appendingSink().buffer().use { sink ->
            sink.writeUtf8(commandBody)
                .writeByte('\n'.code)
        }
    }
}
