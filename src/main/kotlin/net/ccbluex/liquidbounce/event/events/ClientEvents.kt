/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.features.chat.packet.User
import net.ccbluex.liquidbounce.features.misc.ProxyManager
import net.ccbluex.liquidbounce.utils.client.Nameable
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.InventoryActionChain
import net.ccbluex.liquidbounce.utils.inventory.InventoryConstraints
import net.ccbluex.liquidbounce.integration.browser.supports.IBrowser
import net.ccbluex.liquidbounce.integration.interop.protocol.event.WebSocketEvent
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerData
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.minecraft.client.network.ServerInfo
import net.minecraft.world.GameMode

@Nameable("clickGuiScaleChange")
@WebSocketEvent
class ClickGuiScaleChangeEvent(val value: Float): Event()

@Nameable("spaceSeperatedNamesChange")
@WebSocketEvent
class SpaceSeperatedNamesChangeEvent(val value: Boolean) : Event()

@Nameable("clientStart")
class ClientStartEvent : Event()

@Nameable("clientShutdown")
class ClientShutdownEvent : Event()

@Nameable("valueChanged")
class ValueChangedEvent(val value: Value<*>) : Event()

@Nameable("toggleModule")
@WebSocketEvent
class ToggleModuleEvent(val moduleName: String, val hidden: Boolean, val enabled: Boolean) : Event()

@Nameable("refreshArrayList")
@WebSocketEvent
class RefreshArrayListEvent : Event()

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
class ProxyAdditionResultEvent(val proxy: ProxyManager.Proxy? = null, val error: String? = null) : Event()

@Nameable("proxyCheckResult")
@WebSocketEvent
class ProxyCheckResultEvent(val proxy: ProxyManager.Proxy, val error: String? = null) : Event()

@Nameable("proxyEditResult")
@WebSocketEvent
class ProxyEditResultEvent(val proxy: ProxyManager.Proxy? = null, val error: String? = null) : Event()

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
@WebSocketEvent
class ComponentsUpdate(val components: List<Component>) : Event()

/**
 * The simulated tick event is called by the [MovementInputEvent] with a simulated movement context.
 * This context includes a simulated player position one tick into the future.
 * Position changes will not apply within the simulated tick. Only use this for prediction purposes as
 * updating the rotation or target.
 */
@Nameable("simulatedTick")
class SimulatedTickEvent(val movementEvent: MovementInputEvent, val simulatedPlayer: SimulatedPlayer) : Event()

@Nameable("resourceReload")
class ResourceReloadEvent : Event()

@Nameable("scaleFactorChange")
@WebSocketEvent
class ScaleFactorChangeEvent(val scaleFactor: Double) : Event()

@Nameable("scheduleInventoryAction")
class ScheduleInventoryActionEvent(
    val schedule: MutableList<InventoryActionChain> = mutableListOf()
) : Event() {

    fun schedule(constrains: InventoryConstraints, action: InventoryAction) =
        schedule.add(InventoryActionChain(constrains, arrayOf(action)))
    fun schedule(constrains: InventoryConstraints, vararg actions: InventoryAction) =
        this.schedule.add(InventoryActionChain(constrains, actions))
    fun schedule(constrains: InventoryConstraints, actions: List<InventoryAction>) =
        this.schedule.add(InventoryActionChain(constrains, actions.toTypedArray()))

}

@Nameable("browserUrlChange")
@WebSocketEvent
class BrowserUrlChangeEvent(val index: Int, val url: String) : Event()
