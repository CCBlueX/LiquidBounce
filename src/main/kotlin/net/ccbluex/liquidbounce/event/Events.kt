/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.event

import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.Nameable
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.InputUtil
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.MovementType
import net.minecraft.network.Packet
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.shape.VoxelShape

// Game events

@Nameable("gameTick")
class GameTickEvent : Event()

// Render events

@Nameable("gameRender")
class GameRenderEvent : Event()

@Nameable("engineRender")
class EngineRenderEvent(val tickDelta: Float) : Event()

@Nameable("overlayRender")
class OverlayRenderEvent(val matrixStack: MatrixStack, val tickDelta: Float) : Event()

@Nameable("screenRender")
class ScreenRenderEvent(val screen: Screen, val matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) : Event()

@Nameable("windowResize")
class WindowResizeEvent(val window: Long, val width: Int, val height: Int) : Event()

@Nameable("windowFocus")
class WindowFocusEvent(val window: Long, val focused: Boolean) : Event()

@Nameable("mouseButton")
class MouseButtonEvent(val window: Long, val button: Int, val action: Int, val mods: Int) : Event()

@Nameable("mouseScroll")
class MouseScrollEvent(val window: Long, val horizontal: Double, val vertical: Double) : Event()

@Nameable("mouseCursor")
class MouseCursorEvent(val window: Long, val x: Double, val y: Double) : Event()

@Nameable("keyboardKey")
class KeyboardKeyEvent(val window: Long, val keyCode: Int, val scancode: Int, val action: Int, val mods: Int) : Event()

@Nameable("keyboardChar")
class KeyboardCharEvent(val window: Long, val codepoint: Int) : Event()

// Input events

@Nameable("inputHandle")
class InputHandleEvent : Event()

@Nameable("key")
class KeyEvent(val key: InputUtil.Key, val action: Int, val mods: Int) : Event()

@Nameable("mouseRotation")
class MouseRotationEvent(var cursorDeltaX: Double, var cursorDeltaY: Double) : CancellableEvent()

// User action events

@Nameable("attack")
class AttackEvent(val enemy: Entity) : Event()

@Nameable("session")
class SessionEvent : Event()

@Nameable("screen")
class ScreenEvent(val screen: Screen?) : CancellableEvent()

@Nameable("chatSend")
class ChatSendEvent(val message: String) : CancellableEvent()

@Nameable("useCooldown")
class UseCooldownEvent(var cooldown: Int) : Event()

// World events

@Nameable("blockShape")
class BlockShapeEvent(val state: BlockState, val pos: BlockPos, var shape: VoxelShape) : Event()

@Nameable("blockMultiplier")
class BlockVelocityMultiplierEvent(val block: Block, var multiplier: Float) : Event()

// Entity events

@Nameable("entityMargin")
class EntityMarginEvent(val entity: Entity, var margin: Float) : Event()

// Entity events bound to client-user entity

@Nameable("playerTick")
class PlayerTickEvent : Event()

@Nameable("playerMovementTick")
class PlayerMovementTickEvent : Event()

@Nameable("playerNetworkMovementTick")
class PlayerNetworkMovementTickEvent(val state: EventState) : Event()

@Nameable("playerPushOut")
class PlayerPushOutEvent : CancellableEvent()

@Nameable("playerMove")
class PlayerMoveEvent(val type: MovementType, val movement: Vec3d) : Event()

@Nameable("playerJump")
class PlayerJumpEvent(var motion: Float) : CancellableEvent()

@Nameable("playerUseMultiplier")
class PlayerUseMultiplier(var forward: Float, var sideways: Float) : Event()

@Nameable("playerVelocity")
class PlayerVelocityStrafe(val movementInput: Vec3d, val speed: Float, val yaw: Float, var velocity: Vec3d) : Event()

@Nameable("playerStride")
class PlayerStrideEvent(var strideOnAir: Boolean) : Event()

@Nameable("cancelBlockBreaking")
class CancelBlockBreakingEvent : CancellableEvent()

// Network events

@Nameable("packet")
class PacketEvent<T : Packet<*>>(val origin: TransferOrigin, val packet: T) : CancellableEvent()

enum class TransferOrigin {
    SEND, RECEIVE
}

// Client events

@Nameable("clientStart")
class ClientStartEvent : Event()

@Nameable("clientShutdown")
class ClientShutdownEvent : Event()

@Nameable("valueChanged")
class ValueChangedEvent(val value: Value<*>) : Event()

@Nameable("toggleModule")
class ToggleModuleEvent(val module: Module, val newState: Boolean) : Event()

@Nameable("notification")
class NotificationEvent(val title: String, val message: String, val severity: Severity) : Event() {
    enum class Severity {
        INFO,
        SUCCESS,
        ERROR
    }
}
