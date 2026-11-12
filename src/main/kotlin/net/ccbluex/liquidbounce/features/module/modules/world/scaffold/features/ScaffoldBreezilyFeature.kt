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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.core.Direction
import kotlin.math.floor

/**
 * Breezily bridging: while the player runs forwards over a bridge, it taps left and right to stay over the blocks.
 */
object ScaffoldBreezilyFeature : ToggleableValueGroup(ModuleScaffold, "Breezily", false) {

    private var lastSideways = 0f
    private var lastAirTime = 0L
    private var currentEdgeDistanceRandom = 0.45

    private val edgeDistance by floatRange(
        "EdgeDistance", 0.45f..0.5f, 0.25f..0.5f, "blocks"
    )

    @Suppress("unused")
    private val handleMovementInput = handler<MovementInputEvent>(
        priority = EventPriorityConvention.SAFETY_FEATURE
    ) { event ->
        if (!event.directionalInput.forwards || player.isShiftKeyDown) {
            return@handler
        }

        if (player.blockPosition().below().state!!.isAir) {
            lastAirTime = System.currentTimeMillis()
        } else if (System.currentTimeMillis() - lastAirTime > 500) {
            return@handler
        }

        val modX = player.x - floor(player.x)
        val modZ = player.z - floor(player.z)

        val ma = 1 - currentEdgeDistanceRandom
        var currentSideways = 0f
        when (Direction.fromYRot(player.yRot.toDouble())) {
            Direction.SOUTH -> {
                if (modX > ma) currentSideways = 1f
                if (modX < currentEdgeDistanceRandom) currentSideways = -1f
            }

            Direction.NORTH -> {
                if (modX > ma) currentSideways = -1f
                if (modX < currentEdgeDistanceRandom) currentSideways = 1f
            }

            Direction.EAST -> {
                if (modZ > ma) currentSideways = -1f
                if (modZ < currentEdgeDistanceRandom) currentSideways = 1f
            }

            Direction.WEST -> {
                if (modZ > ma) currentSideways = 1f
                if (modZ < currentEdgeDistanceRandom) currentSideways = -1f
            }
            else -> {
                // do nothing
            }
        }

        if (lastSideways != currentSideways && currentSideways != 0f) {
            lastSideways = currentSideways
            currentEdgeDistanceRandom = edgeDistance.random().toDouble()
        }

        event.directionalInput = DirectionalInput(
            event.directionalInput.forwards,
            event.directionalInput.backwards,
            lastSideways == -1f,
            lastSideways == 1f
        )
    }

}
