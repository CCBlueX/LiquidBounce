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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.axochat.AxochatClient
import net.ccbluex.axochat.AxochatSession
import net.ccbluex.axochat.LoginResult
import net.ccbluex.axochat.protocol.Channel
import net.ccbluex.axochat.protocol.Clientbound
import net.ccbluex.axochat.protocol.ErrorCode
import net.ccbluex.axochat.protocol.Serverbound
import net.ccbluex.axochat.protocol.SuccessReason
import net.ccbluex.axochat.user.LegacyUser
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientChatErrorEvent
import net.ccbluex.liquidbounce.event.events.ClientChatMessageEvent
import net.ccbluex.liquidbounce.event.events.ClientChatPacketEvent
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import java.net.URI
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

private const val MALFORMED_LOG_LENGTH = 200

private val KNOWN_ERRORS = setOf(
    "NotSupported", "LoginFailed", "NotLoggedIn", "AlreadyLoggedIn", "MojangRequestMissing", "NotPermitted",
    "NotBanned", "Banned", "RateLimited", "PrivateMessageNotAccepted", "EmptyMessage", "MessageTooLong",
    "InvalidCharacter", "InvalidId", "Internal", "UnknownUser", "UnknownChannel", "UnknownGroup", "Muted",
    "NotInParty", "AlreadyInParty", "PartyFull", "PartyLocked", "NoInvite", "NotFriends", "AlreadyFriends",
    "AccountRequired", "GroupFull", "InvalidName", "TooLarge",
)

class LiquidChatClient(private val allowMessages: () -> Boolean) {

