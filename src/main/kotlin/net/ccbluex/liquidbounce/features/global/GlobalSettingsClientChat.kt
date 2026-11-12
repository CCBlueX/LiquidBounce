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

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeoutOrNull
import net.ccbluex.axochat.protocol.Channel
import net.ccbluex.axochat.protocol.Serverbound
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.ClientChatErrorEvent
import net.ccbluex.liquidbounce.event.events.ClientChatMessageEvent
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.SessionEvent
import net.ccbluex.liquidbounce.event.events.UserLoggedInEvent
import net.ccbluex.liquidbounce.event.events.UserLoggedOutEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.features.chat.ChatMessageFormat
import net.ccbluex.liquidbounce.features.chat.ChatSession
import net.ccbluex.liquidbounce.features.chat.LiquidChatClient
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.misc.SelfDestruct.isDestructed
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.text.asPlayerHead
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.clientTag
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.removeMessage
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.asText
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.component.ResolvableProfile
import java.util.TreeSet
import kotlin.time.Duration.Companion.seconds

object GlobalSettingsClientChat : ToggleableValueGroup(
    name = "ClientChat",
    enabled = true,
    aliases = listOf("GlobalChat", "IRC")
) {

    private val logger = clientLogger(this.name)

    private object FilterConf : ToggleableValueGroup(this, "Filter", false) {
        private val usernames by textList("Usernames", TreeSet(String.CASE_INSENSITIVE_ORDER))
        private val filter by enumChoice("UsernameFilter", Filter.BLACKLIST)

        fun shouldShow(username: String): Boolean = !enabled || filter(username, usernames)
    }

    init {
        tree(FilterConf)
    }

    private val autoTranslate by multiEnumChoice<ClientChatMessageEvent.ChatGroup>("AutoTranslate")

    // the server only takes it at login
    internal val allowMessages by boolean("AllowMessages", true).onChanged {
        if (LiquidChatClient.isLoggedIn) {
            restartIfRunning()
        }
    }

    private val prefix = clientTag("LiquidChat")
    private val exceptionData = MessageMetadata(prefix = false, id = "LiquidChat#exception")
    private val messageData = MessageMetadata(prefix = false)

    private val filteredNames = hashSetOf<String>()

    /**
     * Prints why the chat cannot be used right now.
     */
    fun checkLoggedIn(): Boolean {
        val reason = when {
            LiquidChatClient.session?.isOpen != true -> "liquidbounce.liquidchat.notConnected"
            !LiquidChatClient.isLoggedIn -> "liquidbounce.liquidchat.notLoggedIn"
            else -> return true
        }

        chat(prefix, translation(reason).withStyle(ChatFormatting.GRAY), metadata = exceptionData)
        return false
    }

    fun notice(message: Component) = writeChat(PlainText.EMPTY, message)

    fun send(channel: Channel, message: String) {
        val session = LiquidChatClient.session
        if (!checkLoggedIn() || session == null) {
            return
        }

        // v1 only knows global and direct messages
        val packet = when {
            session.isModern -> Serverbound.ChatMessage(channel, message)
            channel == Channel.Global -> Serverbound.Message(message)
            channel is Channel.Direct -> Serverbound.PrivateMessage(channel.user, message)
            else -> null
        }
        if (packet == null) {
            notice(translation("liquidbounce.liquidchat.requiresV2").withStyle(ChatFormatting.GRAY))
            return
        }

        session.send(packet)
        if (!session.isModern && channel is Channel.Direct) {
            // v1 servers do not echo direct messages
            val prefix = ChatMessageFormat.directMessagePrefix(channel.user.asPlainText(ChatFormatting.BLUE))
            writeChat(prefix, regular(message))
        }
    }

    private fun registerChatWriteCommand(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("chat") {
            argument("message", StringArgumentType.greedyString()) { message ->
                exec { ctx ->
                    send(Channel.Global, ctx.get(message))
                    1
                }
            }
        }
    }

    init {
        CommandManager.register(::registerChatWriteCommand)
    }

    override fun onEnabledValueRegistration(value: Value<Boolean>): Value<Boolean> =
        super.onEnabledValueRegistration(value).onChanged { enabled ->
            // also outside of a world, unlike onEnabled and onDisabled
            if (enabled) {
                LiquidChatClient.start()
            } else {
                LiquidChatClient.stop()
                filteredNames.clear()
            }
        }

    @Suppress("unused")
    private val shutdownHandler = handler<ClientShutdownEvent> {
        LiquidChatClient.stop()
    }

    @Suppress("unused")
    private val sessionChange = handler<SessionEvent> {
        restartIfRunning()
    }

    @Suppress("unused")
    private val accountChange = handler<UserLoggedInEvent> {
        restartIfRunning()
    }

    @Suppress("unused")
    private val accountRemoval = handler<UserLoggedOutEvent> {
        restartIfRunning()
    }

    private fun restartIfRunning() {
        if (running) {
            LiquidChatClient.restart()
        }
    }

    @Suppress("unused")
    private val handleChatMessage = suspendHandler<ClientChatMessageEvent> { event ->
        if (!FilterConf.shouldShow(event.user.name)) {
            if (filteredNames.add(event.user.name)) {
                logger.info("[Chat] Message from ${event.user.name} has been filtered.")
            }
            return@suspendHandler
        }

        val resolvableProfile = ResolvableProfile.createUnresolved(event.user.uuid)
        withTimeoutOrNull(5.seconds) {
            resolvableProfile.resolveProfile(mc.services().profileResolver).await()
        }

        val playerSpritePart = resolvableProfile.asPlayerHead()
            .copyable(copyContent = event.user.uuid.toString())

        val prefix = ChatMessageFormat.messagePrefix(event, playerSpritePart)
        val content = if (event.author?.highlight == true) {
            event.message.asText().withStyle(ChatFormatting.WHITE)
        } else {
            regular(event.message)
        }

        // the profile lookup above can take longer than the server needs to take a message back
        val metadata = event.id?.let { id ->
            if (ChatSession.isRemoved(id)) {
                return@suspendHandler
            }
            MessageMetadata(prefix = false, id = messageId(id), remove = false)
        } ?: messageData
        writeChat(prefix, content.copyable(copyContent = event.message), metadata)

        if (event.chatGroup !in autoTranslate) {
            return@suspendHandler
        }

        val result = GlobalSettingsAutoTranslate.translate(text = event.message)
        if (result.isValid && event.id?.let(ChatSession::isRemoved) != true) {
            writeChat(prefix, result.toResultText(), metadata)
        }
    }

    /**
     * Takes a message the server removed out of the chat, with its translation.
     */
    fun remove(id: Long) = mc.gui.hud.chat.removeMessage(messageId(id))

    private fun messageId(id: Long) = "LiquidChat#message$id"

    /**
     * Connection errors carry no code and are left to the notifications.
     */
    @Suppress("unused")
    private val handleError = handler<ClientChatErrorEvent> { event ->
        if (event.code != null) {
            notice(event.error.asText().withStyle(ChatFormatting.RED))
        }
    }

    private fun writeChat(playerPrefix: Component, message: Component, metadata: MessageMetadata = messageData) {
        if (!inGame) {
            logger.info("[Chat] ${playerPrefix.string} ${message.string}")
        } else {
            chat(prefix, playerPrefix, message, metadata = metadata)
        }
    }

    /**
     * Overwrites the condition requirement for being in-game
     */
    override val running
        get() = !isDestructed && enabled

    init {
        if (enabled) {
            LiquidChatClient.start()
        }
    }

}
