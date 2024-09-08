/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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

import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.chat.ChatClient
import net.ccbluex.liquidbounce.features.chat.packet.ServerRequestJWTPacket
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.register.IncludeModule
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.*

@IncludeModule
object ModuleLiquidChat : Module("LiquidChat", Category.CLIENT, hide = true, state = true,
    aliases = arrayOf("GlobalChat")) {

    private var jwtToken by text("JwtToken", "")
    private val chatClient = ChatClient()

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
        .handler { _, args ->
            if (!chatClient.connected) {
                chat("§9§lLiquidChat §8▸ " +
                    "§7${translation("liquidbounce.liquidchat.notConnected").convertToString()}")
                return@handler
            }

            if (!chatClient.loggedIn) {
                chat("§9§lLiquidChat §8▸ " +
                    "§7${translation("liquidbounce.liquidchat.notLoggedIn").convertToString()}")
                return@handler
            }

            chatClient.sendMessage((args[0] as Array<*>).joinToString(" ") { it as String })
        }
        .build()

    private fun createChatJwtCommand() = CommandBuilder
        .begin("chatjwt")
        .handler { command, anies ->
            if (!chatClient.connected) {
                chat("§9§lLiquidChat §8▸ " +
                    "§7${translation("liquidbounce.liquidchat.notConnected").convertToString()}")
                return@handler
            }

            chatClient.sendPacket(ServerRequestJWTPacket())
            chat("§9§lLiquidChat §8▸ " +
                "§7${translation("liquidbounce.liquidchat.jwtTokenRequested").convertToString()}")
        }
        .build()

    init {
        CommandManager.addCommand(createChatWriteCommand())
        CommandManager.addCommand(createChatJwtCommand())
    }

    override fun enable() {
        chatClient.connectAsync()
        super.enable()
    }

    override fun disable() {
        chatClient.disconnect()
        super.disable()
    }

    val repeatable = repeatable {
        if (!chatClient.connected) {
            chatClient.connectAsync()

            // Wait 60 seconds before retrying
            waitSeconds(60)
        }
    }

    @Suppress("unused")
    val sessionChange = handler<SessionEvent> {
        chatClient.reconnect()
    }

    @Suppress("unused")
    val handleChatMessage = handler<ClientChatMessageEvent> {
        when (it.chatGroup) {
            ClientChatMessageEvent.ChatGroup.PUBLIC_CHAT -> writeChat("${it.user.name} §8▸ §7${it.message}")
            ClientChatMessageEvent.ChatGroup.PRIVATE_CHAT -> writeChat("§8[§9${it.user.name}§8] §7${it.message}")
        }
    }

    @Suppress("unused")
    val handleIncomingJwtToken = handler<ClientChatJwtTokenEvent> { event ->
        jwtToken = event.jwt
        chatClient.reconnect()
    }

    @Suppress("unused")
    val handleStateChange = handler<ClientChatStateChange> {
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

    private fun writeChat(message: String) {
        if (!inGame) {
            logger.info("[Chat] $message")
        } else {
            player.sendMessage("§9§lLiquidChat §8▸ §7$message".asText(), false)
        }
    }

    override fun handleEvents() = enabled

}
