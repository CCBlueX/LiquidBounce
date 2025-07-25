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
 */

package net.ccbluex.liquidbounce.utils.aiming.point.features

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.util.math.Vec3d

/**
 * Lazy Point allows you to set a threshold when the point is going to be updated.
 * If the new point is below this threshold, we return the current point instead
 */
internal class LazyPoint(parent: EventListener) : ToggleableConfigurable(parent, "Lazy", false) {

    private val threshold by floatRange("Threshold", 0.0f..0.2f, 0.01f..1f).onChanged { range ->
        currentThreshold = range.random()
    }

    private var currentThreshold: Float = threshold.random()
    private var currentPoint: Vec3d? = null

    /**
     * Update the point if the distance is greater than the threshold
     */
    fun update(point: Vec3d): Vec3d {
        if (!enabled) {
            return point
        }

        val currentPoint = currentPoint ?: run {
            this.currentPoint = point
            return point
        }

        val distance = point.distanceTo(currentPoint)
        ModuleDebug.debugParameter(this, "Distance", distance)

        // Check if the current point has not reached the minimum threshold to move
        if (distance < currentThreshold) {
            return currentPoint
        }

        this.currentPoint = point
        this.currentThreshold = threshold.random()
        return point
    }


}
