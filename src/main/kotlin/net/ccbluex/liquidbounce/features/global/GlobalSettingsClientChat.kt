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

package net.ccbluex.liquidbounce.features.global

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.SuspendHandlerBehavior.CancelPrevious
import net.ccbluex.liquidbounce.event.eventListenerScope
import net.ccbluex.liquidbounce.event.events.ClientChatJwtTokenEvent
import net.ccbluex.liquidbounce.event.events.ClientChatMessageEvent
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.SessionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.chat.AxochatClient
import net.ccbluex.liquidbounce.features.chat.packet.C2SRequestJWTPacket
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.withColor
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import kotlin.time.Duration.Companion.seconds

object GlobalSettingsClientChat : ToggleableValueGroup(
    name = "ClientChat",
    enabled = true,
    aliases = listOf("GlobalChat", "IRC")
) {

    private var jwtToken by text("JwtToken", "")

    private val autoTranslate by multiEnumChoice<ClientChatMessageEvent.ChatGroup>("AutoTranslate")

    private val chatClient = AxochatClient()
    private val prefix: Component = "".asText()
        .withStyle(ChatFormatting.RESET).withStyle(ChatFormatting.GRAY)
        .append(this.name.asPlainText(ChatFormatting.BLUE))
        .withStyle(ChatFormatting.BOLD)
        .append(" ▸ ".asText().withStyle(ChatFormatting.RESET).withColor(ChatFormatting.DARK_GRAY))
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
            if (!chatClient.isConnected) {
                chat(
                    prefix, translation("liquidbounce.liquidchat.notConnected").withStyle(ChatFormatting.GRAY),
                    metadata = exceptionData
                )
                return@handler
            }

            if (!chatClient.isLoggedIn) {
                chat(
                    prefix, translation("liquidbounce.liquidchat.notLoggedIn").withStyle(ChatFormatting.GRAY),
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
            if (!chatClient.isConnected) {
                chat(
                    prefix, translation("liquidbounce.liquidchat.notConnected").withStyle(ChatFormatting.GRAY),
                    metadata = exceptionData
                )
                return@handler
            }

            chatClient.sendPacket(C2SRequestJWTPacket())
            chat(
                prefix, translation("liquidbounce.liquidchat.jwtTokenRequested").withStyle(ChatFormatting.GRAY),
                metadata = exceptionData
            )
        }
        .build()

    init {
        CommandManager.addCommand(createChatWriteCommand())
        CommandManager.addCommand(createChatJwtCommand())
    }

    override fun onEnabled() {
        eventListenerScope.launch {
            chatClient.connect()
        }
    }

    override fun onDisabled() {
        chatClient.disconnect()
    }

    @Suppress("unused")
    private val shutdownHandler = handler<ClientShutdownEvent> {
        chatClient.disconnect()
    }

    @Suppress("unused")
    private val repeatable = tickHandler(Dispatchers.IO) {
        if (!chatClient.isConnected) {
            chatClient.connect()
        } else {
            // Wait 5 seconds before retrying
            delay(5.seconds)
        }
    }

    @Suppress("unused")
    private val sessionChange = suspendHandler<SessionEvent>(behavior = CancelPrevious) {
        chatClient.reconnect()
    }

    @Suppress("unused")
    private val handleChatMessage = suspendHandler<ClientChatMessageEvent> { event ->
        fun prefix(): MutableComponent = when (event.chatGroup) {
            ClientChatMessageEvent.ChatGroup.PUBLIC_CHAT ->
                event.user.name.asText().withStyle(ChatFormatting.GRAY).copyable(copyContent = event.user.name)
                    .append(" ▸ ".asPlainText(ChatFormatting.DARK_GRAY))
            ClientChatMessageEvent.ChatGroup.PRIVATE_CHAT ->
                "[".asText().withStyle(ChatFormatting.DARK_GRAY)
                    .append(
                        event.user.name.asText().withStyle(ChatFormatting.BLUE).copyable(copyContent = event.message)
                    )
                    .append("] ".asPlainText(ChatFormatting.DARK_GRAY))
        }

        writeChat(prefix().append(regular(event.message).copyable(copyContent = event.message)))

        if (event.chatGroup !in autoTranslate) {
            return@suspendHandler
        }

        val result = GlobalSettingsAutoTranslate.translate(text = event.message)
        if (result.isValid) {
            writeChat(prefix().append(result.toResultText()))
        }
    }

    @Suppress("unused")
    private val handleIncomingJwtToken = suspendHandler<ClientChatJwtTokenEvent>(behavior = CancelPrevious) { event ->
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

    private fun writeChat(message: Component) {
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
