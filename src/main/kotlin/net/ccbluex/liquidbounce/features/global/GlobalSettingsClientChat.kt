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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.SuspendHandlerBehavior.CancelPrevious
import net.ccbluex.liquidbounce.event.eventListenerScope
import net.ccbluex.liquidbounce.event.events.ClientChatErrorEvent
import net.ccbluex.liquidbounce.event.events.ClientChatMessageEvent
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.SessionEvent
import net.ccbluex.liquidbounce.event.events.UserLoggedInEvent
import net.ccbluex.liquidbounce.event.events.UserLoggedOutEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.chat.AxochatClient
import net.ccbluex.liquidbounce.features.chat.ChatMessageFormat
import net.ccbluex.liquidbounce.features.chat.ChatSession
import net.ccbluex.liquidbounce.features.chat.packet.C2SSettingsPacket
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.misc.SelfDestruct.isDestructed
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.withColor
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.kotlin.optional
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.asText
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.contents.ObjectContents
import net.minecraft.network.chat.contents.objects.PlayerSprite
import net.minecraft.world.item.component.ResolvableProfile
import java.util.TreeSet
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val ACCOUNT_RESTORE_TIMEOUT = 15.seconds

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

    private val allowMessages by boolean("AllowMessages", true).onChanged { sendSettings() }
    private val serverChat by boolean("ServerChat", false).onChanged { sendSettings() }
    private val hideServer by boolean("HideServer", false).onChanged { sendSettings() }
    private val acceptFriendRequests by boolean("AcceptFriendRequests", true).onChanged { sendSettings() }

    val chatClient = AxochatClient { allowMessages }
    private val prefix: Component = "".asText()
        .withStyle(ChatFormatting.RESET).withStyle(ChatFormatting.GRAY)
        .append(this.name.asPlainText(ChatFormatting.BLUE))
        .withStyle(ChatFormatting.BOLD)
        .append(" ▸ ".asText().withStyle(ChatFormatting.RESET).withColor(ChatFormatting.DARK_GRAY))
    private val exceptionData = MessageMetadata(prefix = false, id = "LiquidChat#exception")
    private val messageData = MessageMetadata(prefix = false)

    private val filteredNames = hashSetOf<String>()

    /**
     * Prints why the chat cannot be used right now.
     */
    fun checkLoggedIn(): Boolean {
        val reason = when {
            !chatClient.isConnected -> "liquidbounce.liquidchat.notConnected"
            !chatClient.isLoggedIn -> "liquidbounce.liquidchat.notLoggedIn"
            else -> return true
        }

        chat(prefix, translation(reason).withStyle(ChatFormatting.GRAY), metadata = exceptionData)
        return false
    }

    fun notice(message: Component) = writeChat(PlainText.EMPTY, message)

    fun send(channel: String, message: String) {
        if (!checkLoggedIn()) {
            return
        }

        if (!chatClient.sendMessage(channel, message)) {
            notice(translation("liquidbounce.liquidchat.requiresV2").withStyle(ChatFormatting.GRAY))
        } else if (!chatClient.isModern && channel.startsWith(ChatSession.USER_PREFIX)) {
            // v1 servers do not echo direct messages
            val receiver = channel.removePrefix(ChatSession.USER_PREFIX)
            writeChat(ChatMessageFormat.directMessagePrefix(receiver), regular(message))
        }
    }

    private fun registerChatWriteCommand(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("chat") {
            argument("message", StringArgumentType.greedyString()) { message ->
                exec { ctx ->
                    send(ChatSession.GLOBAL, ctx.get(message))
                    1
                }
            }
        }
    }

    private fun registerPartyChatCommand(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("pc") {
            argument("message", StringArgumentType.greedyString()) { message ->
                exec { ctx ->
                    send(ChatSession.PARTY, ctx.get(message))
                    1
                }
            }
        }
    }

    private fun registerMessageCommand(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("msg", aliases = listOf("whisper")) {
            argument("user", StringArgumentType.word()) { user ->
                argument("message", StringArgumentType.greedyString()) { message ->
                    exec { ctx ->
                        send(ChatSession.USER_PREFIX + ctx.get(user), ctx.get(message))
                        1
                    }
                }
            }
        }
    }

    init {
        CommandManager.register(::registerChatWriteCommand)
        CommandManager.register(::registerMessageCommand)
        CommandManager.register(::registerPartyChatCommand)
    }

    private fun sendSettings() {
        if (chatClient.isLoggedIn && chatClient.isModern) {
            chatClient.sendPacket(C2SSettingsPacket(allowMessages, hideServer, acceptFriendRequests, serverChat))
        }
    }

    override fun onEnabled() {
        eventListenerScope.launch {
            chatClient.connect()
        }
    }

    override fun onDisabled() {
        chatClient.disconnect()
        filteredNames.clear()
    }

    @Suppress("unused")
    private val shutdownHandler = handler<ClientShutdownEvent> {
        chatClient.disconnect()
    }

    private var reconnectAttempts = 0

    @Suppress("unused")
    private val repeatable = tickHandler(Dispatchers.IO) {
        if (chatClient.isConnected) {
            if (chatClient.isLoggedIn) {
                reconnectAttempts = 0
            }
            delay(5.seconds)
            return@tickHandler
        }

        if (reconnectAttempts > 0) {
            // Exponential backoff with jitter, so a server restart is not hit by every client at once
            val backoff = (2.seconds * (1 shl (reconnectAttempts - 1).coerceAtMost(6))).coerceAtMost(2.minutes)
            delay(backoff * Random.nextDouble(0.75, 1.25))
        }
        reconnectAttempts++
        chatClient.connect()
    }

    @Suppress("unused")
    private val sessionChange = suspendHandler<SessionEvent>(behavior = CancelPrevious) {
        chatClient.reconnect()
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

        val playerSpritePart = MutableComponent.create(
            ObjectContents(PlayerSprite(resolvableProfile, false), optional())
        ).copyable(copyContent = event.user.uuid.toString())

        val prefix = ChatMessageFormat.messagePrefix(event, playerSpritePart)
        val content = if (event.author?.highlight == true) {
            event.message.asText().withStyle(ChatFormatting.WHITE)
        } else {
            regular(event.message)
        }

        writeChat(prefix, content.copyable(copyContent = event.message))

        if (event.chatGroup !in autoTranslate) {
            return@suspendHandler
        }

        val result = GlobalSettingsAutoTranslate.translate(text = event.message)
        if (result.isValid) {
            writeChat(prefix, result.toResultText())
        }
    }

    @Volatile
    private var accountLoginPending = false

    @Suppress("unused")
    private val accountChange = suspendHandler<UserLoggedInEvent>(behavior = CancelPrevious) {
        chatClient.reconnect()
    }

    @Suppress("unused")
    private val accountRemoval = suspendHandler<UserLoggedOutEvent>(behavior = CancelPrevious) {
        chatClient.reconnect()
    }

    @Suppress("unused")
    private val handleLoginFailure = handler<ClientChatErrorEvent> { event ->
        if (event.code == "LoginFailed" && accountLoginPending) {
            accountLoginPending = false
            logger.info("LiquidBounce Account login failed, falling back to Mojang...")
            chatClient.requestMojangLogin()
        }
    }

    private suspend fun login() {
        // at startup the stored account loads alongside; logging in without it would pick the Minecraft identity
        withTimeoutOrNull(ACCOUNT_RESTORE_TIMEOUT) { ClientAccountManager.restored.await() }
        val account = ClientAccountManager.clientAccount
        val accessToken = if (chatClient.isModern && account != ClientAccount.EMPTY_ACCOUNT) {
            runCatching { account.takeSession().accessToken.value }
                .onFailure { logger.warn("Could not refresh the LiquidBounce Account session", it) }
                .getOrNull()
        } else {
            null
        }

        if (accessToken != null) {
            logger.info("Logging in with LiquidBounce Account...")
            accountLoginPending = true
            chatClient.loginAccount(accessToken)
        } else {
            logger.info("Requesting to login into Mojang...")
            accountLoginPending = false
            chatClient.requestMojangLogin()
        }
    }

    @Suppress("unused")
    private val handleStateChange = suspendHandler<ClientChatStateChange>(behavior = CancelPrevious) {
        when (it.state) {
            ClientChatStateChange.State.CONNECTED -> {
                notification(
                    "LiquidChat",
                    translation("liquidbounce.liquidchat.states.connected"),
                    NotificationEvent.Severity.INFO
                )

                chatClient.negotiate()
                login()
            }
            ClientChatStateChange.State.LOGGED_IN -> {
                accountLoginPending = false
                sendSettings()
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

    private fun writeChat(playerPrefix: Component, message: Component) {
        if (!inGame) {
            logger.info("[Chat] ${playerPrefix.string} ${message.string}")
        } else {
            chat(prefix, playerPrefix, message, metadata = messageData)
        }
    }

    /**
     * Overwrites the condition requirement for being in-game
     */
    override val running
        get() = !isDestructed && enabled

}
