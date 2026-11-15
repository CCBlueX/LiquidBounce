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
package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.agent.simulation.FallingPlayer
import net.ccbluex.agent.simulation.SimulatedInput
import net.ccbluex.agent.simulation.SimulatedPlayer
import net.ccbluex.agent.simulation.SimulationHooks
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
import net.ccbluex.liquidbounce.event.events.PlayerSneakMultiplier
import net.ccbluex.liquidbounce.event.events.PlayerUseMultiplier
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.ModuleNoSlow
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.MoverType
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3

/** How the modules change the local player's movement, so its simulations move the same. */
object LocalPlayerHooks : SimulationHooks {

    override fun itemUseMultiplier(multiplier: Float): Vec2 {
        val event = callEvent(PlayerUseMultiplier(multiplier, multiplier))
        return Vec2(event.sideways, event.forward)
    }

    override fun sneakingSpeed(speed: Double) = callEvent(PlayerSneakMultiplier(speed)).multiplier

    override fun move(type: MoverType, movement: Vec3) = callEvent(PlayerMoveEvent(type, movement)).movement

    override fun slowedByItemUse() = !ModuleNoSlow.running

}

/** A simulation of the local player holding [input]. */
fun SimulatedPlayer.Companion.fromClientPlayer(input: SimulatedInput): SimulatedPlayer =
    fromPlayer(player, input, PlayerSimulationCache.isLocalPlayerMoving, LocalPlayerHooks)

/** The local player's keys with [directionalInput], held back at ledges while a module makes it safe walk. */
fun SimulatedInput.Companion.fromClientPlayer(
    directionalInput: DirectionalInput,
    jump: Boolean = player.input.keyPresses.jump,
    sprinting: Boolean = player.isSprinting || player.input.keyPresses.sprint,
    sneaking: Boolean = player.isShiftKeyDown,
) = SimulatedInput(
    directionalInput.toInput(jump = jump, sneak = sneaking, sprint = sprinting),
    forceStayOnGroundSurface = callEvent(PlayerSafeWalkEvent()).isSafeWalk,
)

/** Follows the player with its current keys until it lands, moving facing [movementYaw] from [motion] on. */
fun LocalPlayer.fallingPlayer(movementYaw: Float = yRot, motion: Vec3 = deltaMovement) = FallingPlayer(
    this,
    SimulatedInput.fromClientPlayer(DirectionalInput(input)),
    PlayerSimulationCache.isLocalPlayerMoving,
    LocalPlayerHooks,
    motion,
    movementYaw,
)
