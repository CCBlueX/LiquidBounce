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

package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.theme.component.HudComponent
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.render.engine.type.BoundingBox2f
import net.ccbluex.liquidbounce.utils.render.Alignment

abstract class NativeHudComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<HudComponentTweak> = emptyArray(),
    description: String = "",
) : HudComponent(name, enabled, alignment, tweaks, description) {

    /**
     * @see com.mojang.blaze3d.platform.Window.guiScaledWidth
     */
    abstract val guiScaledWidth: Float

    /**
     * @see com.mojang.blaze3d.platform.Window.guiScaledHeight
     */
    abstract val guiScaledHeight: Float

    val width: Float get() = guiScaledWidth * WEB_HUD_BASE_SCALE

    val height: Float get() = guiScaledHeight * WEB_HUD_BASE_SCALE

    protected fun getGuiScaledBounds(
        width: Float = guiScaledWidth,
        height: Float = guiScaledHeight,
    ): BoundingBox2f {
        val screenWidth = mc.window.guiScaledWidth.toFloat()
        val screenHeight = mc.window.guiScaledHeight.toFloat()
        val horizontalOffset = alignment.horizontalOffset / WEB_HUD_BASE_SCALE
        val verticalOffset = alignment.verticalOffset / WEB_HUD_BASE_SCALE

        val x = when (alignment.horizontalAlignment) {
            Alignment.ScreenAxisX.LEFT -> horizontalOffset
            Alignment.ScreenAxisX.CENTER_TRANSLATED -> screenWidth / 2f - width / 2f + horizontalOffset
            Alignment.ScreenAxisX.RIGHT -> screenWidth - width - horizontalOffset
            Alignment.ScreenAxisX.CENTER -> screenWidth / 2f + horizontalOffset
        }

        val y = when (alignment.verticalAlignment) {
            Alignment.ScreenAxisY.TOP -> verticalOffset
            Alignment.ScreenAxisY.CENTER_TRANSLATED -> screenHeight / 2f - height / 2f + verticalOffset
            Alignment.ScreenAxisY.BOTTOM -> screenHeight - height - verticalOffset
            Alignment.ScreenAxisY.CENTER -> screenHeight / 2f + verticalOffset
        }

        return BoundingBox2f(x, y, x + width, y + height)
    }

    private companion object {
        /**
         * The browser HUD uses GUI scale 2 as its layout coordinate space and then zooms by
         * currentGuiScale / 2. Native components have to expose their editor dimensions and
         * interpret stored offsets in that same coordinate space.
         */
        const val WEB_HUD_BASE_SCALE = 2f
    }

}
