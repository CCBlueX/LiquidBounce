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
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.render.engine.font.BoundingBox2f
import net.ccbluex.liquidbounce.utils.client.mc

class Alignment(
    horizontalAlignment: ScreenAxisX,
    horizontalOffset: Int,
    verticalAlignment: ScreenAxisY,
    verticalOffset: Int,
) : Configurable("Alignment") {

    companion object {
        @JvmStatic
        fun center() = Alignment(ScreenAxisX.CENTER, 0, ScreenAxisY.CENTER, 0)
    }

    val horizontalAlignment by enumChoice("Horizontal", horizontalAlignment)
    val horizontalOffset by int("HorizontalOffset", horizontalOffset, -1000..1000)
    val verticalAlignment by enumChoice("Vertical", verticalAlignment)
    val verticalOffset by int("VerticalOffset", verticalOffset, -1000..1000)

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
     * Converts the alignement configurable to style (CSS)
     */
    fun toStyle() = """
        position: fixed;
        ${when (horizontalAlignment) {
        ScreenAxisX.LEFT -> "left: ${horizontalOffset}px"
        ScreenAxisX.RIGHT -> "right: ${horizontalOffset}px"
        ScreenAxisX.CENTER -> "left: calc(50% + ${horizontalOffset}px)"
        ScreenAxisX.CENTER_TRANSLATED -> "left: calc(50% + ${horizontalOffset}px)"
    }};
        ${when (verticalAlignment) {
        ScreenAxisY.TOP -> "top: ${verticalOffset}px"
        ScreenAxisY.BOTTOM -> "bottom: ${verticalOffset}px"
        ScreenAxisY.CENTER -> "top: calc(50% + ${verticalOffset}px)"
        ScreenAxisY.CENTER_TRANSLATED -> "top: calc(50% + ${verticalOffset}px)"
    }};
        transform: translate(
            ${if (horizontalAlignment == ScreenAxisX.CENTER_TRANSLATED) "-50%" else "0"},
            ${if (verticalAlignment == ScreenAxisY.CENTER_TRANSLATED) "-50%" else "0"}
        );
    """.trimIndent().replace("\n", "")

}
