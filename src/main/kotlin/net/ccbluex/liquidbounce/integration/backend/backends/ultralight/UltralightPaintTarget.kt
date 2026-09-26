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
package net.ccbluex.liquidbounce.integration.backend.backends.ultralight

import net.ccbluex.liquidbounce.mcef.cef.MCEFRenderer
import net.janrupf.ujr.api.bitmap.UltralightBitmapSurface
import java.awt.Rectangle

/**
 * Uploads what Ultralight painted into a texture, the same way the pixels of CEF are uploaded.
 *
 * Ultralight paints BGRA pixels without padding between rows, as its config sets no bitmap alignment.
 */
internal class UltralightPaintTarget : MCEFRenderer(true) {

    private var paintedWidth = 0
    private var paintedHeight = 0

    init {
        initialize()
    }

    fun paint(surface: UltralightBitmapSurface) {
        val dirtyBounds = surface.dirtyBounds()
        if (dirtyBounds.isEmpty) {
            return
        }

        val width = surface.width().toInt()
        val height = surface.height().toInt()

        surface.lockPixels().use { pixels ->
            val buffer = pixels.asByteBuffer()

            if (width != paintedWidth || height != paintedHeight || textureSetup == null) {
                onPaint(buffer, width, height)
                paintedWidth = width
                paintedHeight = height
            } else {
                val dirty = Rectangle(dirtyBounds.x(), dirtyBounds.y(), dirtyBounds.width(), dirtyBounds.height())
                    .intersection(Rectangle(0, 0, width, height))

                if (!dirty.isEmpty) {
                    onPaint(buffer, width, height, arrayOf(dirty), 0, 0)
                }
            }
        }

        surface.clearDirtyBounds()
    }

}
