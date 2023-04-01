package net.ccbluex.liquidbounce.features.module.modules.misc

import me.liuli.elixir.account.CrackedAccount
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.file.FileManager.accountsConfig
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.ServerUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.randomAccount
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.ccbluex.liquidbounce.value.TextValue
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S40PacketDisconnect
import net.minecraft.util.ChatComponentText
import net.minecraft.util.Session
import java.util.*
import kotlin.concurrent.schedule

@ModuleInfo("AutoAccount", "Most feature-packed auto register/login & account manager.", ModuleCategory.MISC)
object AutoAccount : Module() {

    private val registerValue = BoolValue("AutoRegister", true)

    private val loginValue = BoolValue("AutoLogin", true)

    // Gamster requires 8 chars+
    private val passwordValue = object : TextValue("Password", "axolotlaxolotl") {
        override fun changeValue(newValue: String) {
            when {
                newValue.equals("reset", true) -> {
                    super.changeValue("axolotlaxolotl")
                    displayChatMessage("§7[§a§lAutoAccount§7] §3Password reset to its default value.")
                }
                newValue.length < 4 -> displayChatMessage("§7[§a§lAutoAccount§7] §cPassword must be longer than 4 characters!")
                else -> super.changeValue(newValue)
            }
        }

        override fun isSupported() = registerValue.get() || loginValue.get()
    }

    // Needed for Gamster
    private val sendDelayValue = object : IntegerValue("SendDelay", 250, 0, 500) {
        override fun isSupported() = passwordValue.isSupported()
    }

    private val autoSessionValue = BoolValue("AutoSession", false)

    private val startupValue = object : BoolValue("RandomAccountOnStart", false) {
        override fun isSupported() = autoSessionValue.get()
    }

    private val relogInvalidValue = object : BoolValue("RelogWhenPasswordInvalid", true) {
        override fun isSupported() = autoSessionValue.get()
    }

    private val relogKickedValue = object : BoolValue("RelogWhenKicked", false) {
        override fun isSupported() = autoSessionValue.get()
    }

    private val reconnectDelayValue = object : IntegerValue("ReconnectDelay", 1000, 0, 2500) {
        override fun isSupported() = relogInvalidValue.isActive() || relogKickedValue.isActive()
    }

    private val accountModeValue = object : ListValue("AccountMode", arrayOf("RandomName", "RandomAlt"), "RandomName") {
        override fun isSupported() = reconnectDelayValue.isSupported() || startupValue.isActive()

        override fun changeValue(newValue: String) {
            if (newValue == "RandomAlt" && accountsConfig.accounts.filterIsInstance<CrackedAccount>().size <= 1)
                displayChatMessage("§7[§a§lAutoAccount§7] §cAdd more cracked accounts in AltManager to use RandomAlt option!")
            else super.changeValue(newValue)
        }
    }

    private val saveValue = object : BoolValue("SaveToAlts", false) {
        override fun isSupported() = accountModeValue.isSupported() && accountModeValue.get() != "RandomAlt"
    }

    private var status = Status.WAITING

    private fun relog(info: String = "") {
        // Disconnect from server
        if (mc.currentServerData != null && mc.theWorld != null)
             mc.netHandler.networkManager.closeChannel(
                 ChatComponentText("$info\n\nReconnecting with a random account in ${reconnectDelayValue.get()}ms")
             )

        // Log in to account with a random name, optionally save it
        changeAccount()

        // Reconnect normally with OpenGL context
        if (reconnectDelayValue.isMinimal()) return ServerUtils.connectToLastServer()

        // Delay the reconnect, connectToLastServer gets called from a TimerThread with no OpenGL context
        Timer().schedule(reconnectDelayValue.get().toLong()) {
            ServerUtils.connectToLastServer(true)
        }
    }

