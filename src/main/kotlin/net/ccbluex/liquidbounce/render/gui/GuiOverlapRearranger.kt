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

import net.ccbluex.liquidbounce.render.engine.type.BoundingBox2f

/**
 * Pushes overlapping GUI elements apart so that they stay readable.
 *
 * Elements are laid out in the order they are given: the first one keeps its position and every later one is pushed
 * down until it clears whatever it ran into. Only the vertical axis is used, so an element never leaves the column it
 * belongs to.
 *
 * The order is the caller's on purpose. Deriving it from the positions, as sorting by y centre does, reverses
 * whenever two elements sit within a pixel of each other, which made stacked tags flip up and down on the slightest
 * camera movement. A caller that wants a specific element to keep its place simply puts it first.
 *
 * TODO: pushing down is unbounded, so a dense pile stacks past the bottom of the screen. Clamping would need the
 *  screen rectangle, and no caller has enough elements for it to matter yet.
 */
object GuiOverlapRearranger {

    fun rearrange(elements: List<GuiRearrangeable>) {
        if (elements.size <= 1) {
            return
        }

        val placed = ArrayList<GuiRearrangeable>(elements.size)
        for (element in elements) {
            element.bounds = settle(element.bounds, placed)
            placed += element
        }
    }

    /**
     * Moves [bounds] down until it collides with none of [placed].
     *
     * Pushing past the lowest blocker rather than the first one found guarantees progress, since the top edge then
     * strictly increases and that blocker is cleared outright. With finitely many blockers, this terminates.
     */
    private fun settle(bounds: BoundingBox2f, placed: List<GuiRearrangeable>): BoundingBox2f {
        var result = bounds

        while (true) {
            var lowestBlocker = Float.NaN

            for (element in placed) {
                val other = element.bounds
                if (other intersects result && (lowestBlocker.isNaN() || other.yMax > lowestBlocker)) {
                    lowestBlocker = other.yMax
                }
            }

            if (lowestBlocker.isNaN()) {
                return result
            }

            result = BoundingBox2f(result.xMin, lowestBlocker, result.xMax, lowestBlocker + result.height)
        }
    }

}
