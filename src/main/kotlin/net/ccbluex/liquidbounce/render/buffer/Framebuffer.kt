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

package net.ccbluex.liquidbounce.render.buffer

import com.mojang.blaze3d.opengl.GlStateManager
import net.ccbluex.liquidbounce.common.GlobalFramebuffer
import net.ccbluex.liquidbounce.render.shader.shaders.BlitToScreenShader
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30.*
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.IntBuffer

open class Framebuffer(var width: Int, var height: Int, val useDepth: Boolean) : Closeable {

    val colorAttachment = GlStateManager._genTexture()
    private var depthAttachment = 0
    open val id = GlStateManager.glGenFramebuffers()
    private var clearR = 0f
    private var clearG = 0f
    private var clearB = 0f
    private var clearA = 0f

    init {
        GlobalFramebuffer.push(this)

        GlStateManager._bindTexture(colorAttachment)
        GlStateManager._texImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null)
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        GlStateManager._glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorAttachment, 0)

        if (useDepth) {
            depthAttachment = glGenRenderbuffers()
            glBindRenderbuffer(GL_RENDERBUFFER, depthAttachment)
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height)
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depthAttachment)
        }

        check(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE) {
            "Framebuffer is not complete! Status: ${glCheckFramebufferStatus(GL_FRAMEBUFFER)}"
        }

        GlobalFramebuffer.pop()
    }

    fun resize(width: Int, height: Int) {
        glBindTexture(GL_TEXTURE_2D, colorAttachment)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null as ByteBuffer?)

        glBindRenderbuffer(GL_RENDERBUFFER, depthAttachment)
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height)
    }

    fun beginWrite(viewport: Boolean, clear: Boolean = true) {
        GlobalFramebuffer.push(this)

        if (viewport) {
            GlStateManager._viewport(0, 0, width, height)
        }

        if (clear) {
            glClearColor(clearR, clearG, clearB, clearA)
            if (useDepth) {
                glClearDepth(1.0)
            }

            GlStateManager._clear(GL_COLOR_BUFFER_BIT or (if (useDepth) GL_DEPTH_BUFFER_BIT else 0))
        }
    }

    fun drawBlit() {
        BlitToScreenShader.blit(colorAttachment)
    }

    fun end() {
        GlobalFramebuffer.pop()
    }

    fun setClearColor(r: Float, g: Float, b: Float, a: Float) {
        clearR = r
        clearG = g
        clearB = b
        clearA = a
    }

    override fun close() {
        GlStateManager._glDeleteFramebuffers(id)
    }

}