    private fun respond(msg: String?): Boolean {
        if (msg == null) return false

        when {
            registerValue.get() && "/reg" in msg -> {
                hud.addNotification(Notification("Trying to register."))
                Timer().schedule(sendDelayValue.get().toLong()) {
                    mc.thePlayer.sendChatMessage("/register ${passwordValue.get()} ${passwordValue.get()}")
                }
            }
            loginValue.get() && "/log" in msg -> {
                hud.addNotification(Notification("Trying to log in."))
                Timer().schedule(sendDelayValue.get().toLong()) {
                    mc.thePlayer.sendChatMessage("/login ${passwordValue.get()}")
                }
            }
            else -> return false
        }
        return true
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        when (packet) {
            is S02PacketChat -> {
                // Don't respond to register / login prompts when failed once
                if (!passwordValue.isSupported() || status == Status.STOPPED) return

                val msg = packet.chatComponent?.unformattedText?.lowercase() ?: return

                if (status == Status.WAITING) {
                    // Try to register / log in, return if invalid message
                    if (!respond(msg)) return

                    event.cancelEvent()
                    status = Status.SENT_COMMAND
                } else {
                    // Check response from server
                    when {
                        // Logged in
                        "success" in msg || "logged" in msg || "registered" in msg -> {
                            success()
                            event.cancelEvent()
                        }
                        // Login failed, possibly relog
                        "incorrect" in msg || "wrong" in msg || "spatne" in msg -> fail()
                        "unknown" in msg || "command" in msg || "allow" in msg || "already" in msg -> {
                            // Tried executing /login or /register from lobby, stop trying
                            status = Status.STOPPED
                            event.cancelEvent()
                        }
                    }
                }
            }
            is S40PacketDisconnect -> {
                if (relogKickedValue.isActive() && status != Status.SENT_COMMAND) {
                    val reason = packet.reason.unformattedText
                    if ("ban" in reason) return

                    relog(packet.reason.unformattedText)
                }
            }
        }

    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        if (!passwordValue.isSupported()) return

        // Reset status if player wasn't in a world before
        if (mc.theWorld == null) {
            status = Status.WAITING
            return
        }

        if (status == Status.SENT_COMMAND) {
            // Server redirected the player to a lobby, success
            if (event.worldClient != null && mc.theWorld != event.worldClient) success()
            // Login failed, possibly relog
            else fail()
        }
    }

    @EventTarget
    fun onStartup(startupEvent: StartupEvent) {
        // Log in to account with a random name after startup, optionally save it
        if (startupValue.isActive()) changeAccount()
    }

    // Login succeeded
    private fun success() {
        if (status == Status.SENT_COMMAND) {
            hud.addNotification(Notification("Logged in as ${mc.session.username}"))

            // Stop waiting for response
            status = Status.STOPPED
        }
    }

    // Login failed
    private fun fail() {
        if (status == Status.SENT_COMMAND) {
            hud.addNotification(Notification("Failed to log in as ${mc.session.username}"))

            // Stop waiting for response
            status = Status.STOPPED

            // Trigger relog task
            if (relogInvalidValue.isActive()) relog()
        }
    }

    private fun changeAccount() {
        if (accountModeValue.get() == "RandomAlt") {
            val account = accountsConfig.accounts.filter { it is CrackedAccount && it.name != mc.session.username }
                .randomOrNull() ?: return
            mc.session = Session(
                account.session.username, account.session.uuid,
                account.session.token, account.session.type
            )
            LiquidBounce.eventManager.callEvent(SessionEvent())
            return
        }

        // Log in to account with a random name
        val account = randomAccount()

        // Save as a new account if SaveToAlts is enabled
        if (saveValue.isActive() && !accountsConfig.accountExists(account)) {
            accountsConfig.addAccount(account)
            accountsConfig.saveConfig()

            hud.addNotification(Notification("Saved alt ${account.name}"))
        }
    }

    enum class Status {
        WAITING, SENT_COMMAND, STOPPED
    }
}