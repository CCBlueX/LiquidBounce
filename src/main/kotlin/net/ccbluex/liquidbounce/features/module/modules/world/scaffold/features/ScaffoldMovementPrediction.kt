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

import net.ccbluex.liquidbounce.LiquidBounce.logger
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.math.average
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.ccbluex.liquidbounce.utils.math.withLength
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.findEdgeCollision
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2

object ScaffoldMovementPrediction : ToggleableValueGroup(ModuleScaffold, "Prediction", true) {

    private val lastPlacementOffsets = ArrayDeque<Vec3>()

    private const val MAX_PLACEMENT_OFFSETS = 4

    fun reset() {
        lastPlacementOffsets.clear()
    }

    fun onPlace(optimalLine: Line?, lastFallOffPosition: Vec3?) {
        if (optimalLine == null || !this.enabled) {
            return
        }

        val fallOffPoint = lastFallOffPosition ?: return

        val lineDirAngle = atan2(optimalLine.direction.z, optimalLine.direction.x).toFloat()

        val unrotatedOffset = (player.position() - fallOffPoint).yRot(lineDirAngle)

        val x = getAvgPlacementPos()

        if (x != null) {
            logger.debug(x.distanceTo(unrotatedOffset))
        }

        lastPlacementOffsets.addLast(unrotatedOffset)

        if (lastPlacementOffsets.size > MAX_PLACEMENT_OFFSETS) {
            lastPlacementOffsets.removeFirst()
        }
    }

    fun getAvgPlacementPos(): Vec3? {
        if (lastPlacementOffsets.isEmpty()) {
            return null
        }

        return lastPlacementOffsets.average()
    }

    /**
     * Calculates where the player will stand when he places the block. Useful for rotations
     *
     * @return the predicted pos or `null` if the prediction failed
     */
    fun getPredictedPlacementPos(optimalLine: Line?): Vec3? {
        if (optimalLine == null || !this.enabled) {
            return null
        }

        val optimalEdgeDist = 0.0

        // When we are close to the edge, we are able to place right now. Thus, we don't want to use a future position
        if (player.isCloseToEdge(DirectionalInput(player.input), distance = optimalEdgeDist)) {
            return null
        }

        // If the next placement point is far in the future. Don't predict for now
        val fallOffPoint = getFallOffPositionOnLine(optimalLine) ?: return null

        val fallOffPointToPlayer = fallOffPoint - player.position()

        val offset = when (val last = getAvgPlacementPos()) {
            null -> {
                // Move the point where we want to place a bit more to the player since we ideally want to place at an
                // edge distance of 0.2 or so
                fallOffPoint - fallOffPointToPlayer.normalize() * optimalEdgeDist
            }
            else -> {
                val lineDirAngle = atan2(optimalLine.direction.z, optimalLine.direction.x).toFloat()

                val predictedPos = fallOffPoint + last.yRot(-lineDirAngle)

                predictedPos
            }
        }

        return offset
    }

    fun getFallOffPositionOnLine(optimalLine: Line): Vec3? {
        // TODO Check if the player is moving away from the line and implement another prediction method for that case

        val nearestPosToPlayer = optimalLine.getNearestPointTo(player.position())

        val fromLine = nearestPosToPlayer.add(0.0, -0.1, 0.0)
        val toLine = fromLine + optimalLine.direction.withLength(3.0)

        val edgeCollision = findEdgeCollision(fromLine, toLine) ?: return null

        val fallOffPoint = edgeCollision.copy(y = player.y)

        return fallOffPoint
    }

}
