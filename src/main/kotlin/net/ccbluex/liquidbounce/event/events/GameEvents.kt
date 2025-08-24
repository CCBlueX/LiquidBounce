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

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.integration.interop.protocol.event.WebSocketEvent
import net.ccbluex.liquidbounce.utils.client.Nameable
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen
import net.minecraft.client.network.CookieStorage
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.network.ServerInfo
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.option.Perspective
import net.minecraft.client.session.Session
import net.minecraft.client.util.InputUtil
import net.minecraft.item.ItemStack
import net.minecraft.text.Text

@Nameable("gameTick")
object GameTickEvent : Event()

/**
 * We can use this event to populate the render task queue with tasks that should be
 * executed in the same frame. This is useful for more responsive task execution
 * and allows to also schedule tasks off-schedule.
 */
@Nameable("gameRenderTaskQueue")
object GameRenderTaskQueueEvent : Event()

@Nameable("tickPacketProcess")
object TickPacketProcessEvent : Event()

@Nameable("key")
class KeyEvent(
    val key: InputUtil.Key,
    val action: Int,
) : Event(), WebSocketEvent

// Input events
@Nameable("inputHandle")
object InputHandleEvent : Event()

@Nameable("movementInput")
class MovementInputEvent(
    var directionalInput: DirectionalInput,
    var jump: Boolean,
    var sneak: Boolean,
) : Event()

@Nameable("sprint")
class SprintEvent(
    val directionalInput: DirectionalInput,
    var sprint: Boolean,
    val source: Source,
) : Event() {
    enum class Source {
        INPUT,
        MOVEMENT_TICK,
        NETWORK,
    }
}

@Nameable("sneakNetwork")
class SneakNetworkEvent(
    val directionalInput: DirectionalInput,
    var sneak: Boolean,
) : Event()

@Nameable("mouseRotation")
class MouseRotationEvent(
    var cursorDeltaX: Double,
    var cursorDeltaY: Double,
) : CancellableEvent()

@Nameable("keybindChange")
object KeybindChangeEvent : Event(), WebSocketEvent

@Nameable("keybindIsPressed")
class KeybindIsPressedEvent(
    val keyBinding: KeyBinding,
    var isPressed: Boolean,
) : Event()

@Nameable("useCooldown")
class UseCooldownEvent(
    var cooldown: Int,
) : Event()

@Nameable("cancelBlockBreaking")
class CancelBlockBreakingEvent : CancellableEvent()

@Nameable("autoJump")
class MinecraftAutoJumpEvent(
    var autoJump: Boolean,
) : Event()

/**
 * All events which are related to the minecraft client
 */

@Nameable("session")
class SessionEvent(
    val session: Session,
) : Event(), WebSocketEvent

@Nameable("screen")
class ScreenEvent(
    val screen: Screen?,
) : CancellableEvent()

@Nameable("chatSend")
class ChatSendEvent(
    val message: String,
) : CancellableEvent(), WebSocketEvent

@Nameable("chatReceive")
class ChatReceiveEvent(
    val message: String,
    val textData: Text,
    val type: ChatType,
    val applyChatDecoration: (Text) -> Text,
) : CancellableEvent(), WebSocketEvent {
    enum class ChatType(override val choiceName: String) : NamedChoice {
        CHAT_MESSAGE("ChatMessage"),
        DISGUISED_CHAT_MESSAGE("DisguisedChatMessage"),
        GAME_MESSAGE("GameMessage"),
    }
}

@Nameable("serverConnect")
class ServerConnectEvent(
    val connectScreen: ConnectScreen,
    val address: ServerAddress,
    val serverInfo: ServerInfo,
    val cookieStorage: CookieStorage?,
) : CancellableEvent()

@Nameable("disconnect")
object DisconnectEvent : Event(), WebSocketEvent

@Nameable("overlayMessage")
class OverlayMessageEvent(
    val text: Text,
    val tinted: Boolean,
) : Event(), WebSocketEvent

@Nameable("perspective")
class PerspectiveEvent(
    var perspective: Perspective,
) : Event()

@Nameable("itemLoreQuery")
class ItemLoreQueryEvent(
    val itemStack: ItemStack,
    val lore: ArrayList<Text>,
) : Event()
