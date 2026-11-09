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

import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.theme.component.HudComponent
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.render.engine.type.BoundingBox2f
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.minecraft.client.gui.GuiGraphicsExtractor

@AddonApi
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

    /**
     * [ModuleHud.scale], the size of the component relative to [guiScaledWidth] and [guiScaledHeight].
     */
    val hudScale: Float get() = ModuleHud.scale

    val width: Float get() = guiScaledWidth * hudScale * WEB_HUD_BASE_SCALE

    val height: Float get() = guiScaledHeight * hudScale * WEB_HUD_BASE_SCALE

    /**
     * Where the component is on screen, [width] by [height] grown by [hudScale].
     *
     * Drawing that fills these bounds scales with them; [withHudScale] scales any other drawing.
     */
    protected fun getGuiScaledBounds(
        width: Float = guiScaledWidth,
        height: Float = guiScaledHeight,
    ): BoundingBox2f {
        val scaledWidth = width * hudScale
        val scaledHeight = height * hudScale
        val screenWidth = mc.window.guiScaledWidth.toFloat()
        val screenHeight = mc.window.guiScaledHeight.toFloat()
        val horizontalOffset = alignment.horizontalOffset / WEB_HUD_BASE_SCALE
        val verticalOffset = alignment.verticalOffset / WEB_HUD_BASE_SCALE

        val x = when (alignment.horizontalAlignment) {
            Alignment.ScreenAxisX.LEFT -> horizontalOffset
            Alignment.ScreenAxisX.CENTER_TRANSLATED -> screenWidth / 2f - scaledWidth / 2f + horizontalOffset
            Alignment.ScreenAxisX.RIGHT -> screenWidth - scaledWidth - horizontalOffset
            Alignment.ScreenAxisX.CENTER -> screenWidth / 2f + horizontalOffset
        }

        val y = when (alignment.verticalAlignment) {
            Alignment.ScreenAxisY.TOP -> verticalOffset
            Alignment.ScreenAxisY.CENTER_TRANSLATED -> screenHeight / 2f - scaledHeight / 2f + verticalOffset
            Alignment.ScreenAxisY.BOTTOM -> screenHeight - scaledHeight - verticalOffset
            Alignment.ScreenAxisY.CENTER -> screenHeight / 2f + verticalOffset
        }

        return BoundingBox2f(x, y, x + scaledWidth, y + scaledHeight)
    }

    /**
     * Draws [block] zoomed by [hudScale] around the top left corner of [bounds], from [getGuiScaledBounds].
     * [block] gets the bounds at their unscaled size, so it can draw in the component's own pixels.
     */
    protected inline fun GuiGraphicsExtractor.withHudScale(
        bounds: BoundingBox2f,
        block: GuiGraphicsExtractor.(BoundingBox2f) -> Unit,
    ) {
        val scale = hudScale
        val unscaled = BoundingBox2f(
            bounds.xMin,
            bounds.yMin,
            bounds.xMin + (bounds.xMax - bounds.xMin) / scale,
            bounds.yMin + (bounds.yMax - bounds.yMin) / scale,
        )
        pose().withPush {
            translate(bounds.xMin, bounds.yMin)
            scale(scale)
            translate(-bounds.xMin, -bounds.yMin)
            block(unscaled)
        }
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
