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
import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.features.chat.packet.User
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.Nameable
import net.ccbluex.liquidbounce.web.browser.supports.IBrowser
import net.ccbluex.liquidbounce.web.socket.protocol.event.WebSocketEvent

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

@Nameable("altManagerUpdate")
@WebSocketEvent
class AltManagerUpdateEvent(val success: Boolean, val message: String) : Event()

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

/**
 * The simulated tick event is called by the [MovementInputEvent] with a simulated movement context.
 * This context includes a simulated player position one tick into the future.
 * Position changes will not apply within the simulated tick. Only use this for prediction purposes as
 * updating the rotation or target.
 */
@Nameable("simulatedTick")
class SimulatedTickEvent : Event()
