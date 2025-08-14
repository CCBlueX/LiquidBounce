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
import net.ccbluex.liquidbounce.config.gson.accessibleInteropGson
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.features.chat.packet.User
import net.ccbluex.liquidbounce.features.misc.proxy.Proxy
import net.ccbluex.liquidbounce.integration.VirtualScreenType
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
class ClickGuiScaleChangeEvent(val value: Float) : Event(), WebSocketEvent

@Nameable("clickGuiValueChange")
class ClickGuiValueChangeEvent(val configurable: Configurable) : Event(), WebSocketEvent

@Nameable("spaceSeperatedNamesChange")
class SpaceSeperatedNamesChangeEvent(val value: Boolean) : Event(), WebSocketEvent

@Nameable("clientStart")
object ClientStartEvent : Event()

@Nameable("clientShutdown")
object ClientShutdownEvent : Event()

@Nameable("clientLanguageChanged")
class ClientLanguageChangedEvent : Event(), WebSocketEvent

@Nameable("valueChanged")
class ValueChangedEvent(val value: Value<*>) : Event(), WebSocketEvent

@Nameable("moduleActivation")
class ModuleActivationEvent(val moduleName: String) : Event(), WebSocketEvent

@Nameable("moduleToggle")
class ModuleToggleEvent(val moduleName: String, val hidden: Boolean, val enabled: Boolean) : Event(), WebSocketEvent

@Nameable("refreshArrayList")
object RefreshArrayListEvent : Event()

@Nameable("notification")
class NotificationEvent(val title: String, val message: String, val severity: Severity) : Event(), WebSocketEvent {
    enum class Severity {
        INFO, SUCCESS, ERROR, ENABLED, DISABLED
    }
}

@Nameable("gameModeChange")
class GameModeChangeEvent(val gameMode: GameMode) : Event(), WebSocketEvent

@Nameable("targetChange")
class TargetChangeEvent(val target: PlayerData?) : Event(), WebSocketEvent

@Nameable("blockCountChange")
class BlockCountChangeEvent(val count: Int?) : Event(), WebSocketEvent

@Nameable("clientChatStateChange")
class ClientChatStateChange(val state: State) : Event(), WebSocketEvent {
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
class ClientChatMessageEvent(val user: User, val message: String, val chatGroup: ChatGroup) : Event(), WebSocketEvent {
    enum class ChatGroup(override val choiceName: String) : NamedChoice {
        @SerializedName("public")
        PUBLIC_CHAT("PublicChat"),

        @SerializedName("private")
        PRIVATE_CHAT("PrivateChat"),
    }
}

@Nameable("clientChatError")
class ClientChatErrorEvent(val error: String) : Event(), WebSocketEvent

@Nameable("clientChatJwtToken")
// Do not define as WebSocket event, because it contains sensitive data
class ClientChatJwtTokenEvent(val jwt: String) : Event()

@Nameable("accountManagerMessage")
class AccountManagerMessageEvent(val message: String) : Event(), WebSocketEvent

@Nameable("accountManagerLogin")
class AccountManagerLoginResultEvent(val username: String? = null, val error: String? = null) : Event(), WebSocketEvent

@Nameable("accountManagerAddition")
class AccountManagerAdditionResultEvent(
    val username: String? = null, val error: String? = null
) : Event(), WebSocketEvent

@Nameable("accountManagerRemoval")
class AccountManagerRemovalResultEvent(val username: String?) : Event(), WebSocketEvent

@Nameable("proxyAdditionResult")
class ProxyAdditionResultEvent(val proxy: Proxy? = null, val error: String? = null) : Event(), WebSocketEvent

@Nameable("proxyCheckResult")
class ProxyCheckResultEvent(val proxy: Proxy, val error: String? = null) : Event(), WebSocketEvent

@Nameable("proxyEditResult")
class ProxyEditResultEvent(val proxy: Proxy? = null, val error: String? = null) : Event(), WebSocketEvent

@Nameable("browserReady")
object BrowserReadyEvent : Event()

@Nameable("virtualScreen")
class VirtualScreenEvent(
    val type: VirtualScreenType,
    @Deprecated("Use `type` instead") val screenName: String = type.routeName,
    val action: Action
) : Event(), WebSocketEvent {

    enum class Action {
        @SerializedName("open")
        OPEN,

        @SerializedName("close")
        CLOSE
    }

}

@Nameable("serverPinged")
class ServerPingedEvent(val server: ServerInfo) : Event(), WebSocketEvent

@Nameable("componentsUpdate")
class ComponentsUpdate(val components: List<Component>) : Event(), WebSocketEvent {
    override val serializer get() = accessibleInteropGson
}

@Nameable("rotationUpdate")
object RotationUpdateEvent : Event()

@Nameable("resourceReload")
object ResourceReloadEvent : Event()

@Nameable("scaleFactorChange")
class ScaleFactorChangeEvent(val scaleFactor: Double) : Event(), WebSocketEvent

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
class BrowserUrlChangeEvent(val index: Int, val url: String) : Event(), WebSocketEvent
