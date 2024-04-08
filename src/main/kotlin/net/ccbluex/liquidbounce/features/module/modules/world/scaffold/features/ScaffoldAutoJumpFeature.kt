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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.EventScheduler

object ScaffoldAutoJumpFeature : ToggleableConfigurable(ModuleScaffold, "AutoJump", false) {
    private val whenGoingDiagonal by boolean("WhenGoingDiagonal", false)
    private val predictFactor by float("PredictFactor", 0.54f, 0f..2f)
    private val useDelay by boolean("UseDelay", true)

    private val maxBlocks by int("MaxBlocks", 8, 3..17)

    private var blocksPlaced = 0

    fun onBlockPlacement() {
        blocksPlaced++
    }

    fun jumpIfNeeded(ticksUntilNextBlock: Int) {
        if (shouldJump(ticksUntilNextBlock)) {
            EventScheduler.schedule<MovementInputEvent>(ModuleScaffold) {
                it.jumping = true
            }
            blocksPlaced = 0
        }
    }

    var isGoingDiagonal = false

    fun shouldJump(ticksUntilNextBlock: Int): Boolean {
        if (!enabled)
            return false
        if (!player.isOnGround)
            return false
        if (player.isSneaking)
            return false
        if (!whenGoingDiagonal && isGoingDiagonal)
            return false

        val extraPrediction =
            if (blocksPlaced >= maxBlocks) 1
            else if (useDelay) ticksUntilNextBlock
            else 0

        // TODO: Use player.isCloseToEdge() instead
        val predictedBoundingBox = player.boundingBox.offset(0.0, -1.5, 0.0)
            .offset(player.velocity.multiply(predictFactor.toDouble() + extraPrediction))

        return world.getBlockCollisions(player, predictedBoundingBox).none()
    }
}
