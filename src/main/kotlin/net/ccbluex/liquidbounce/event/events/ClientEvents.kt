/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.features.chat.client.packet.User
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.Nameable

@Nameable("clientStart")
class ClientStartEvent : Event()

@Nameable("clientShutdown")
class ClientShutdownEvent : Event()

@Nameable("valueChanged")
class ValueChangedEvent(val value: Value<*>) : Event()

@Nameable("toggleModule")
class ToggleModuleEvent(val module: Module, val newState: Boolean, val ignoreCondition: Boolean = false) : Event()

@Nameable("choiceChange")
class ChoiceChangeEvent(val module: Module, val oldChoice: Choice, val newChoice: Choice) : Event()

@Nameable("notification")
class NotificationEvent(val title: String, val message: String, val severity: Severity) : Event() {
    enum class Severity {
        INFO, SUCCESS, ERROR, ENABLED, DISABLED
    }
}

@Nameable("clientChatMessage")
class ClientChatMessageEvent(val user: User, val message: String, val chatGroup: ChatGroup) : Event() {
    enum class ChatGroup {
        PUBLIC_CHAT, PRIVATE_CHAT
    }
}

@Nameable("clientChatError")
class ClientChatErrorEvent(val error: String) : Event()

@Nameable("altManagerUpdate")
class AltManagerUpdateEvent(val success: Boolean, val message: String) : Event()

/**
 * The simulated tick event is called by the [MovementInputEvent] with a simulated movement context.
 * This context includes a simulated player position one tick into the future.
 * Position changes will not apply within the simulated tick. Only use this for prediction purposes as
 * updating the rotation or target.
 */
@Nameable("simulatedTick")
class SimulatedTickEvent : Event()
