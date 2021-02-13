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
 *
 * and
 *
 * Ultralight Java - Java wrapper for the Ultralight web engine
 * Copyright (C) 2020 - 2021 LabyMedia and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.ccbluex.liquidbounce.renderer.ultralight

import com.labymedia.ultralight.UltralightJava
import com.labymedia.ultralight.UltralightPlatform
import com.labymedia.ultralight.UltralightRenderer
import com.labymedia.ultralight.UltralightView
import com.labymedia.ultralight.bitmap.UltralightBitmapSurface
import com.labymedia.ultralight.config.FontHinting
import com.labymedia.ultralight.config.UltralightConfig
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.renderer.ultralight.input.ClipboardAdapter
import net.ccbluex.liquidbounce.renderer.ultralight.input.CursorAdapter
import net.ccbluex.liquidbounce.renderer.ultralight.listener.ViewListener
import net.ccbluex.liquidbounce.renderer.ultralight.listener.ViewLoadListener
import net.ccbluex.liquidbounce.renderer.ultralight.support.ViewFileSystem
import net.ccbluex.liquidbounce.renderer.ultralight.support.ViewLogger
import net.ccbluex.liquidbounce.renderer.ultralight.theme.Page
import net.ccbluex.liquidbounce.utils.mc
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import java.io.File
import java.nio.ByteBuffer

class WebView(
    val window: Long = mc.window.handle,
    val scale: Float = 2.0f,
    deviceScale: Double = scale.toDouble(),
    var width: () -> Int,
    var height: () -> Int
) {

    var platform: UltralightPlatform
    var renderer: UltralightRenderer
    var view: UltralightView

    var currentPage: Page? = null

    private var glTexture = -1

    companion object {
        init {
            // Load natives from native directory inside root folder
            UltralightJava.load(File(LiquidBounce.configSystem.rootFolder, "natives").toPath())
        }
    }

    init {
        // Setup platform
        platform = UltralightPlatform.instance()
        platform.setConfig(
            UltralightConfig()
                .resourcePath("./resources/")
                .fontHinting(FontHinting.NORMAL)
                .deviceScale(deviceScale)
        )
        platform.usePlatformFontLoader()
        platform.setFileSystem(ViewFileSystem())
        platform.setLogger(ViewLogger())
        platform.setClipboard(ClipboardAdapter())

        // Setup renderer
        renderer = UltralightRenderer.create()
        renderer.logMemoryUsage()

        // Setup view
        view = renderer.createView(width().toLong() * scale.toLong(), height().toLong() * scale.toLong(), true)
        view.setViewListener(ViewListener(CursorAdapter(window)))
        view.setLoadListener(ViewLoadListener(view))
    }

    /**
     * Loads the specified [url]
     */
    fun loadPage(page: Page) {
        if (currentPage != page && currentPage != null) {
            page.close()
        }

        view.loadURL(page.viewableFile)
        currentPage = page
    }

    /**
     * Updates and renders the renderer
     */
    fun update() {
        val width = width()
        val height = height()
        if (width.toLong() * scale.toLong() != view.width() || height.toLong() * scale.toLong() != view.height()) {
            resize(width, height)
        }

        val page = currentPage
        if (page?.hasUpdate() == true) {
            loadPage(page)
        }

        renderer.update()
        renderer.render()
    }

    /**
     * Resizes the web view
     *
     * @param width  The new view width
     * @param height The new view height
     */
    fun resize(width: Int, height: Int) {
        println("resize $width ${view.width()}, $height ${view.height()}")
        view.resize(width.toLong() * scale.toLong(), height.toLong() * scale.toLong())
    }

    /**
     * Render the current image using OpenGL
     */
    fun render() {
        if (glTexture == -1) {
            createGLTexture()
        }

        // As we are using the CPU renderer, draw with a bitmap (we did not set a custom surface)
        val surface = view.surface() as UltralightBitmapSurface
        val bitmap = surface.bitmap()
        val width = view.width().toInt()
        val height = view.height().toInt()

        // Prepare OpenGL for 2D textures and bind our texture
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTexture)
        val dirtyBounds = surface.dirtyBounds()
        if (dirtyBounds.isValid) {
            val imageData = bitmap.lockPixels()
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, bitmap.rowBytes().toInt() / 4)
            if (dirtyBounds.width() == width && dirtyBounds.height() == height) {
                // Update full image
                GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA8,
                    width,
                    height,
                    0,
                    GL12.GL_BGRA,
                    GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
                    imageData
                )
                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0)
            } else {
                // Update partial image
                val x = dirtyBounds.x()
                val y = dirtyBounds.y()
                val dirtyWidth = dirtyBounds.width()
                val dirtyHeight = dirtyBounds.height()
                val startOffset = (y * bitmap.rowBytes() + x * 4).toInt()
                GL11.glTexSubImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    x, y, dirtyWidth, dirtyHeight,
                    GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
                    imageData.position(startOffset) as ByteBuffer
                )
            }
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0)
            bitmap.unlockPixels()
            surface.clearDirtyBounds()
        }

        // Set up the OpenGL state for rendering of a fullscreen quad
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT or GL11.GL_COLOR_BUFFER_BIT or GL11.GL_TRANSFORM_BIT)
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        GL11.glOrtho(0.0, view.width().toDouble(), view.height().toDouble(), 0.0, -1.0, 1.0)
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPushMatrix()

        // Disable lighting and scissoring, they could mess up th renderer
        GL11.glLoadIdentity()
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        // Make sure we draw with a neutral color
        // (so we don't mess with the color channels of the image)
        GL11.glColor4f(1f, 1f, 1f, 1f)
        GL11.glBegin(GL11.GL_QUADS)

        // Lower left corner, 0/0 on the screen space, and 0/0 of the image UV
        GL11.glTexCoord2i(0, 0)
        GL11.glVertex2f(0f, 0f)

        // Upper left corner
        GL11.glTexCoord2f(0f, 1f)
        GL11.glVertex2i(0, height)

        // Upper right corner
        GL11.glTexCoord2f(1f, 1f)
        GL11.glVertex2i(width, height)

        // Lower right corner
        GL11.glTexCoord2f(1f, 0f)
        GL11.glVertex2i(width, 0)
        GL11.glEnd()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)

        // Restore OpenGL state
        GL11.glPopMatrix()
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPopMatrix()
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glPopAttrib()
    }

    /**
     * Sets up the OpenGL texture for rendering
     */
    private fun createGLTexture() {
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        glTexture = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTexture)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
    }

}
