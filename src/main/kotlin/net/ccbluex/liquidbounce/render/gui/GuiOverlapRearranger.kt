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

package net.ccbluex.liquidbounce.render.gui

import kotlin.math.abs

class GuiOverlapRearranger(
    private val maxIter: Int = 16,
) {
    init {
        require(maxIter > 0) { "maxIter must be greater than zero." }
    }

    @Suppress("NestedBlockDepth", "CognitiveComplexMethod")
    fun rearrange(elements: Collection<GuiRearrangeable>) {
        if (elements.size <= 1) {
            return
        }

        val sorted = elements.toTypedArray()
        sorted.sortWith { a, b ->
            val ay = a.bounds.yCenter
            val by = b.bounds.yCenter
            when {
                ay != by -> ay.compareTo(by)
                else -> a.bounds.xCenter.compareTo(b.bounds.xCenter)
            }
        }

        var iter = 0
        while (iter++ < maxIter) {
            var moved = false

            for (i in 0 until sorted.size) {
                for (j in i + 1 until sorted.size) {
                    val a = sorted[i]
                    val b = sorted[j]
                    val aBounds = a.bounds
                    val bBounds = b.bounds

                    val ax = aBounds.xCenter
                    val ay = aBounds.yCenter
                    val bx = bBounds.xCenter
                    val by = bBounds.yCenter
                    val dx = (aBounds.width + bBounds.width) * 0.5f - abs(ax - bx)
                    val dy = (aBounds.height + bBounds.height) * 0.5f - abs(ay - by)

                    if (dx > 0f && dy > 0f) {
                        b.bounds = if (dx < dy) {
                            bBounds.offset(if (ax < bx) dx else -dx, 0f)
                        } else {
                            bBounds.offset(0f, if (ay < by) dy else -dy)
                        }
                        moved = true
                    }
                }
            }

            if (!moved) {
                break
            }
        }
    }
}
