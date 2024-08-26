/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.chat.Client
import net.ccbluex.liquidbounce.chat.packet.packets.*
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.login.UserUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.event.ClickEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.IChatComponent
import java.net.URI
import java.net.URISyntaxException
import java.util.regex.Pattern
import kotlin.concurrent.thread

object LiquidChat : Module("LiquidChat", Category.MISC, subjective = true, gameDetecting = false) {

    init {
        state = true
        inArray = false
    }

    var jwt by object : BoolValue("JWT", false) {
        override fun onChanged(oldValue: Boolean, newValue: Boolean) {
            if (state) {
                state = false
                state = true
            }
        }
    }

    var jwtToken = ""

    val client = object : Client() {

        /**
         * Handle connect to web socket
         */
        override fun onConnect() = displayChatMessage("§7[§a§lChat§7] §9Connecting to chat server...")

        /**
         * Handle connect to web socket
         */
        override fun onConnected() = displayChatMessage("§7[§a§lChat§7] §9Connected to chat server!")

        /**
         * Handle handshake
         */
        override fun onHandshake(success: Boolean) {}

        /**
         * Handle disconnect
         */
        override fun onDisconnect() = displayChatMessage("§7[§a§lChat§7] §cDisconnected from chat server!")

        /**
         * Handle logon to web socket with minecraft account
         */
        override fun onLogon() = displayChatMessage("§7[§a§lChat§7] §9Logging in...")

        /**
         * Handle incoming packets
         */
        override fun onPacket(packet: Packet) {
            when (packet) {
                is ClientMessagePacket -> {
                    val player = mc.player

                    if (player == null) {
                        LOGGER.info("[LiquidChat] ${packet.user.name}: ${packet.content}")
                        return
                    }

                    val chatComponent = ChatComponentText("§7[§a§lChat§7] §9${packet.user.name}: ")
                    val messageComponent = toChatComponent(packet.content)
                    chatComponent.appendSibling(messageComponent)

                    player.addChatMessage(chatComponent)
                }
                is ClientPrivateMessagePacket -> displayChatMessage("§7[§a§lChat§7] §c(P)§9 ${packet.user.name}: §7${packet.content}")
                is ClientErrorPacket -> {
                    val message = when (packet.message) {
                        "NotSupported" -> "This method is not supported!"
                        "LoginFailed" -> "Login Failed!"
                        "NotLoggedIn" -> "You must be logged in to use the chat! Enable LiquidChat."
                        "AlreadyLoggedIn" -> "You are already logged in!"
                        "MojangRequestMissing" -> "Mojang request missing!"
                        "NotPermitted" -> "You are missing the required permissions!"
                        "NotBanned" -> "You are not banned!"
                        "Banned" -> "You are banned!"
                        "RateLimited" -> "You have been rate limited. Please try again later."
                        "PrivateMessageNotAccepted" -> "Private message not accepted!"
                        "EmptyMessage" -> "You are trying to send an empty message!"
                        "MessageTooLong" -> "Message is too long!"
                        "InvalidCharacter" -> "Message contains a non-ASCII character!"
                        "InvalidId" -> "The given ID is invalid!"
                        "Internal" -> "An internal server error occurred!"
                        else -> packet.message
                    }

                    displayChatMessage("§7[§a§lChat§7] §cError: §7$message")
                }
                is ClientSuccessPacket -> {
                    when (packet.reason) {
                        "Login" -> {
                            displayChatMessage("§7[§a§lChat§7] §9Logged in!")

                            displayChatMessage("====================================")
                            displayChatMessage("§c>> §lLiquidChat")
                            displayChatMessage("§7Write message: §a.chat <message>")
                            displayChatMessage("§7Write private message: §a.pchat <user> <message>")
                            displayChatMessage("====================================")

                            loggedIn = true
                        }
                        "Ban" -> displayChatMessage("§7[§a§lChat§7] §9Successfully banned user!")
                        "Unban" -> displayChatMessage("§7[§a§lChat§7] §9Successfully unbanned user!")
                    }
                }
                is ClientNewJWTPacket -> {
                    jwtToken = packet.token
                    jwt = true

                    state = false
                    state = true
                }
            }
        }

        /**
         * Handle error
         */
        override fun onError(cause: Throwable) = displayChatMessage("§7[§a§lChat§7] §c§lError: §7${cause.javaClass.name}: ${cause.message}")
    }

    private var loggedIn = false

    private var loginThread: Thread? = null

    private val connectTimer = MSTimer()

    override fun onDisable() {
        loggedIn = false
        client.disconnect()
    }

    @EventTarget
    fun onSession(sessionEvent: SessionEvent) {
        client.disconnect()
        connect()
    }

    @EventTarget
    fun onUpdate(updateEvent: UpdateEvent) {
        if (client.isConnected() || (loginThread?.isAlive == true)) return

        if (connectTimer.hasTimePassed(5000)) {
            connect()
            connectTimer.reset()
        }
    }

    private fun connect() {
        if (client.isConnected() || (loginThread?.isAlive == true)) return

        if (jwt && jwtToken.isEmpty()) {
            displayChatMessage("§7[§a§lChat§7] §cError: §7No token provided!")
            state = false
            return
        }

        loggedIn = false

        loginThread = thread {
            try {
                client.connect()

                if (jwt)
                    client.loginJWT(jwtToken)
                else if (UserUtils.isValidTokenOffline(mc.session.token)) {
                    client.loginMojang()
                }
            } catch (cause: Exception) {
                LOGGER.error("LiquidChat error", cause)
                displayChatMessage("§7[§a§lChat§7] §cError: §7${cause.javaClass.name}: ${cause.message}")
            }

            loginThread = null
        }
    }

    /**
     * Forge Hooks
     *
     * @author Forge
     */

    private val urlPattern = Pattern.compile("((?:[a-z0-9]{2,}:\\/\\/)?(?:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|(?:[-\\w_\\.]{1,}\\.[a-z]{2,}?))(?::[0-9]{1,5})?.*?(?=[!\"\u00A7 \n]|$))", Pattern.CASE_INSENSITIVE)

    private fun toChatComponent(string: String): IChatComponent {
        var component: IChatComponent? = null
        val matcher = urlPattern.matcher(string)
        var lastEnd = 0

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            // Append the previous leftovers.
            val part = string.substring(lastEnd, start)
            if (part.isNotEmpty()) {
                if (component == null) {
                    component = ChatComponentText(part)
                    component.chatStyle.color = EnumChatFormatting.GRAY
                } else
                    component.appendText(part)
            }

            lastEnd = end

            val url = string.substring(start, end)

            try {
                if (URI(url).scheme != null) {
                    // Set the click event and append the link.
                    val link: IChatComponent = ChatComponentText(url)

                    link.chatStyle.chatClickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, url)
                    link.chatStyle.underlined = true
                    link.chatStyle.color = EnumChatFormatting.GRAY

                    if (component == null)
                        component = link
                    else
                        component.appendSibling(link)
                    continue
                }
            } catch (_: URISyntaxException) { }

            if (component == null) {
                component = ChatComponentText(url)
                component.chatStyle.color = EnumChatFormatting.GRAY
            } else
                component.appendText(url)
        }

        // Append the rest of the message.
        val end = string.substring(lastEnd)

        if (component == null) {
            component = ChatComponentText(end)
            component.chatStyle.color = EnumChatFormatting.GRAY
        } else if (end.isNotEmpty())
            component.appendText(string.substring(lastEnd))

        return component
    }

}