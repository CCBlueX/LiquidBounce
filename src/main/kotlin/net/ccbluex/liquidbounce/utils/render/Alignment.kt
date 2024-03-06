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
                ScreenAxisX.CENTER_TRANSLATED -> screenWidth / 2f - width / 2f - horizontalPadding.toFloat()
                ScreenAxisX.RIGHT -> screenWidth - width - horizontalPadding.toFloat()
                ScreenAxisX.CENTER -> screenWidth / 2f - width / 2f
            }

        val y =
            when (verticalAlignment) {
                ScreenAxisY.TOP -> verticalPadding.toFloat()
                ScreenAxisY.CENTER_TRANSLATED -> screenHeight / 2f - height / 2f - verticalPadding.toFloat()
                ScreenAxisY.BOTTOM -> screenHeight - height - verticalPadding.toFloat()
                ScreenAxisY.CENTER -> screenWidth / 2f - height / 2f
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
     * Converts the alignement configurable to style (CSS)
     */
    fun toStyle() = """
        position: fixed;
        ${when (horizontalAlignment) {
            ScreenAxisX.LEFT -> "left: 0"
            ScreenAxisX.RIGHT -> "right: 0"
            ScreenAxisX.CENTER -> "left: 50%"
            ScreenAxisX.CENTER_TRANSLATED -> "left: 50%"
    }};
        ${when (verticalAlignment) {
            ScreenAxisY.TOP -> "top: 0"
            ScreenAxisY.BOTTOM -> "bottom: 0"
            ScreenAxisY.CENTER -> "top: 50%"
            ScreenAxisY.CENTER_TRANSLATED -> "top: 50%"
    }};
        transform: translate(
            ${if (horizontalAlignment == ScreenAxisX.CENTER_TRANSLATED) "calc(-50% + ${horizontalPadding}px)" else "${horizontalPadding}px"},
            ${if (verticalAlignment == ScreenAxisY.CENTER_TRANSLATED) "calc(-50% + ${verticalPadding}px)" else "${verticalPadding}px"}
        );
    """.trimIndent().replace("\n", "")

}
