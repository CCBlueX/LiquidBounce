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

import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import it.unimi.dsi.fastutil.floats.Float2IntFunction
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.render.uploadRect
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.Mth

/**
 * Shared LUT atlas used by GUI circle shader rendering.
 *
 * Each requested circle writes one row of angle->color values into a [DynamicTexture].
 * The shader then samples that row by index to reproduce the caller-provided [Float2IntFunction].
 */
object GuiCircleLutAtlas {

    private const val LUT_WIDTH = 256
    private const val INITIAL_ROWS = 16

    private val sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR)

    private var texture = DynamicTexture(
        { "GuiCircleLutAtlas ${LUT_WIDTH}x$INITIAL_ROWS" },
        NativeImage(LUT_WIDTH, INITIAL_ROWS, false),
    )
    private var pixels = requireNotNull(texture.pixels)
    private var rows = INITIAL_ROWS
    private var nextRow = 0
    private var textureSetup = TextureSetup.singleTexture(texture.textureView, sampler)

    /**
     * Result of one LUT allocation for a single circle draw.
     *
     * @property row Row index in the LUT texture.
     * @property textureSetup Texture binding that should be passed to GUI render state.
     */
    @JvmRecord
    data class Allocation(
        val row: Int,
        val textureSetup: TextureSetup,
    )

    /**
     * Allocates one LUT row and uploads colors produced by [colorGetter].
     *
     * [colorGetter] receives angle in radians in range `[0, 2π)`.
     */
    fun allocate(colorGetter: Float2IntFunction): Allocation {
        if (nextRow >= rows) {
            growRows(nextRow + 1)
        }

        val row = nextRow++
        for (x in 0 until LUT_WIDTH) {
            val angle = x.toFloat() / LUT_WIDTH.toFloat() * Mth.TWO_PI
            pixels.setPixel(x, row, colorGetter.get(angle))
        }
        texture.uploadRect(0, 0, row, LUT_WIDTH, 1)

        return Allocation(row, textureSetup)
    }

    /**
     * Resets row allocation cursor for the next GUI draw pass.
     */
    fun resetForNextDraw() {
        nextRow = 0
    }

    private fun growRows(minRows: Int) {
        val oldRows = rows
        var newRows = oldRows
        while (newRows < minRows) {
            newRows *= 2
        }

        logger.info("GUI circle LUT atlas grown from $oldRows to $newRows rows")

        val oldTexture = texture
        val oldPixels = pixels
        val newPixels = NativeImage(LUT_WIDTH, newRows, false)
        for (y in 0 until oldRows) {
            for (x in 0 until LUT_WIDTH) {
                newPixels.setPixel(x, y, oldPixels.getPixel(x, y))
            }
        }

        texture = DynamicTexture(
            { "GuiCircleLutAtlas ${LUT_WIDTH}x$newRows" },
            newPixels,
        )
        pixels = requireNotNull(texture.pixels)
        rows = newRows
        textureSetup = TextureSetup.singleTexture(texture.textureView, sampler)
        oldTexture.close()
    }
}
