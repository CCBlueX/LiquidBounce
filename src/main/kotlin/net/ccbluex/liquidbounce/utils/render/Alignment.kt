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
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.render.engine.type.BoundingBox2f
import net.ccbluex.liquidbounce.utils.client.mc

class Alignment(
    horizontalAlignment: ScreenAxisX,
    horizontalOffset: Int,
    verticalAlignment: ScreenAxisY,
    verticalOffset: Int,
) : ValueGroup("Alignment") {

    companion object {
        @JvmStatic
        fun center() = Alignment(ScreenAxisX.CENTER, 0, ScreenAxisY.CENTER, 0)
    }

    var horizontalAlignment by enumChoice("Horizontal", horizontalAlignment)
        private set

    var horizontalOffset by int("HorizontalOffset", horizontalOffset, -1000..1000, suffix = "px")
        private set

    val guiScaledHorizontalOffset get() = horizontalOffset.toFloat() / mc.window.guiScale

    var verticalAlignment by enumChoice("Vertical", verticalAlignment)
        private set

    var verticalOffset by int("VerticalOffset", verticalOffset, -1000..1000, suffix = "px")
        private set

    val guiScaledVerticalOffset get() = verticalOffset.toFloat() / mc.window.guiScale

    fun setFrom(other: Alignment) {
        this.horizontalAlignment = other.horizontalAlignment
        this.horizontalOffset = other.horizontalOffset
        this.verticalAlignment = other.verticalAlignment
        this.verticalOffset = other.verticalOffset
    }

    /**
     * @return Scaled bounds follows [com.mojang.blaze3d.platform.Window.guiScale]
     */
    fun getBounds(
        width: Float,
        height: Float,
    ): BoundingBox2f {
        val screenWidth = mc.window.guiScaledWidth.toFloat()
        val screenHeight = mc.window.guiScaledHeight.toFloat()

        val guiScaledHorizontalOffset = this.guiScaledHorizontalOffset
        val x = when (horizontalAlignment) {
            ScreenAxisX.LEFT -> guiScaledHorizontalOffset
            ScreenAxisX.CENTER_TRANSLATED -> screenWidth / 2f - width / 2f + guiScaledHorizontalOffset
            ScreenAxisX.RIGHT -> screenWidth - width - guiScaledHorizontalOffset
            ScreenAxisX.CENTER -> screenWidth / 2f + guiScaledHorizontalOffset
        }

        val guiScaledVerticalOffset = this.guiScaledVerticalOffset
        val y = when (verticalAlignment) {
            ScreenAxisY.TOP -> guiScaledVerticalOffset
            ScreenAxisY.CENTER_TRANSLATED -> screenHeight / 2f - height / 2f + guiScaledVerticalOffset
            ScreenAxisY.BOTTOM -> screenHeight - height - guiScaledVerticalOffset
            ScreenAxisY.CENTER -> screenHeight / 2f + guiScaledVerticalOffset
        }

        return BoundingBox2f(x, y, x + width, y + height)
    }

    enum class ScreenAxisX(override val tag: String) : Tagged {
        LEFT("Left"),
        CENTER("Center"),
        CENTER_TRANSLATED("CenterTranslated"),
        RIGHT("Right"),
    }

    enum class ScreenAxisY(override val tag: String) : Tagged {
        TOP("Top"),
        CENTER("Center"),
        CENTER_TRANSLATED("CenterTranslated"),
        BOTTOM("Bottom"),
    }

}
