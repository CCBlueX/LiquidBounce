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


package net.ccbluex.liquidbounce.features.chat

import com.mojang.authlib.exceptions.InvalidCredentialsException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.ccbluex.axochat.AxochatClient
import net.ccbluex.axochat.AxochatSession
import net.ccbluex.axochat.Backoff
import net.ccbluex.axochat.LoginResult
import net.ccbluex.axochat.protocol.Channel
import net.ccbluex.axochat.protocol.Clientbound
import net.ccbluex.axochat.protocol.ErrorCode
import net.ccbluex.axochat.protocol.SuccessReason
import net.ccbluex.axochat.user.LegacyUser
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientChatErrorEvent
import net.ccbluex.liquidbounce.event.events.ClientChatMessageEvent
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.regular
import net.minecraft.ChatFormatting
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val MALFORMED_LOG_LENGTH = 200
private val ACCOUNT_RESTORE_TIMEOUT = 15.seconds

private val timeFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())

object LiquidChatClient {

    private val client = AxochatClient(
        URI(System.getProperty("net.ccbluex.liquidbounce.chat.url", "wss://chat.ccbluex.net/ws")),
        // the API client's interceptor rejects the 101 of the websocket upgrade
        HttpClient.defaultClient,
        malformed = { logger.warn("Malformed LiquidChat packet: {}", it.take(MALFORMED_LOG_LENGTH)) },
    )

    private val backoff = Backoff()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var connection: Job? = null

    @Volatile
    var session: AxochatSession? = null
        private set

    @Volatile
    var isLoggedIn = false
        private set

    // a login reports its own refusal; the same error must not show as a chat error too
    @Volatile
    private var authenticating = false

    // a connection that keeps failing or getting reset must not raise a notification per attempt
    private var announcedLogin = false
    private var announcedAuthFailure = false
    private var loggedInAt: TimeMark? = null

    @Synchronized
    fun start() {
        if (connection?.isActive != true) {
            restart()
        }
    }

    /**
     * Replaces the connection, as a new Minecraft session or account logs in differently.
     */
    @Synchronized
    fun restart() {
        val previous = connection
        connection = scope.launch {
            previous?.cancelAndJoin()
            stateChange(ClientChatStateChange.State.CONNECTING)
            client.keepConnected(backoff, failed = {
                EventManager.callEvent(ClientChatErrorEvent(it.localizedMessage ?: it.message ?: it.javaClass.name))
            }) { connected(this) }
        }
    }

    @Synchronized
    fun stop() {
        // kept, so a following start waits for the cancelled connection to finish
        connection?.cancel()
        session?.close()
    }

    private suspend fun connected(session: AxochatSession) = coroutineScope {
        if (!GlobalSettingsClientChat.running) {
            // self-destructed; the session closes and the connection ends
            stop()
            return@coroutineScope
        }

        launch(start = CoroutineStart.UNDISPATCHED) { session.incoming().collect(::handle) }
        this@LiquidChatClient.session = session
        try {
            stateChange(ClientChatStateChange.State.CONNECTED)
            session.negotiate()
            login(session)
            session.closed.await()
        } finally {
            disconnected()
        }
    }

    private fun disconnected() {
        session = null
        isLoggedIn = false
        ChatSession.reset()
        stateChange(ClientChatStateChange.State.DISCONNECTED)

        val stable = loggedInAt?.let { it.elapsedNow() >= backoff.reset } == true
        loggedInAt = null
        // a session reset right after login stays announced as online until one lasts again
        if (announcedLogin && stable) {
            announcedLogin = false
            announce("states.disconnected", NotificationEvent.Severity.INFO)
        }
    }

    private suspend fun login(session: AxochatSession) {
        // at startup the stored account loads alongside; logging in without it would pick the Minecraft identity
        withTimeoutOrNull(ACCOUNT_RESTORE_TIMEOUT) { ClientAccountManager.restored.await() }
        val account = ClientAccountManager.clientAccount
        val accessToken = if (session.isModern && account != ClientAccount.EMPTY_ACCOUNT) {
            runCatching { account.takeSession().accessToken.value }
                .onFailure { logger.warn("Could not refresh the LiquidBounce Account session", it) }
                .getOrNull()
        } else {
            null
        }

        authenticating = true
        try {
            if (accessToken != null && loginAccount(session, accessToken)) {
                return
            }
            logger.info("Logging in with the Minecraft account...")
            loginMojang(session)
        } finally {
            authenticating = false
        }
    }

