/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.render.ultralight.renderer

import com.labymedia.ultralight.UltralightView
import com.labymedia.ultralight.bitmap.UltralightBitmapSurface
import com.labymedia.ultralight.config.UltralightViewConfig
import org.lwjgl.opengl.GL12.*
import java.nio.ByteBuffer

/**
 * A cpu renderer which is being supported on OpenGL functionality from version 1.2.
 */
class CpuViewRenderer : ViewRenderer {

    private var glTexture = -1

    override fun setupConfig(viewConfig: UltralightViewConfig) {
        // CPU rendering is not accelerated
        // viewConfig.isAccelerated(false)
    }

    override fun delete() {
        glDeleteTextures(glTexture)
        glTexture = -1
    }

    /**
     * Render the current view
     */
    override fun render(view: UltralightView) {
        if (glTexture == -1) {
            createGlTexture()
        }

        // As we are using the CPU renderer, draw with a bitmap (we did not set a custom surface)
        val surface = view.surface() as UltralightBitmapSurface
        val bitmap = surface.bitmap()
        val width = view.width().toInt()
        val height = view.height().toInt()

        // Prepare OpenGL for 2D textures and bind our texture
        glEnable(GL_TEXTURE_2D)
        glBindTexture(GL_TEXTURE_2D, glTexture)

        val dirtyBounds = surface.dirtyBounds()

        if (dirtyBounds.isValid) {
            val imageData = bitmap.lockPixels()

            glPixelStorei(GL_UNPACK_SKIP_ROWS, 0)
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0)
            glPixelStorei(GL_UNPACK_SKIP_IMAGES, 0)
            glPixelStorei(GL_UNPACK_ROW_LENGTH, bitmap.rowBytes().toInt() / 4)

            if (dirtyBounds.width() == width && dirtyBounds.height() == height) {
                // Update full image
                glTexImage2D(
                    GL_TEXTURE_2D,
                    0,
                    GL_RGBA8,
                    width,
                    height,
                    0,
                    GL_BGRA,
                    GL_UNSIGNED_INT_8_8_8_8_REV,
                    imageData
                )
            } else {
                // Update partial image
                val x = dirtyBounds.x()
                val y = dirtyBounds.y()
                val dirtyWidth = dirtyBounds.width()
                val dirtyHeight = dirtyBounds.height()
                val startOffset = (y * bitmap.rowBytes() + x * 4).toInt()

                glTexSubImage2D(
                    GL_TEXTURE_2D,
                    0,
                    x, y, dirtyWidth, dirtyHeight,
                    GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV,
                    imageData.position(startOffset) as ByteBuffer
                )
            }
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)

            bitmap.unlockPixels()
            surface.clearDirtyBounds()
        }

        // Set up the OpenGL state for rendering of a fullscreen quad
        glPushAttrib(GL_ENABLE_BIT or GL_COLOR_BUFFER_BIT or GL_TRANSFORM_BIT)
        glMatrixMode(GL_PROJECTION)
        glPushMatrix()
        glLoadIdentity()
        glOrtho(0.0, view.width().toDouble(), view.height().toDouble(), 0.0, -1.0, 1.0)
        glMatrixMode(GL_MODELVIEW)
        glPushMatrix()

        // Disable lighting and scissoring, they could mess up th renderer
        glLoadIdentity()
        glDisable(GL_LIGHTING)
        glDisable(GL_SCISSOR_TEST)
        glEnable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        // Make sure we draw with a neutral color
        // (so we don't mess with the color channels of the image)
        glColor4f(1f, 1f, 1f, 1f)
        glBegin(GL_QUADS)

        // Lower left corner, 0/0 on the screen space, and 0/0 of the image UV
        glTexCoord2i(0, 0)
        glVertex2f(0f, 0f)

        // Upper left corner
        glTexCoord2f(0f, 1f)
        glVertex2i(0, height)

        // Upper right corner
        glTexCoord2f(1f, 1f)
        glVertex2i(width, height)

        // Lower right corner
        glTexCoord2f(1f, 0f)
        glVertex2i(width, 0)
        glEnd()
        glBindTexture(GL_TEXTURE_2D, 0)

        // Restore OpenGL state
        glPopMatrix()
        glMatrixMode(GL_PROJECTION)
        glPopMatrix()
        glMatrixMode(GL_MODELVIEW)
        glDisable(GL_TEXTURE_2D)
        glPopAttrib()
    }

    /**
     * Sets up the OpenGL texture for rendering
     */
    private fun createGlTexture() {
        glEnable(GL_TEXTURE_2D)
        glTexture = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, glTexture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glBindTexture(GL_TEXTURE_2D, 0)
        glDisable(GL_TEXTURE_2D)
    }

}
