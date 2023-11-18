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

import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.utils.client.ForcedState
import net.ccbluex.liquidbounce.utils.client.Nameable
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.web.socket.protocol.event.WebSocketEvent
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text

@Nameable("gameTick")
class GameTickEvent : Event()

@Nameable("key")
class KeyEvent(val key: InputUtil.Key, val action: Int, val mods: Int) : Event()

// Input events
@Nameable("inputHandle")
class InputHandleEvent : Event()

@Nameable("movementInput")
class MovementInputEvent(var directionalInput: DirectionalInput, var jumping: Boolean) : Event()

@Nameable("mouseRotation")
class MouseRotationEvent(var cursorDeltaX: Double, var cursorDeltaY: Double) : CancellableEvent()

@Nameable("keyBinding")
class KeyBindingEvent(var key: KeyBinding) : Event()

@Nameable("useCooldown")
class UseCooldownEvent(var cooldown: Int) : Event()

@Nameable("cancelBlockBreaking")
class CancelBlockBreakingEvent : CancellableEvent()

@Nameable("stateUpdate")
class StateUpdateEvent : Event() {
    val state: ForcedState = ForcedState()
}

/**
 * All events which are related to the the minecraft client
 */

@Nameable("session")
@WebSocketEvent
class SessionEvent : Event()

@Nameable("screen")
class ScreenEvent(val screen: Screen?) : CancellableEvent()

@Nameable("chatSend")
@WebSocketEvent
class ChatSendEvent(val message: String) : CancellableEvent()

@Nameable("chatReceive")
@WebSocketEvent
class ChatReceiveEvent(val message: String, val textData: Text, val type: ChatType) : Event() {

    enum class ChatType {
        CHAT_MESSAGE, DISGUISED_CHAT_MESSAGE, GAME_MESSAGE
    }

}