    /**
     * @return false to fall back to the Minecraft account
     */
    private suspend fun loginAccount(session: AxochatSession, accessToken: String): Boolean {
        logger.info("Logging in with LiquidBounce Account...")
        stateChange(ClientChatStateChange.State.LOGGING_IN)
        when (val result = session.loginAccount(accessToken, GlobalSettingsClientChat.allowMessages)) {
            LoginResult.Success -> loggedIn()
            is LoginResult.Refused -> if (result.error.code == ErrorCode.LoginFailed) {
                logger.info("LiquidBounce Account login failed, falling back to Mojang...")
                return false
            }
            LoginResult.Closed -> {}
        }

        // a cracked session has no access token to join the session server with
        if (isLoggedIn && mc.user.accessToken.length > 1) {
            try {
                session.proveMinecraft(mc.user.name, mc.user.profileId, ::joinServer)
            } catch (e: InvalidCredentialsException) {
                logger.info("Could not prove the Minecraft session to LiquidChat", e)
            }
        }
        return true
    }

    private suspend fun loginMojang(session: AxochatSession) {
        stateChange(ClientChatStateChange.State.LOGGING_IN)
        val result = try {
            session.loginMojang(mc.user.name, mc.user.profileId, GlobalSettingsClientChat.allowMessages, ::joinServer)
        } catch (e: InvalidCredentialsException) {
            logger.warn("Could not join the session server for LiquidChat", e)
            stateChange(ClientChatStateChange.State.AUTHENTICATION_FAILED)
            if (!announcedAuthFailure) {
                announcedAuthFailure = true
                announce("authenticationFailed", NotificationEvent.Severity.ERROR)
            }
            return
        }

        when (result) {
            LoginResult.Success -> loggedIn()
            is LoginResult.Refused -> EventManager.callEvent(
                ClientChatErrorEvent(translateError(result.error), result.error.code.code)
            )
            LoginResult.Closed -> {}
        }
    }

    private fun loggedIn() {
        isLoggedIn = true
        loggedInAt = TimeSource.Monotonic.markNow()
        announcedAuthFailure = false
        stateChange(ClientChatStateChange.State.LOGGED_IN)
        if (!announcedLogin) {
            announcedLogin = true
            announce("states.loggedIn", NotificationEvent.Severity.INFO)
        }
    }

    private fun handle(packet: Clientbound) {
        ChatSession.update(packet)
        when (packet) {
            is Clientbound.Message -> EventManager.callEvent(ClientChatMessageEvent(packet.authorInfo, packet.content,
                ClientChatMessageEvent.ChatGroup.PUBLIC_CHAT))
            is Clientbound.PrivateMessage -> EventManager.callEvent(ClientChatMessageEvent(packet.authorInfo,
                packet.content, ClientChatMessageEvent.ChatGroup.PRIVATE_CHAT))
            is Clientbound.ChatMessage -> EventManager.callEvent(ClientChatMessageEvent(
                LegacyUser(packet.author.name, packet.author.uuid),
                packet.content,
                chatGroupOf(packet.channel),
                packet.channel,
                packet.author,
                packet.id,
            ))
            is Clientbound.MessageRemoved -> GlobalSettingsClientChat.remove(packet.id)
            is Clientbound.Error -> if (!authenticating) {
                EventManager.callEvent(ClientChatErrorEvent(translateError(packet), packet.code.code))
            }
            is Clientbound.Punished -> GlobalSettingsClientChat.notice(regular(translation(
                "liquidbounce.liquidchat.punished.${packet.kind}",
                packet.reason,
                packet.expires?.let { timeFormat.format(Instant.ofEpochMilli(it)) }
                    ?: translation("liquidbounce.liquidchat.punished.permanent").string,
            )).withStyle(ChatFormatting.RED))
            is Clientbound.Success -> if (packet.reason == SuccessReason.Report) {
                GlobalSettingsClientChat.notice(regular(translation("liquidbounce.liquidchat.success.report")))
            }

            else -> {}
        }
    }

}

private fun stateChange(state: ClientChatStateChange.State) = EventManager.callEvent(ClientChatStateChange(state))

private fun announce(key: String, severity: NotificationEvent.Severity) =
    notification("LiquidChat", translation("liquidbounce.liquidchat.$key"), severity)

private suspend fun joinServer(serverId: String) = withContext(Dispatchers.IO) {
    mc.services.sessionService.joinServer(mc.user.profileId, mc.user.accessToken, serverId)
}

private fun chatGroupOf(channel: Channel) = when (channel) {
    is Channel.Direct -> ClientChatMessageEvent.ChatGroup.PRIVATE_CHAT
    else -> ClientChatMessageEvent.ChatGroup.PUBLIC_CHAT
}

private fun translateError(error: Clientbound.Error): String {
    val code = error.code.code
    val key = "liquidbounce.liquidchat.error.${code.replaceFirstChar(Char::lowercaseChar)}"
    return if (LanguageManager.hasFallbackTranslation(key)) {
        translation(key, error.details ?: "").string
    } else {
        listOfNotNull(code, error.details).joinToString(": ")
    }
}
