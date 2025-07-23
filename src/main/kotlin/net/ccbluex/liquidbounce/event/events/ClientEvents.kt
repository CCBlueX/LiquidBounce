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
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.features.chat.packet.User
import net.ccbluex.liquidbounce.features.misc.proxy.Proxy
import net.ccbluex.liquidbounce.integration.interop.protocol.event.WebSocketEvent
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerData
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.annotations.InbuiltEvent
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
@InbuiltEvent("clickGuiScaleChange")
@WebSocketEvent
class ClickGuiScaleChangeEvent(val value: Float) : Event()

@InbuiltEvent("clickGuiValueChange")
@WebSocketEvent
class ClickGuiValueChangeEvent(val configurable: Configurable) : Event()

@InbuiltEvent("spaceSeperatedNamesChange")
@WebSocketEvent
class SpaceSeperatedNamesChangeEvent(val value: Boolean) : Event()

@InbuiltEvent("clientStart")
object ClientStartEvent : Event()

@InbuiltEvent("clientShutdown")
object ClientShutdownEvent : Event()

@InbuiltEvent("clientLanguageChanged")
@WebSocketEvent
class ClientLanguageChangedEvent : Event()

@InbuiltEvent("valueChanged")
@WebSocketEvent
class ValueChangedEvent(val value: Value<*>) : Event()

@InbuiltEvent("moduleActivation")
@WebSocketEvent
class ModuleActivationEvent(val moduleName: String) : Event()

@InbuiltEvent("moduleToggle")
@WebSocketEvent
class ModuleToggleEvent(val moduleName: String, val hidden: Boolean, val enabled: Boolean) : Event()

@InbuiltEvent("refreshArrayList")
@WebSocketEvent
object RefreshArrayListEvent : Event()

@InbuiltEvent("notification")
@WebSocketEvent
class NotificationEvent(val title: String, val message: String, val severity: Severity) : Event() {
    enum class Severity {
        INFO, SUCCESS, ERROR, ENABLED, DISABLED
    }
}

@InbuiltEvent("gameModeChange")
@WebSocketEvent
class GameModeChangeEvent(val gameMode: GameMode) : Event()

@InbuiltEvent("targetChange")
@WebSocketEvent
class TargetChangeEvent(val target: PlayerData?) : Event()

@InbuiltEvent("blockCountChange")
@WebSocketEvent
class BlockCountChangeEvent(val count: Int?) : Event()

@InbuiltEvent("clientChatStateChange")
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

@InbuiltEvent("clientChatMessage")
@WebSocketEvent
class ClientChatMessageEvent(val user: User, val message: String, val chatGroup: ChatGroup) : Event() {
    enum class ChatGroup(override val choiceName: String) : NamedChoice {
        @SerializedName("public")
        PUBLIC_CHAT("PublicChat"),

        @SerializedName("private")
        PRIVATE_CHAT("PrivateChat"),
    }
}

@InbuiltEvent("clientChatError")
@WebSocketEvent
class ClientChatErrorEvent(val error: String) : Event()

@InbuiltEvent("clientChatJwtToken")
// Do not define as WebSocket event, because it contains sensitive data
class ClientChatJwtTokenEvent(val jwt: String) : Event()

@InbuiltEvent("accountManagerMessage")
@WebSocketEvent
class AccountManagerMessageEvent(val message: String) : Event()

@InbuiltEvent("accountManagerLogin")
@WebSocketEvent
class AccountManagerLoginResultEvent(val username: String? = null, val error: String? = null) : Event()

@InbuiltEvent("accountManagerAddition")
@WebSocketEvent
class AccountManagerAdditionResultEvent(val username: String? = null, val error: String? = null) : Event()

@InbuiltEvent("accountManagerRemoval")
@WebSocketEvent
class AccountManagerRemovalResultEvent(val username: String?) : Event()

@InbuiltEvent("proxyAdditionResult")
@WebSocketEvent
class ProxyAdditionResultEvent(val proxy: Proxy? = null, val error: String? = null) : Event()

@InbuiltEvent("proxyCheckResult")
@WebSocketEvent
class ProxyCheckResultEvent(val proxy: Proxy, val error: String? = null) : Event()

@InbuiltEvent("proxyEditResult")
@WebSocketEvent
class ProxyEditResultEvent(val proxy: Proxy? = null, val error: String? = null) : Event()

@InbuiltEvent("browserReady")
object BrowserReadyEvent : Event()

@InbuiltEvent("virtualScreen")
@WebSocketEvent
class VirtualScreenEvent(val screenName: String, val action: Action) : Event() {

    enum class Action {
        @SerializedName("open")
        OPEN,

        @SerializedName("close")
        CLOSE
    }

}

@InbuiltEvent("serverPinged")
@WebSocketEvent
class ServerPingedEvent(val server: ServerInfo) : Event()

@InbuiltEvent("componentsUpdate")
@WebSocketEvent(serializer = GsonInstance.ACCESSIBLE_INTEROP)
class ComponentsUpdate(val components: List<Component>) : Event()

@InbuiltEvent("rotationUpdate")
object RotationUpdateEvent : Event()

@InbuiltEvent("resourceReload")
object ResourceReloadEvent : Event()

@InbuiltEvent("scaleFactorChange")
@WebSocketEvent
class ScaleFactorChangeEvent(val scaleFactor: Double) : Event()

@InbuiltEvent("scheduleInventoryAction")
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

@InbuiltEvent("selectHotbarSlotSilently")
class SelectHotbarSlotSilentlyEvent(val requester: Any?, val slot: Int): CancellableEvent()

@InbuiltEvent("browserUrlChange")
@WebSocketEvent
class BrowserUrlChangeEvent(val index: Int, val url: String) : Event()
