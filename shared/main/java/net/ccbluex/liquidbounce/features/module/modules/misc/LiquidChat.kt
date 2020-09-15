/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.event.IClickEvent
import net.ccbluex.liquidbounce.api.minecraft.util.IIChatComponent
import net.ccbluex.liquidbounce.api.minecraft.util.WEnumChatFormatting
import net.ccbluex.liquidbounce.chat.Client
import net.ccbluex.liquidbounce.chat.packet.packets.*
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.login.UserUtils
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.net.URISyntaxException
import java.util.regex.Pattern
import kotlin.concurrent.thread

@ModuleInfo(name = "LiquidChat", description = "Allows you to chat with other LiquidBounce users.", category = ModuleCategory.MISC)
class LiquidChat : Module() {

    init {
        state = true
        array = false
    }

    val jwtValue = object : BoolValue("JWT", false) {
        override fun onChanged(oldValue: Boolean, newValue: Boolean) {
            if (state) {
                state = false
                state = true
            }
        }
    }

    companion object {
        var jwtToken = ""
    }

    val client = object : Client() {

        /**
         * Handle connect to web socket
         */
        override fun onConnect() {
            ClientUtils.displayChatMessage("§7[§a§lChat§7] §9Connecting to chat server...")
        }

        /**
         * Handle connect to web socket
         */
        override fun onConnected() {
            ClientUtils.displayChatMessage("§7[§a§lChat§7] §9Connected to chat server!")
        }

        /**
         * Handle handshake
         */
        override fun onHandshake(success: Boolean) {}

        /**
         * Handle disconnect
         */
        override fun onDisconnect() {
            ClientUtils.displayChatMessage("§7[§a§lChat§7] §cDisconnected from chat server!")
        }

        /**
         * Handle logon to web socket with minecraft account
         */
        override fun onLogon() {
            ClientUtils.displayChatMessage("§7[§a§lChat§7] §9Logging in...")
        }

        /**
         * Handle incoming packets
         */
        override fun onPacket(packet: Packet) {
            when (packet) {
                is ClientMessagePacket -> {
                    val thePlayer = mc.thePlayer

                    if (thePlayer == null) {
                        ClientUtils.getLogger().info("[LiquidChat] ${packet.user.name}: ${packet.content}")
                        return
                    }

                    val chatComponent = classProvider.createChatComponentText("§7[§a§lChat§7] §9${packet.user.name}: ")
                    val messageComponent = toChatComponent(packet.content)
                    chatComponent.appendSibling(messageComponent)

                    thePlayer.addChatMessage(chatComponent)
                }
                is ClientPrivateMessagePacket -> ClientUtils.displayChatMessage("§7[§a§lChat§7] §c(P)§9 ${packet.user.name}: §7${packet.content}")
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

                    ClientUtils.displayChatMessage("§7[§a§lChat§7] §cError: §7$message")
                }
                is ClientSuccessPacket -> {
                    when (packet.reason) {
                        "Login" -> {
                            ClientUtils.displayChatMessage("§7[§a§lChat§7] §9Logged in!")

                            ClientUtils.displayChatMessage("====================================")
                            ClientUtils.displayChatMessage("§c>> §lLiquidChat")
                            ClientUtils.displayChatMessage("§7Write message: §a.chat <message>")
                            ClientUtils.displayChatMessage("§7Write private message: §a.pchat <user> <message>")
                            ClientUtils.displayChatMessage("====================================")

                            loggedIn = true
                        }
                        "Ban" -> ClientUtils.displayChatMessage("§7[§a§lChat§7] §9Successfully banned user!")
                        "Unban" -> ClientUtils.displayChatMessage("§7[§a§lChat§7] §9Successfully unbanned user!")
                    }
                }
                is ClientNewJWTPacket -> {
                    jwtToken = packet.token
                    jwtValue.set(true)

                    state = false
                    state = true
                }
            }
        }

        /**
         * Handle error
         */
        override fun onError(cause: Throwable) {
            ClientUtils.displayChatMessage("§7[§a§lChat§7] §c§lError: §7${cause.javaClass.name}: ${cause.message}")
        }
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
        if (client.isConnected() || (loginThread != null && loginThread!!.isAlive)) return

        if (connectTimer.hasTimePassed(5000)) {
            connect()
            connectTimer.reset()
        }
    }

    private fun connect() {
        if (client.isConnected() || (loginThread != null && loginThread!!.isAlive)) return

        if (jwtValue.get() && jwtToken.isEmpty()) {
            ClientUtils.displayChatMessage("§7[§a§lChat§7] §cError: §7No token provided!")
            state = false
            return
        }

        loggedIn = false

        loginThread = thread {
            try {
                client.connect()

                if (jwtValue.get())
                    client.loginJWT(jwtToken)
                else if (UserUtils.isValidToken(mc.session.token))
                    client.loginMojang()
            } catch (cause: Exception) {
                ClientUtils.getLogger().error("LiquidChat error", cause)
                ClientUtils.displayChatMessage("§7[§a§lChat§7] §cError: §7${cause.javaClass.name}: ${cause.message}")
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

    private fun toChatComponent(string: String): IIChatComponent {
        var component: IIChatComponent? = null
        val matcher = urlPattern.matcher(string)
        var lastEnd = 0

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            // Append the previous left overs.
            val part = string.substring(lastEnd, start)
            if (part.isNotEmpty()) {
                if (component == null) {
                    component = classProvider.createChatComponentText(part)
                    component.chatStyle.color = WEnumChatFormatting.GRAY
                } else
                    component.appendText(part)
            }

            lastEnd = end

            val url = string.substring(start, end)

            try {
                if (URI(url).scheme != null) {
                    // Set the click event and append the link.
                    val link: IIChatComponent = classProvider.createChatComponentText(url)

                    link.chatStyle.chatClickEvent = classProvider.createClickEvent(IClickEvent.WAction.OPEN_URL, url)
                    link.chatStyle.underlined = true
                    link.chatStyle.color = WEnumChatFormatting.GRAY

                    if (component == null)
                        component = link
                    else
                        component.appendSibling(link)
                    continue
                }
            } catch (e: URISyntaxException) {
            }

            if (component == null) {
                component = classProvider.createChatComponentText(url)
                component.chatStyle.color = WEnumChatFormatting.GRAY
            } else
                component.appendText(url)
        }

        // Append the rest of the message.
        val end = string.substring(lastEnd)

        if (component == null) {
            component = classProvider.createChatComponentText(end)
            component.chatStyle.color = WEnumChatFormatting.GRAY
        } else if (end.isNotEmpty())
            component.appendText(string.substring(lastEnd))

        return component
    }

}