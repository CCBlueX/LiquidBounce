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
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.moving

/**
 * Telly feature
 *
 * This is based on the telly technique and means that the player will jump when moving.
 * That allows for a faster scaffold.
 * Depending on the SameY setting, we might scaffold upwards.
 *
 * @see ModuleScaffold
 */
object ScaffoldTellyFeature : ToggleableConfigurable(ScaffoldNormalTechnique, "Telly", false) {

    val doNotAim: Boolean
        get() = offGroundTicks < straightTicks && groundTicks >= jumpTicks

    private var offGroundTicks = 0
    private var groundTicks = 0

    private val straightTicks by int("Straight", 0, 0..5, "ticks")
    private val jumpTicksOpt by intRange("Jump", 0..0, 0..10, "ticks")
    private var jumpTicks = jumpTicksOpt.random()

    @Suppress("unused")
    private val gameHandler = handler<GameTickEvent> {
        if (player.isOnGround) {
            offGroundTicks = 0
            groundTicks++
        } else {
            offGroundTicks++
        }
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        if (!player.moving || ModuleScaffold.blockCount <= 0) {
            return@handler
        }

        // If we want to aim straight for more than 1 tick, we
        // have to predict if we are about to hit the ground
        if (this.straightTicks > 0 && player.velocity.y <= 0) {
            val snapshots = PlayerSimulationCache
                .getSimulationForLocalPlayer()
                .getSnapshotsBetween(0 until straightTicks)
            chat("ticks until ground: ${
                snapshots.withIndex()
                    .filter { (_, snapshot) ->
                        snapshot.onGround
                    }.minByOrNull { (index, _) ->
                        index
                    }?.index ?: -1
            }")

            val touchesGround = snapshots.any { snapshot -> snapshot.onGround }
            val touchesGroundOnNextTick = snapshots.firstOrNull()?.onGround ?: false
            if (touchesGround) {
                offGroundTicks = 0

                if (touchesGroundOnNextTick) {
                    // This will overwrite any Keep Reset delay
                    chat("Force Reset")
                    RotationManager.forceReset()
                }
            }
        }

        val isStraight = RotationManager.currentRotation == null || straightTicks == 0
        if (isStraight) {
            groundTicks++

            if (groundTicks >= jumpTicks) {
                event.jumping = true
            }
        }
    }

    @Suppress
    private val afterJumpHandler = handler<PlayerAfterJumpEvent> {
        groundTicks = 0
        jumpTicks = jumpTicksOpt.random()
    }

}
