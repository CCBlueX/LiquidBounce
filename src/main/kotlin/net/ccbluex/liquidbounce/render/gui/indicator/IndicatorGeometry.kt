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

package net.ccbluex.liquidbounce.render.gui.indicator

import org.joml.Vector2f
import kotlin.math.abs

internal object IndicatorGeometry {

    /**
     * Where the ray from the center of a [halfWidth] x [halfHeight] rectangle, which keeps [margin] free, leaves it
     * in the direction of ([directionX], [directionY]).
     *
     * Only the direction of the vector matters, its length is discarded. That is what a target behind the camera
     * needs, where the offset carries a direction but no usable length.
     */
    fun borderPoint(
        directionX: Float,
        directionY: Float,
        halfWidth: Float,
        halfHeight: Float,
        margin: Float,
        target: Vector2f = Vector2f(),
    ): Vector2f {
        val limitX = (halfWidth - margin).coerceAtLeast(0.0F)
        val limitY = (halfHeight - margin).coerceAtLeast(0.0F)

        val scale = minOf(
            if (directionX == 0.0F) Float.POSITIVE_INFINITY else limitX / abs(directionX),
            if (directionY == 0.0F) Float.POSITIVE_INFINITY else limitY / abs(directionY),
        )

        return if (scale.isFinite()) {
            target.set(directionX * scale, directionY * scale)
        } else {
            // No direction at all, so there is no border point to pick.
            target.set(0.0F, 0.0F)
        }
    }

    /**
     * [offsetX]/[offsetY] itself while it is inside the rectangle, otherwise [borderPoint] of its direction, so that
     * a target on screen keeps its projected position while one outside is pulled onto the border along the
     * direction it actually is.
     */
    fun clampToBorder(
        offsetX: Float,
        offsetY: Float,
        halfWidth: Float,
        halfHeight: Float,
        margin: Float,
        target: Vector2f = Vector2f(),
    ): Vector2f {
        val limitX = (halfWidth - margin).coerceAtLeast(0.0F)
        val limitY = (halfHeight - margin).coerceAtLeast(0.0F)

        if (abs(offsetX) <= limitX && abs(offsetY) <= limitY) {
            return target.set(offsetX, offsetY)
        }

        return borderPoint(offsetX, offsetY, halfWidth, halfHeight, margin, target)
    }

    /**
     * How far a span running from [min] to [max] has to move so that it stays within `0..extent`.
     *
     * A span wider than the extent cannot fit at all: it then keeps its start inside and overflows the other end,
     * which is the least bad option for a tag that simply is too long.
     */
    fun fitInside(min: Float, max: Float, extent: Float): Float = when {
        min < 0.0F -> -min
        max > extent -> extent - max
        else -> 0.0F
    }

}
