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

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.render.engine.font.BoundingBox2f
import net.ccbluex.liquidbounce.utils.client.mc

class Alignment(
    horizontalAlignment: ScreenAxisX,
    horizontalPadding: Int,
    verticalAlignment: ScreenAxisY,
    verticalPadding: Int,
) : Configurable("Alignment") {

    val horizontalAlignment by enumChoice("Horizontal", horizontalAlignment)
    val horizontalPadding by int("HorizontalPadding", horizontalPadding, -256..256)
    val verticalAlignment by enumChoice("Vertical", verticalAlignment)
    val verticalPadding by int("VerticalPadding", verticalPadding, -256..256)

    fun getBounds(
        width: Float,
        height: Float,
    ): BoundingBox2f {
        val screenWidth = mc.window.scaledWidth.toFloat()
        val screenHeight = mc.window.scaledHeight.toFloat()

        val x =
            when (horizontalAlignment) {
                ScreenAxisX.LEFT -> horizontalPadding.toFloat()
                ScreenAxisX.CENTER -> screenWidth / 2f - width / 2f - horizontalPadding.toFloat()
                ScreenAxisX.RIGHT -> screenWidth - width - horizontalPadding.toFloat()
            }

        val y =
            when (verticalAlignment) {
                ScreenAxisY.TOP -> verticalPadding.toFloat()
                ScreenAxisY.CENTER -> screenHeight / 2f - height / 2f - verticalPadding.toFloat()
                ScreenAxisY.BOTTOM -> screenHeight - height - verticalPadding.toFloat()
            }

        return BoundingBox2f(x, y, x + width, y + height)
    }

    enum class ScreenAxisX(override val choiceName: String) : NamedChoice {
        LEFT("Left"),
        CENTER("Center"),
        RIGHT("Right"),
    }

    enum class ScreenAxisY(override val choiceName: String) : NamedChoice {
        TOP("Top"),
        CENTER("Center"),
        BOTTOM("Bottom"),
    }

    /**
     * Converts the alignement configurable to style (CSS)
     */
    fun toStyle() = """
        position: fixed;
        ${when (horizontalAlignment) {
            ScreenAxisX.LEFT -> "left: ${horizontalPadding}px"
            ScreenAxisX.RIGHT -> "right: ${horizontalPadding}px"
            ScreenAxisX.CENTER -> "left: calc(50% + ${horizontalPadding}px)"
        }};
        ${when (verticalAlignment) {
            ScreenAxisY.TOP -> "top: ${verticalPadding}px"
            ScreenAxisY.BOTTOM -> "bottom: ${verticalPadding}px"
            ScreenAxisY.CENTER -> "top: calc(50% + ${verticalPadding}px)"
        }};
        transform: translate(
            ${if (horizontalAlignment == ScreenAxisX.CENTER) "-50%" else "0"},
            ${if (verticalAlignment == ScreenAxisY.CENTER) "-50%" else "0"}
        );
    """.trimIndent().replace("\n", "")

}
