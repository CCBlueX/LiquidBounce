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

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.Nameable
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

@Nameable("engineRender")
class EngineRenderEvent(val tickDelta: Float) : Event()

@Nameable("overlayRender")
class OverlayRenderEvent(val matrixStack: MatrixStack, val tickDelta: Float) : Event()

// Input events

@Nameable("inputHandle")
class InputHandleEvent : Event()

@Nameable("key")
class KeyEvent(val key: InputUtil.Key, val action: Int, val mods: Int) : Event()

// User action events

@Nameable("attack")
class AttackEvent(val enemy: Entity) : Event()

@Nameable("session")
class SessionEvent : Event()

@Nameable("screen")
class ScreenEvent(val screen: Screen?) : Event()

@Nameable("chatSend")
class ChatSendEvent(val message: String) : CancellableEvent()

// World events

/**
 * Block Shape hooked at CACTUS_BLOCK, FLUID_BLOCK to reduce performance impact and headache
 */
@Nameable("blockShape")
class BlockShapeEvent(val state: BlockState, val pos: BlockPos, var shape: VoxelShape) : Event()

// Entity events

@Nameable("entityMargin")
class EntityMarginEvent(val entity: Entity, var margin: Float) : Event()


// Entity events bound to client-user entity

@Nameable("playerTick")
class PlayerTickEvent : Event()

@Nameable("playerNetworkMovementTick")
class PlayerNetworkMovementTickEvent(val state: EventState) : Event()

@Nameable("playerPushOut")
class PlayerPushOutEvent : CancellableEvent()

@Nameable("playerMove")
class PlayerMoveEvent(val type: MovementType, val movement: Vec3d) : Event()

@Nameable("cancelBlockBreaking")
class CancelBlockBreakingEvent : CancellableEvent()

// Network events

@Nameable("packet")
class PacketEvent(val packet: Packet<*>) : CancellableEvent()

// Client events

@Nameable("clientStart")
class ClientStartEvent : Event()

@Nameable("clientShutdown")
class ClientShutdownEvent : Event()

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
