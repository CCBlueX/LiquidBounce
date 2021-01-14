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
package net.ccbluex.liquidbounce.sciter.natives

import net.ccbluex.liquidbounce.sciter.natives.SciterNative.render0
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

object SciterNative {
    @JvmStatic
    external fun init0(windowHandle: Long, libraryLocation: String)
    @JvmStatic
    external fun loadHTML0(windowHandle: Long, html: String, uri: String?)
    @JvmStatic
    external fun render0(windowHandle: Long, framebuffer_pointer: Long, framebuffer_size: Int)
    @JvmStatic
    external fun destroy0(windowHandle: Long)
    @JvmStatic
    external fun draw0(windowHandle: Long)
    @JvmStatic
    external fun mouseButtonEvent0(
        windowHandle: Long,
        x: Int,
        y: Int,
        keyboardState: Int,
        button: Int,
        released: Boolean
    )

    @JvmStatic
    external fun mouseEvent0(windowHandle: Long, x: Int, y: Int, button: Int)
    @JvmStatic
    external fun keyEvent0(windowHandle: Long, scancode: Int, keyboardState: Int, eventType: Int)
    @JvmStatic
    external fun heartbit0(windowHandle: Long, timeDelta: Int)
    @JvmStatic
    external fun setResolution0(windowHandle: Long, ppi: Int)
    @JvmStatic
    external fun setSize0(windowHandle: Long, width: Int, height: Int)
}

class Sciter(private val windowHandle: Long) {

    val textureID = GL11.glGenTextures()
    private var startTime = 0L
    private var initialized = false
    private var currentWidth = 0
    private var currentHeight = 0
    private var framebuffer: ByteBuffer? = null


    init {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID) //Bind texture ID
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0) //Bind texture ID
    }

    /**
     * Must be called before any other method is called
     *
     * @param libraryLocation Must be a path to the sciter dynamic library
     */
    fun init(libraryLocation: String) {
        check(!initialized) { "Sciter already initialized" }

        SciterNative.init0(windowHandle, libraryLocation)

        startTime = System.currentTimeMillis()
        initialized = true
    }

    /**
     * Called on every tick
     */
    fun heartBit() {
        checkInitialized()

        SciterNative.heartbit0(windowHandle, (System.currentTimeMillis() - startTime).toInt())
    }

    fun setResolution(resolution: Int) {
        checkInitialized()

        SciterNative.setResolution0(windowHandle, resolution)
    }

    fun setSize(width: Int, height: Int) {
        checkInitialized()

        // Don't send an update to sciter if there is no update
        if (currentWidth == width && currentHeight == height)
            return

        SciterNative.setSize0(windowHandle, width, height)

        currentWidth = width
        currentHeight = height

        // Reallocate framebuffer
        allocateFramebuffer()
    }

    private fun allocateFramebuffer() {
        framebuffer = BufferUtils.createByteBuffer(currentWidth * currentHeight * 4)
    }

    fun mouseMoved(x: Int, y: Int, button: Int) {
        checkInitialized()
        SciterNative.mouseEvent0(windowHandle, x, y, button)
    }

    fun mouseButtonEvent(x: Int, y: Int, button: Int, released: Boolean, keyboardState: Int) {
        checkInitialized()
        SciterNative.mouseButtonEvent0(windowHandle, x, y, keyboardState, button, released)
    }

    fun keyEvent(scancode: Int, keyboardState: Int, eventType: Int) {
        checkInitialized()
        SciterNative.keyEvent0(windowHandle, scancode, keyboardState, eventType)
    }

    fun render() {
        // causing crashes
        // draw0(windowHandle)
        checkInitialized()
        render0(windowHandle, MemoryUtil.memAddress(framebuffer!!), currentWidth * currentHeight * 4)

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID)
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA8,
            currentWidth,
            currentHeight,
            0,
            GL12.GL_BGRA,
            GL11.GL_UNSIGNED_BYTE,
            framebuffer
        )
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
    }

    /**
     * Loads an HTML page
     *
     * @param html HTML-Data
     * @param uri URI of the content, may be null
     */
    fun loadHtml(html: String, uri: String?) = SciterNative.loadHTML0(windowHandle, html, uri)

    /**
     * Throws an [IllegalStateException] if sciter is not initialized yet
     */
    private fun checkInitialized() {
        check(initialized) { "not initialized yet" }
    }
}
