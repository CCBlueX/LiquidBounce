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

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.annotations.Tag
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.integration.interop.protocol.event.WebSocketEvent
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.client.CameraType
import net.minecraft.client.KeyMapping
import net.minecraft.client.User
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.TransferState
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

@Tag("gameTick")
object GameTickEvent : Event()

/**
 * We can use this event to populate the render task queue with tasks that should be
 * executed in the same frame. This is useful for more responsive task execution
 * and allows to also schedule tasks off-schedule.
 */
@Tag("gameRenderTaskQueue")
object GameRenderTaskQueueEvent : Event()

@Tag("tickPacketProcess")
object TickPacketProcessEvent : Event()

@Tag("key")
class KeyEvent(
    val key: InputConstants.Key,
    val action: Int,
) : Event(), WebSocketEvent

// Input events
@Tag("inputHandle")
object InputHandleEvent : Event()

@Tag("movementInput")
class MovementInputEvent(
    var directionalInput: DirectionalInput,
    var jump: Boolean,
    var sneak: Boolean,
) : Event()

@Tag("sprint")
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

@Tag("mouseRotation")
class MouseRotationEvent(
    var cursorDeltaX: Double,
    var cursorDeltaY: Double,
) : CancellableEvent()

@Tag("keybindChange")
object KeybindChangeEvent : Event(), WebSocketEvent

@Tag("keybindIsPressed")
class KeybindIsPressedEvent(
    val keyBinding: KeyMapping,
    var isPressed: Boolean,
) : Event()

@Tag("useCooldown")
class UseCooldownEvent(
    var cooldown: Int,
) : Event()

@Tag("cancelBlockBreaking")
class CancelBlockBreakingEvent : CancellableEvent()

@Tag("allowAutoJump")
class AllowAutoJumpEvent(
    var isAllowed: Boolean,
) : Event()

/**
 * All events which are related to the minecraft client
 */

@Tag("session")
class SessionEvent(
    val session: User,
) : Event(), WebSocketEvent

@Tag("screen")
class ScreenEvent(
    val screen: Screen?,
) : CancellableEvent()

@Tag("chatSend")
class ChatSendEvent(
    val message: String,
) : CancellableEvent(), WebSocketEvent

@Tag("chatReceive")
class ChatReceiveEvent(
    val message: String,
    val textData: Component,
    val type: ChatType,
    val applyChatDecoration: (Component) -> Component,
) : CancellableEvent(), WebSocketEvent {
    enum class ChatType(override val tag: String) : Tagged {
        CHAT_MESSAGE("ChatMessage"),
        DISGUISED_CHAT_MESSAGE("DisguisedChatMessage"),
        GAME_MESSAGE("GameMessage"),
    }
}

@Tag("serverConnect")
class ServerConnectEvent(
    val connectScreen: ConnectScreen,
    val address: ServerAddress,
    val serverInfo: ServerData,
    val cookieStorage: TransferState?,
) : CancellableEvent()

@Tag("disconnect")
object DisconnectEvent : Event(), WebSocketEvent

@Tag("overlayMessage")
class OverlayMessageEvent(
    val text: Component,
    val tinted: Boolean,
) : Event(), WebSocketEvent

@Tag("perspective")
class PerspectiveEvent(
    var perspective: CameraType,
) : Event()

@Tag("itemLoreQuery")
class ItemLoreQueryEvent(
    val itemStack: ItemStack,
    val lore: ArrayList<Component>,
) : Event()
