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

import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.integration.interop.protocol.event.WebSocketEvent
import net.ccbluex.liquidbounce.annotations.InbuiltEvent
import net.minecraft.entity.MovementType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.Fluid
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.math.Vec3d

// Entity events bound to client-user entity
@InbuiltEvent("healthUpdate")
class HealthUpdateEvent(val health: Float, val food: Int, val saturation: Float, val previousHealth: Float) : Event()

@InbuiltEvent("death")
object DeathEvent : Event(), WebSocketEvent

@InbuiltEvent("playerTick")
class PlayerTickEvent : CancellableEvent()

@InbuiltEvent("playerPostTick")
object PlayerPostTickEvent : Event()

@InbuiltEvent("playerMovementTick")
object PlayerMovementTickEvent : Event()

@InbuiltEvent("playerNetworkMovementTick")
class PlayerNetworkMovementTickEvent(
    val state: EventState,
    var x: Double,
    var y: Double,
    var z: Double,
    var ground: Boolean
) : CancellableEvent()

@InbuiltEvent("playerPushOut")
class PlayerPushOutEvent : CancellableEvent()

@InbuiltEvent("playerMove")
class PlayerMoveEvent(val type: MovementType, var movement: Vec3d) : Event()

@InbuiltEvent("playerJump")
class PlayerJumpEvent(var motion: Float, var yaw: Float) : CancellableEvent()

@InbuiltEvent("playerAfterJump")
object PlayerAfterJumpEvent : Event()

@InbuiltEvent("playerUseMultiplier")
class PlayerUseMultiplier(var forward: Float, var sideways: Float) : Event()

@InbuiltEvent("playerSneakMultiplier")
class PlayerSneakMultiplier(var multiplier: Double) : Event()

/**
 * Warning: UseHotbarSlotOrOffHand won't stimulate this event
 */
@InbuiltEvent("playerInteractItem")
class PlayerInteractItemEvent : CancellableEvent()

@InbuiltEvent("playerInteractedItem")
class PlayerInteractedItemEvent(val player: PlayerEntity, val hand: Hand, val actionResult: ActionResult) : Event()

@InbuiltEvent("playerStrafe")
class PlayerVelocityStrafe(val movementInput: Vec3d, val speed: Float, val yaw: Float, var velocity: Vec3d) : Event()

@InbuiltEvent("playerStride")
class PlayerStrideEvent(var strideForce: Float) : Event()

@InbuiltEvent("playerSafeWalk")
class PlayerSafeWalkEvent(var isSafeWalk: Boolean = false) : Event()

@InbuiltEvent("playerStep")
class PlayerStepEvent(var height: Float) : Event()

@InbuiltEvent("playerStepSuccess")
class PlayerStepSuccessEvent(val movementVec: Vec3d, var adjustedVec: Vec3d) : Event()

@InbuiltEvent("playerFluidCollisionCheck")
class PlayerFluidCollisionCheckEvent(val fluid: TagKey<Fluid>) : CancellableEvent()
