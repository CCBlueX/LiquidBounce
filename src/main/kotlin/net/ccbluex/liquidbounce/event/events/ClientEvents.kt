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
 */

package net.ccbluex.liquidbounce.event.events

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.config.gson.GsonInstance
import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.features.chat.packet.User
import net.ccbluex.liquidbounce.features.misc.proxy.Proxy
import net.ccbluex.liquidbounce.integration.browser.supports.IBrowser
import net.ccbluex.liquidbounce.integration.interop.protocol.event.WebSocketEvent
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerData
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.utils.client.Nameable
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.InventoryActionChain
import net.ccbluex.liquidbounce.utils.inventory.InventoryConstraints
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.network.ServerInfo
import net.minecraft.world.GameMode

@Deprecated(
    "The `clickGuiScaleChange` event has been deprecated.",
    ReplaceWith("ClickGuiScaleChangeEvent"),
    DeprecationLevel.WARNING
)
@Nameable("clickGuiScaleChange")
@WebSocketEvent
class ClickGuiScaleChangeEvent(val value: Float) : Event()

@Nameable("clickGuiValueChange")
@WebSocketEvent
class ClickGuiValueChangeEvent(val configurable: Configurable) : Event()

@Nameable("spaceSeperatedNamesChange")
@WebSocketEvent
class SpaceSeperatedNamesChangeEvent(val value: Boolean) : Event()

@Nameable("clientStart")
object ClientStartEvent : Event()

@Nameable("clientShutdown")
object ClientShutdownEvent : Event()

@Nameable("clientLanguageChanged")
@WebSocketEvent
class ClientLanguageChangedEvent : Event()

@Nameable("valueChanged")
@WebSocketEvent
class ValueChangedEvent(val value: Value<*>) : Event()

@Nameable("moduleActivation")
@WebSocketEvent
class ModuleActivationEvent(val moduleName: String) : Event()

@Nameable("moduleToggle")
@WebSocketEvent
class ModuleToggleEvent(val moduleName: String, val hidden: Boolean, val enabled: Boolean) : Event()

@Nameable("refreshArrayList")
@WebSocketEvent
object RefreshArrayListEvent : Event()

@Nameable("notification")
@WebSocketEvent
class NotificationEvent(val title: String, val message: String, val severity: Severity) : Event() {
    enum class Severity {
        INFO, SUCCESS, ERROR, ENABLED, DISABLED
    }
}

@Nameable("gameModeChange")
@WebSocketEvent
class GameModeChangeEvent(val gameMode: GameMode) : Event()

@Nameable("targetChange")
@WebSocketEvent
class TargetChangeEvent(val target: PlayerData?) : Event()

@Nameable("blockCountChange")
@WebSocketEvent
class BlockCountChangeEvent(val count: Int?) : Event()

@Nameable("clientChatStateChange")
@WebSocketEvent
class ClientChatStateChange(val state: State) : Event() {
    enum class State {
        @SerializedName("connecting")
        CONNECTING,

        @SerializedName("connected")
        CONNECTED,

        @SerializedName("logon")
        LOGGING_IN,

        @SerializedName("loggedIn")
        LOGGED_IN,

        @SerializedName("disconnected")
        DISCONNECTED,

        @SerializedName("authenticationFailed")
        AUTHENTICATION_FAILED,
    }
}

@Nameable("clientChatMessage")
@WebSocketEvent
class ClientChatMessageEvent(val user: User, val message: String, val chatGroup: ChatGroup) : Event() {
    enum class ChatGroup {
        @SerializedName("public")
        PUBLIC_CHAT,

        @SerializedName("private")
        PRIVATE_CHAT
    }
}

@Nameable("clientChatError")
@WebSocketEvent
class ClientChatErrorEvent(val error: String) : Event()

@Nameable("clientChatJwtToken")
// Do not define as WebSocket event, because it contains sensitive data
class ClientChatJwtTokenEvent(val jwt: String) : Event()

@Nameable("accountManagerMessage")
@WebSocketEvent
class AccountManagerMessageEvent(val message: String) : Event()

@Nameable("accountManagerLogin")
@WebSocketEvent
class AccountManagerLoginResultEvent(val username: String? = null, val error: String? = null) : Event()

@Nameable("accountManagerAddition")
@WebSocketEvent
class AccountManagerAdditionResultEvent(val username: String? = null, val error: String? = null) : Event()

@Nameable("proxyAdditionResult")
@WebSocketEvent
class ProxyAdditionResultEvent(val proxy: Proxy? = null, val error: String? = null) : Event()

@Nameable("proxyCheckResult")
@WebSocketEvent
class ProxyCheckResultEvent(val proxy: Proxy, val error: String? = null) : Event()

@Nameable("proxyEditResult")
@WebSocketEvent
class ProxyEditResultEvent(val proxy: Proxy? = null, val error: String? = null) : Event()

@Nameable("browserReady")
class BrowserReadyEvent(val browser: IBrowser) : Event()

@Nameable("virtualScreen")
@WebSocketEvent
class VirtualScreenEvent(val screenName: String, val action: Action) : Event() {

    enum class Action {
        @SerializedName("open")
        OPEN,

        @SerializedName("close")
        CLOSE
    }

}

@Nameable("serverPinged")
@WebSocketEvent
class ServerPingedEvent(val server: ServerInfo) : Event()

@Nameable("componentsUpdate")
@WebSocketEvent(serializer = GsonInstance.ACCESSIBLE_INTEROP)
class ComponentsUpdate(val components: List<Component>) : Event()

@Nameable("rotationUpdate")
object RotationUpdateEvent : Event()

@Nameable("resourceReload")
object ResourceReloadEvent : Event()

@Nameable("scaleFactorChange")
@WebSocketEvent
class ScaleFactorChangeEvent(val scaleFactor: Double) : Event()

@Nameable("scheduleInventoryAction")
class ScheduleInventoryActionEvent(val schedule: MutableList<InventoryActionChain> = mutableListOf()) : Event() {

    fun schedule(
        constrains: InventoryConstraints,
        action: InventoryAction,
        priority: Priority = Priority.NORMAL
    ) {
        schedule.add(InventoryActionChain(constrains, arrayOf(action), priority))
    }

    fun schedule(
        constrains: InventoryConstraints,
        vararg actions: InventoryAction,
        priority: Priority = Priority.NORMAL
    ) {
        this.schedule.add(InventoryActionChain(constrains, actions, priority))
    }

    fun schedule(
        constrains: InventoryConstraints,
        actions: List<InventoryAction>,
        priority: Priority = Priority.NORMAL
    ) {
        this.schedule.add(InventoryActionChain(constrains, actions.toTypedArray(), priority))
    }
}

@Nameable("selectHotbarSlotSilently")
class SelectHotbarSlotSilentlyEvent(val requester: Any?, val slot: Int): CancellableEvent()

@Nameable("browserUrlChange")
@WebSocketEvent
class BrowserUrlChangeEvent(val index: Int, val url: String) : Event()
