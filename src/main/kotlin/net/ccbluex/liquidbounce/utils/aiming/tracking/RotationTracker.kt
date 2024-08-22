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
package net.ccbluex.liquidbounce.utils.aiming.tracking

import net.ccbluex.liquidbounce.utils.aiming.RotationEngine
import net.ccbluex.liquidbounce.utils.aiming.data.Orientation
import net.ccbluex.liquidbounce.utils.aiming.data.AngleLine
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.orientation
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times

@Suppress("LongParameterList")
data class RotationTracker(
    val engine: RotationEngine,
    val makeAngleLine: () -> AngleLine?,
) {

    companion object {

        fun withFixedAngleLine(
            engine: RotationEngine,
            angleLine: AngleLine
        ): RotationTracker = RotationTracker(engine) { angleLine }

        fun withFixedAngle(
            engine: RotationEngine,
            orientation: Orientation
        ): RotationTracker = RotationTracker(engine) { AngleLine(player.eyes, orientation) }

        fun withDynamicAngleLine(
            engine: RotationEngine,
            makeAngleLine: () -> AngleLine?
        ): RotationTracker = RotationTracker(engine, makeAngleLine)

    }

    /**
     * Calculates the next rotation to aim at.
     * [fromOrientation] is the current rotation or rather last rotation we aimed at. It is being used to calculate the
     * next rotation.
     *
     * We might even return null if we do not want to aim at anything yet.
     */
    fun nextRotation(fromOrientation: Orientation, isResetting: Boolean): Orientation {
        val angleLine = if (isResetting) {
            AngleLine(orientation = player.orientation)
        } else {
            makeAngleLine() ?: return fromOrientation
        }

        val fromVector = angleLine.fromPoint + fromOrientation.polar3d

        val horizontalFactor = 0.4
        val verticalFactor = 0.1

        val direction = angleLine.direction.multiply(horizontalFactor, verticalFactor, horizontalFactor)
        val newAngleLine = AngleLine(angleLine.fromPoint, fromVector + direction)

        return newAngleLine.orientation
    }

}
