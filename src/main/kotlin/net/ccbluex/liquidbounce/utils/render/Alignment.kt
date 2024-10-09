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
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.render.engine.font.BoundingBox2f
import net.ccbluex.liquidbounce.utils.client.mc

data class Alignment(
    var horizontalAlignment: ScreenAxisX,
    var horizontalOffset: Int,
    var verticalAlignment: ScreenAxisY,
    var verticalOffset: Int,
) {

    constructor() : this(ScreenAxisX.LEFT, 0, ScreenAxisY.TOP, 0)

    fun getBounds(
        width: Float,
        height: Float,
    ): BoundingBox2f {
        val screenWidth = mc.window.scaledWidth.toFloat()
        val screenHeight = mc.window.scaledHeight.toFloat()

        val x =
            when (horizontalAlignment) {
                ScreenAxisX.LEFT -> horizontalOffset.toFloat()
                ScreenAxisX.CENTER_TRANSLATED -> screenWidth / 2f - width / 2f + horizontalOffset.toFloat()
                ScreenAxisX.RIGHT -> screenWidth - width - horizontalOffset.toFloat()
                ScreenAxisX.CENTER -> screenWidth / 2f - width / 2f + horizontalOffset.toFloat()
            }

        val y =
            when (verticalAlignment) {
                ScreenAxisY.TOP -> verticalOffset.toFloat()
                ScreenAxisY.CENTER_TRANSLATED -> screenHeight / 2f - height / 2f + verticalOffset.toFloat()
                ScreenAxisY.BOTTOM -> screenHeight - height - verticalOffset.toFloat()
                ScreenAxisY.CENTER -> screenWidth / 2f - height / 2f + verticalOffset.toFloat()
            }

        return BoundingBox2f(x, y, x + width, y + height)
    }

    enum class ScreenAxisX(override val choiceName: String) : NamedChoice {
        LEFT("Left"),
        CENTER("Center"),
        CENTER_TRANSLATED("CenterTranslated"),
        RIGHT("Right"),
    }

    enum class ScreenAxisY(override val choiceName: String) : NamedChoice {
        TOP("Top"),
        CENTER("Center"),
        CENTER_TRANSLATED("CenterTranslated"),
        BOTTOM("Bottom"),
    }

    /**
     * Checks if the given point is inside the bounds of the alignment
     */
    fun contains(x: Float, y: Float, width: Float, height: Float): Boolean {
        val bounds = getBounds(width, height)
        return x >= bounds.xMin && x <= bounds.xMax && y >= bounds.yMin && y <= bounds.yMax
    }

    fun move(offsetX: Int, offsetY: Int) {
        horizontalOffset += when (horizontalAlignment) {
            ScreenAxisX.RIGHT -> -offsetX
            else -> offsetX
        }

        verticalOffset += when (verticalAlignment) {
            ScreenAxisY.BOTTOM -> -offsetY
            else -> offsetY
        }
    }
}
