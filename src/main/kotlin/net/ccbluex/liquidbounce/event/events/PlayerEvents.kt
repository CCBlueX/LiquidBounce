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

import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.utils.client.Nameable
import net.ccbluex.liquidbounce.web.socket.protocol.event.WebSocketEvent
import net.minecraft.entity.MovementType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.math.Vec3d

// Entity events bound to client-user entity
@Nameable("healthUpdate")
class HealthUpdateEvent(val health: Float, val food: Int, val saturation: Float, val previousHealth: Float) : Event()

@Nameable("death")
@WebSocketEvent
class DeathEvent : Event()

@Nameable("playerTick")
class PlayerTickEvent : CancellableEvent()

@Nameable("playerPostTick")
class PlayerPostTickEvent : Event()

@Nameable("playerMovementTick")
class PlayerMovementTickEvent : Event()

@Nameable("playerNetworkMovementTick")
class PlayerNetworkMovementTickEvent(val state: EventState) : Event()

@Nameable("playerPushOut")
class PlayerPushOutEvent : CancellableEvent()

@Nameable("playerMove")
class PlayerMoveEvent(val type: MovementType, val movement: Vec3d) : Event()

@Nameable("rotatedMovementInput")
class RotatedMovementInputEvent(var forward: Float, var sideways: Float) : Event()

@Nameable("playerJump")
class PlayerJumpEvent(var motion: Float) : CancellableEvent()

@Nameable("playerAfterJump")
class PlayerAfterJumpEvent : Event()

@Nameable("playerUseMultiplier")
class PlayerUseMultiplier(var forward: Float, var sideways: Float) : Event()

@Nameable("playerInteractedItem")
class PlayerInteractedItem(val player: PlayerEntity, val hand: Hand, val actionResult: ActionResult) : Event()

@Nameable("playerStrafe")
class PlayerVelocityStrafe(val movementInput: Vec3d, val speed: Float, val yaw: Float, var velocity: Vec3d) : Event()

@Nameable("playerStride")
class PlayerStrideEvent(var strideForce: Float) : Event()

@Nameable("playerSafeWalk")
class PlayerSafeWalkEvent(var isSafeWalk: Boolean = false) : Event()

@Nameable("playerStep")
class PlayerStepEvent(var height: Float) : Event()

@Nameable("playerStepSuccess")
class PlayerStepSuccessEvent(val movementVec: Vec3d, var adjustedVec: Vec3d) : Event()