    private val client = AxochatClient(
        URI(System.getProperty("net.ccbluex.liquidbounce.chat.url", "wss://chat.liquidbounce.net:7886/ws")),
        // the certificate of the chat server expired years ago
        HttpClient.client.newBuilder().sslSocketFactory(trustingContext().socketFactory, TrustingManager).build(),
        malformed = { logger.warn("Malformed LiquidChat packet: {}", it.take(MALFORMED_LOG_LENGTH)) },
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var session: AxochatSession? = null

    @Volatile
    private var isConnecting = false

    // a login reports its own refusal; the same error must not show as a chat error too
    @Volatile
    private var authenticating = false

    val isConnected: Boolean
        get() = session?.isOpen == true

    @Volatile
    var isLoggedIn = false
        private set

    val isModern: Boolean
        get() = session?.isModern == true

    suspend fun connect() {
        if (isConnecting || isConnected) {
            return
        }

        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.CONNECTING))
        isConnecting = true
        isLoggedIn = false
        try {
            val session = client.connect()
            this.session = session
            scope.launch(start = CoroutineStart.UNDISPATCHED) { session.packets.collect(::handle) }
            scope.launch {
                session.closed.await()
                if (this@LiquidChatClient.session === session) {
                    this@LiquidChatClient.session = null
                    isLoggedIn = false
                    EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.DISCONNECTED))
                }
            }
            EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.CONNECTED))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            EventManager.callEvent(ClientChatErrorEvent(e.localizedMessage ?: e.message ?: e.javaClass.name))
        } finally {
            isConnecting = false
        }
    }

    fun disconnect() {
        session?.let {
            session = null
            it.close()
        }
        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.DISCONNECTED))
        isConnecting = false
        isLoggedIn = false
    }

    suspend fun reconnect() {
        disconnect()
        connect()
    }

    suspend fun negotiate(): Int = session?.negotiate() ?: 1

    suspend fun loginAccount(accessToken: String): LoginResult = authenticate {
        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.LOGGING_IN))
        it.loginAccount(accessToken, allowMessages())
    }

    suspend fun loginMojang(): LoginResult {
        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.LOGGING_IN))
        val result = try {
            authenticate { it.loginMojang(mc.user.name, mc.user.profileId, allowMessages(), ::joinServer) }
        } catch (e: InvalidCredentialsException) {
            logger.warn("Could not join the session server for LiquidChat", e)
            EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.AUTHENTICATION_FAILED))
            return LoginResult.Closed
        }
        if (result is LoginResult.Refused) {
            EventManager.callEvent(ClientChatErrorEvent(translateError(result.error), result.error.code.code))
        }
        return result
    }

    /**
     * Cracked sessions cannot join the session server and stay unproven.
     */
    suspend fun proveMinecraft(): LoginResult = try {
        authenticate { it.proveMinecraft(mc.user.name, mc.user.profileId, ::joinServer) }
    } catch (e: InvalidCredentialsException) {
        logger.info("Could not prove the Minecraft session to LiquidChat", e)
        LoginResult.Closed
    }

    private suspend fun authenticate(login: suspend (AxochatSession) -> LoginResult): LoginResult {
        val session = session ?: return LoginResult.Closed
        authenticating = true
        return try {
            login(session)
        } finally {
            authenticating = false
        }
    }

    /**
     * @return false if the channel needs protocol v2; v1 only knows global and direct messages
     */
    fun sendMessage(channel: String, message: String): Boolean {
        val target = Channel.parse(channel)
        val packet = when {
            isModern -> Serverbound.ChatMessage(channel, message)
            target == Channel.Global -> Serverbound.Message(message)
            target is Channel.User -> Serverbound.PrivateMessage(target.user, message)
            else -> return false
        }
        sendPacket(packet)
        return true
    }

    fun sendPacket(packet: Serverbound) {
        session?.send(packet)
    }

    private fun handle(packet: Clientbound) {
        when (packet) {
            is Clientbound.Message -> EventManager.callEvent(ClientChatMessageEvent(packet.authorInfo, packet.content,
                ClientChatMessageEvent.ChatGroup.PUBLIC_CHAT))
            is Clientbound.PrivateMessage -> EventManager.callEvent(ClientChatMessageEvent(packet.authorInfo,
                packet.content, ClientChatMessageEvent.ChatGroup.PRIVATE_CHAT))
            is Clientbound.ChatMessage -> EventManager.callEvent(ClientChatMessageEvent(
                LegacyUser(packet.author.name, packet.author.uuid),
                packet.content,
                chatGroupOf(packet.target),
                packet.channel,
                packet.author,
                packet.id,
            ))
            is Clientbound.Error -> if (!authenticating) {
                EventManager.callEvent(ClientChatErrorEvent(translateError(packet), packet.code.code))
            }
            is Clientbound.Success -> if (packet.reason == SuccessReason.Login) {
                isLoggedIn = true
                EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.LOGGED_IN))
            }

            else -> {}
        }

        EventManager.callEvent(ClientChatPacketEvent(packet))
    }

}

private suspend fun joinServer(serverId: String) = withContext(Dispatchers.IO) {
    mc.services.sessionService.joinServer(mc.user.profileId, mc.user.accessToken, serverId)
}

private fun chatGroupOf(channel: Channel?) = when (channel) {
    Channel.Server -> ClientChatMessageEvent.ChatGroup.SERVER_CHAT
    Channel.Party -> ClientChatMessageEvent.ChatGroup.PARTY_CHAT
    is Channel.Group -> ClientChatMessageEvent.ChatGroup.GROUP_CHAT
    is Channel.User -> ClientChatMessageEvent.ChatGroup.PRIVATE_CHAT
    else -> ClientChatMessageEvent.ChatGroup.PUBLIC_CHAT
}

private fun translateError(packet: Clientbound.Error): String {
    val code = packet.code.code
    if (code !in KNOWN_ERRORS) {
        return listOfNotNull(code, packet.details).joinToString(": ")
    }

    val key = "liquidbounce.liquidchat.error.${code.replaceFirstChar(Char::lowercaseChar)}"
    return translation(key, packet.details ?: "").string
}

private fun trustingContext(): SSLContext = SSLContext.getInstance("TLS").apply {
    init(null, arrayOf(TrustingManager), SecureRandom())
}

private object TrustingManager : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
