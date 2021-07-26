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
import com.labymedia.ultralight.config.UltralightViewConfig
import com.labymedia.ultralight.gpu.UltralightOpenGLGPUDriverNative
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.util.math.MatrixStack
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.glUseProgram
import org.lwjgl.opengl.GL30

/**
 * A gpu view renderer
 *
 * todo: fix
 */
class GpuViewRenderer : ViewRenderer {

    var window = 0L
    lateinit var driver: UltralightOpenGLGPUDriverNative

    override fun setupConfig(viewConfig: UltralightViewConfig) {
        viewConfig.isAccelerated(true)

        // todo: might use alternative context
        window = mc.window.handle
        driver = UltralightOpenGLGPUDriverNative(window, true)
    }

    override fun render(view: UltralightView, matrices: MatrixStack) {
        driver.setActiveWindow(window)
        glfwMakeContextCurrent(window)
        glPushAttrib(GL_ENABLE_BIT or GL_COLOR_BUFFER_BIT or GL_TRANSFORM_BIT)

        if (driver.hasCommandsPending()) {
            // GLFW.glfwMakeContextCurrent(this.window);
            driver.drawCommandList()
            // GLFW.glfwSwapBuffers(this.window);
        }

        glPopAttrib()

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0)

        this.renderHtmlTexture(view, window)
        glfwMakeContextCurrent(window)

    }

    private fun renderHtmlTexture(view: UltralightView, window: Long) {
        driver.setActiveWindow(window)
        val text = view.renderTarget().textureId
        val width = view.width().toInt()
        val height = view.height().toInt()
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        // Set up the OpenGL state for rendering of a fullscreen quad
        glPushAttrib(GL_ENABLE_BIT or GL_COLOR_BUFFER_BIT or GL_TRANSFORM_BIT)
        driver.bindTexture(0, text)
        glUseProgram(0)
        glMatrixMode(GL_PROJECTION)
        glPushMatrix()
        glLoadIdentity()
        glOrtho(0.0, view.width().toDouble(), view.height().toDouble(), 0.0, -1.0, 1.0)
        glMatrixMode(GL_MODELVIEW)
        glPushMatrix()
        // Disable lighting and scissoring, they could mess up th renderer
        glLoadIdentity()
        glDisable(GL_SCISSOR_TEST)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        // Make sure we draw with a neutral color
        // (so we don't mess with the color channels of the image)
        glColor4f(1f, 1f, 1f, 1f)
        glBegin(GL_QUADS)

        // Lower left corner, 0/0 on the screen space, and 0/0 of the image UV
        glTexCoord2f(0f, 0f)
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
        glPopAttrib()
    }

    override fun delete() {
    }

}
