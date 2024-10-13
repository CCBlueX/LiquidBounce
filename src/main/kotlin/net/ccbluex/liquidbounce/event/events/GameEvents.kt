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
import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.utils.client.Nameable
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.integration.interop.protocol.event.WebSocketEvent
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.session.Session
import net.minecraft.text.Text

@Nameable("gameTick")
class GameTickEvent : Event()

@Nameable("key")
@WebSocketEvent
class KeyEvent(val key: Key, val action: Int, val mods: Int) : Event() {

    data class Key(
        @SerializedName("code")
        val keyCode: Int,
        @SerializedName("name")
        val translationKey: String
    )
}

// Input events
@Nameable("inputHandle")
class InputHandleEvent : Event()

@Nameable("movementInput")
class MovementInputEvent(var directionalInput: DirectionalInput, var jumping: Boolean, var sneaking: Boolean) : Event()

@Nameable("mouseRotation")
class MouseRotationEvent(var cursorDeltaX: Double, var cursorDeltaY: Double) : CancellableEvent()

@Nameable("keybindChange")
@WebSocketEvent
class KeybindChangeEvent: Event()

@Nameable("useCooldown")
class UseCooldownEvent(var cooldown: Int) : Event()

@Nameable("cancelBlockBreaking")
class CancelBlockBreakingEvent : CancellableEvent()

/**
 * All events which are related to the minecraft client
 */

@Nameable("session")
@WebSocketEvent
class SessionEvent(val session: Session) : Event()

@Nameable("screen")
class ScreenEvent(val screen: Screen?) : CancellableEvent()

@Nameable("chatSend")
@WebSocketEvent
class ChatSendEvent(val message: String) : CancellableEvent()

@Nameable("chatReceive")
@WebSocketEvent
class ChatReceiveEvent(
    val message: String,
    val textData: Text,
    val type: ChatType,
    val applyChatDecoration: (Text) -> Text
) : CancellableEvent() {

    enum class ChatType {
        CHAT_MESSAGE,
        DISGUISED_CHAT_MESSAGE,
        GAME_MESSAGE
    }

}

@Nameable("splashOverlay")
@WebSocketEvent
class SplashOverlayEvent(val showingSplash: Boolean) : Event()

@Nameable("splashProgress")
@WebSocketEvent
class SplashProgressEvent(val progress: Float, val isComplete: Boolean) : Event()

@Nameable("serverConnect")
@WebSocketEvent
class ServerConnectEvent(val serverName: String, val serverAddress: String) : Event()

@Nameable("disconnect")
@WebSocketEvent
class DisconnectEvent : Event()

@Nameable("overlayMessage")
@WebSocketEvent
class OverlayMessageEvent(val text: Text, val tinted: Boolean) : Event()
