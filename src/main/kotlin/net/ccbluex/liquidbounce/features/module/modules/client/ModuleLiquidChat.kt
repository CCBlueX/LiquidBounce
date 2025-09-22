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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.event.SuspendHandlerBehavior.CANCEL_PREVIOUS
import net.ccbluex.liquidbounce.event.SuspendHandlerBehavior.DISCARD_LATEST
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.features.chat.ChatClient
import net.ccbluex.liquidbounce.features.chat.packet.ServerRequestJWTPacket
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.time.Duration.Companion.seconds

object ModuleLiquidChat : ClientModule("LiquidChat", Category.CLIENT, hide = true, state = true,
    aliases = listOf("GlobalChat", "IRC")
) {

    private var jwtToken by text("JwtToken", "")

    private val autoTranslate by multiEnumChoice<ClientChatMessageEvent.ChatGroup>("AutoTranslate")

    private val chatClient = ChatClient()
    private val prefix: Text = Text.empty()
        .formatted(Formatting.RESET).formatted(Formatting.GRAY)
        .append(Text.literal(this.name).withColor(Formatting.BLUE))
        .formatted(Formatting.BOLD)
        .append(Text.literal(" ▸ ").formatted(Formatting.RESET).withColor(Formatting.DARK_GRAY))
    private val exceptionData = MessageMetadata(prefix = false, id = "LiquidChat#exception")
    private val messageData = MessageMetadata(prefix = false)

    private fun createChatWriteCommand() = CommandBuilder
        .begin("chat")
        .parameter(
            ParameterBuilder
                .begin<String>("message")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .vararg()
                .build()
        )
        .handler {
            if (!chatClient.connected) {
                chat(
                    prefix, translation("liquidbounce.liquidchat.notConnected").formatted(Formatting.GRAY),
                    metadata = exceptionData
                )
                return@handler
            }

            if (!chatClient.loggedIn) {
                chat(
                    prefix, translation("liquidbounce.liquidchat.notLoggedIn").formatted(Formatting.GRAY),
                    metadata = exceptionData
                )
                return@handler
            }

            chatClient.sendMessage((args[0] as Array<*>).joinToString(" ") { it as String })
        }
        .build()

    private fun createChatJwtCommand() = CommandBuilder
        .begin("chatjwt")
        .handler {
            if (!chatClient.connected) {
                chat(
                    prefix, translation("liquidbounce.liquidchat.notConnected").formatted(Formatting.GRAY),
                    metadata = exceptionData
                )
                return@handler
            }

            chatClient.sendPacket(ServerRequestJWTPacket())
            chat(
                prefix, translation("liquidbounce.liquidchat.jwtTokenRequested").formatted(Formatting.GRAY),
                metadata = exceptionData
            )
        }
        .build()

    init {
        CommandManager.addCommand(createChatWriteCommand())
        CommandManager.addCommand(createChatJwtCommand())
    }

    override suspend fun enabledEffect() {
        chatClient.connect()
    }

    override fun onDisabled() {
        chatClient.disconnect()
    }

    @Suppress("unused")
    private val shutdownHandler = handler<ClientShutdownEvent> {
        chatClient.disconnect()
    }

    @Suppress("unused")
    private val repeatable = suspendHandler<GameTickEvent>(context = Dispatchers.IO, behavior = DISCARD_LATEST) {
        if (!chatClient.connected) {
            chatClient.connect()
        } else {
            // Wait 5 seconds before retrying
            delay(5.seconds)
        }
    }

    @Suppress("unused")
    private val sessionChange = suspendHandler<SessionEvent>(behavior = CANCEL_PREVIOUS) {
        chatClient.reconnect()
    }

    @Suppress("unused")
    private val handleChatMessage = suspendHandler<ClientChatMessageEvent> { event ->
        fun prefix(): MutableText = when (event.chatGroup) {
            ClientChatMessageEvent.ChatGroup.PUBLIC_CHAT ->
                event.user.name.asText().formatted(Formatting.GRAY).copyable(copyContent = event.user.name)
                    .append(" ▸ ".asText().formatted(Formatting.DARK_GRAY))
            ClientChatMessageEvent.ChatGroup.PRIVATE_CHAT ->
                "[".asText().formatted(Formatting.DARK_GRAY)
                    .append(
                        event.user.name.asText().formatted(Formatting.BLUE).copyable(copyContent = event.message)
                    )
                    .append("] ".asText().formatted(Formatting.DARK_GRAY))
        }

        writeChat(prefix().append(regular(event.message).copyable(copyContent = event.message)))

        if (event.chatGroup !in autoTranslate) {
            return@suspendHandler
        }

        val result = ModuleTranslation.translate(text = event.message)
        if (result.isValid) {
            writeChat(prefix().append(result.toResultText()))
        }
    }

    @Suppress("unused")
    private val handleIncomingJwtToken = suspendHandler<ClientChatJwtTokenEvent>(behavior = CANCEL_PREVIOUS) { event ->
        jwtToken = event.jwt
        chatClient.reconnect()
    }

    @Suppress("unused")
    private val handleStateChange = handler<ClientChatStateChange> {
        when (it.state) {
            ClientChatStateChange.State.CONNECTED -> {
                notification(
                    "LiquidChat",
                    translation("liquidbounce.liquidchat.states.connected"),
                    NotificationEvent.Severity.INFO
                )

                // When the token is not empty, we can try to login via JWT
                if (jwtToken.isNotEmpty()) {
                    logger.info("Logging in via JWT...")
                    chatClient.loginViaJwt(jwtToken)
                } else {
                    logger.info("Requesting to login into Mojang...")
                    chatClient.requestMojangLogin()
                }
            }
            ClientChatStateChange.State.LOGGED_IN -> {
                notification(
                    "LiquidChat",
                    translation("liquidbounce.liquidchat.states.loggedIn"),
                    NotificationEvent.Severity.INFO
                )
            }
            ClientChatStateChange.State.DISCONNECTED -> {
                notification(
                    "LiquidChat",
                    translation("liquidbounce.liquidchat.states.disconnected"),
                    NotificationEvent.Severity.INFO
                )
            }
            ClientChatStateChange.State.AUTHENTICATION_FAILED -> {
                notification(
                    "LiquidChat",
                    translation("liquidbounce.liquidchat.authenticationFailed"),
                    NotificationEvent.Severity.ERROR
                )
                logger.warn("Failed authentication to LiquidChat")
            }

            else -> {} // do not bother
        }
    }

    private fun writeChat(message: Text) {
        if (!inGame) {
            logger.info("[Chat] $message")
        } else {
            chat(prefix, message, metadata = messageData)
        }
    }

    /**
     * Overwrites the condition requirement for being in-game
     */
    override val running
        get() = !isDestructed && enabled

}
