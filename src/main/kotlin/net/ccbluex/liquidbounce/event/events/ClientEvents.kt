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

package net.ccbluex.liquidbounce.event.events

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.annotations.Tag
import net.ccbluex.liquidbounce.config.gson.accessibleInteropGson
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.features.chat.packet.AxoUser
import net.ccbluex.liquidbounce.features.misc.proxy.Proxy
import net.ccbluex.liquidbounce.integration.interop.protocol.event.WebSocketEvent
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerData
import net.ccbluex.liquidbounce.integration.screen.CustomScreenType
import net.ccbluex.liquidbounce.integration.theme.component.HudComponent
import net.ccbluex.liquidbounce.utils.block.bed.BedState
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.InventoryConstraints
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.unmodifiable
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Block

@Deprecated(
    "The `clickGuiScaleChange` event has been deprecated.",
    ReplaceWith("ClickGuiScaleChangeEvent"),
    DeprecationLevel.WARNING
)
@Tag("clickGuiScaleChange")
class ClickGuiScaleChangeEvent(val value: Float) : Event(), WebSocketEvent

@Tag("clickGuiValueChange")
class ClickGuiValueChangeEvent(val valueGroup: ValueGroup) : Event(), WebSocketEvent

@Tag("spaceSeperatedNamesChange")
class SpaceSeperatedNamesChangeEvent(val value: Boolean) : Event(), WebSocketEvent

@Tag("clientStart")
object ClientStartEvent : Event()

@Tag("clientShutdown")
object ClientShutdownEvent : Event()

@Tag("clientLanguageChanged")
class ClientLanguageChangedEvent : Event(), WebSocketEvent

@Tag("valueChanged")
class ValueChangedEvent(val value: Value<*>) : Event(), WebSocketEvent

@Tag("moduleActivation")
class ModuleActivationEvent(val moduleName: String) : Event(), WebSocketEvent

@Tag("moduleToggle")
class ModuleToggleEvent(val moduleName: String, val hidden: Boolean, val enabled: Boolean) : Event(), WebSocketEvent

@Tag("refreshArrayList")
object RefreshArrayListEvent : Event(), WebSocketEvent

@Tag("notification")
class NotificationEvent(val title: String, val message: String, val severity: Severity) : Event(), WebSocketEvent {
    enum class Severity {
        INFO, SUCCESS, ERROR, ENABLED, DISABLED
    }
}

@Tag("gameModeChange")
class GameModeChangeEvent(val gameMode: GameType) : Event(), WebSocketEvent

@Tag("targetChange")
class TargetChangeEvent(val target: PlayerData?) : Event(), WebSocketEvent

@Tag("blockCountChange")
class BlockCountChangeEvent(val nextBlock: Block?, val count: Int?) : Event(), WebSocketEvent

@Tag("bedStateChange")
class BedStateChangeEvent(val bedStates: Collection<BedState>) : Event(), WebSocketEvent

@Tag("clientChatStateChange")
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

@Tag("clientChatMessage")
class ClientChatMessageEvent(
    val user: AxoUser,
    val message: String,
    val chatGroup: ChatGroup,
) : Event(), WebSocketEvent {
    enum class ChatGroup(override val tag: String) : Tagged {
        @SerializedName("public")
        PUBLIC_CHAT("PublicChat"),

        @SerializedName("private")
        PRIVATE_CHAT("PrivateChat"),
    }
}

@Tag("clientChatError")
class ClientChatErrorEvent(val error: String) : Event(), WebSocketEvent

@Tag("clientChatJwtToken")
// Do not define as WebSocket event, because it contains sensitive data
class ClientChatJwtTokenEvent(val jwt: String) : Event()

@Tag("accountManagerMessage")
class AccountManagerMessageEvent(val message: String) : Event(), WebSocketEvent

@Tag("accountManagerLogin")
class AccountManagerLoginResultEvent(val username: String? = null, val error: String? = null) : Event(), WebSocketEvent

@Tag("accountManagerAddition")
class AccountManagerAdditionResultEvent(
    val username: String? = null, val error: String? = null
) : Event(), WebSocketEvent

@Tag("accountManagerRemoval")
class AccountManagerRemovalResultEvent(val username: String?) : Event(), WebSocketEvent

@Tag("proxyCheckResult")
class ProxyCheckResultEvent(val proxy: Proxy? = null, val error: String? = null) : Event(), WebSocketEvent

@Tag("browserReady")
object BrowserReadyEvent : Event()

@Tag("virtualScreen")
class VirtualScreenEvent(
    val type: CustomScreenType,
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

@Tag("serverPinged")
class ServerPingedEvent(val server: ServerData) : Event(), WebSocketEvent

@Tag("componentsUpdate")
class ComponentsUpdateEvent(val id: String? = null, val components: List<HudComponent>) : Event(), WebSocketEvent {
    override val serializer get() = accessibleInteropGson
}

@Tag("rotationUpdate")
object RotationUpdateEvent : Event()

@Tag("resourceReload")
object ResourceReloadEvent : Event()

@Tag("scaleFactorChange")
class ScaleFactorChangeEvent(val scaleFactor: Int) : Event(), WebSocketEvent

@Tag("scheduleInventoryAction")
class ScheduleInventoryActionEvent(val schedule: MutableList<InventoryAction.Chain> = mutableListOf()) : Event() {

    fun schedule(
        constrains: InventoryConstraints,
        action: InventoryAction,
        priority: Priority = Priority.NORMAL
    ) {
        this.schedule.add(InventoryAction.Chain(constrains, listOf(action), priority))
    }

    fun schedule(
        constrains: InventoryConstraints,
        vararg actions: InventoryAction,
        priority: Priority = Priority.NORMAL
    ) {
        this.schedule.add(InventoryAction.Chain(constrains, actions.unmodifiable(), priority))
    }

    fun schedule(
        constrains: InventoryConstraints,
        actions: List<InventoryAction>,
        priority: Priority = Priority.NORMAL
    ) {
        this.schedule.add(InventoryAction.Chain(constrains, actions, priority))
    }
}

@Tag("selectHotbarSlotSilently")
class SelectHotbarSlotSilentlyEvent(val requester: Any?, val slot: Int): CancellableEvent()

@Tag("browserUrlChange")
class BrowserUrlChangeEvent(val index: Int, val url: String) : Event(), WebSocketEvent

@Tag("userLoggedIn")
object UserLoggedInEvent : Event(), WebSocketEvent

@Tag("userLoggedOut")
object UserLoggedOutEvent : Event(), WebSocketEvent

