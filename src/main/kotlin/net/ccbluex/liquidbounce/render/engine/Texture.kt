/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

package net.ccbluex.liquidbounce.render.engine

import net.ccbluex.liquidbounce.render.engine.utils.GLIDGuard
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import java.awt.image.BufferedImage

/**
 * Handles a 2D OpenGL texture
 *
 * @constructor Can only be initialized from an OpenGL context
 */
class Texture() : GLIDGuard(GL11.glGenTextures(), GL11::glDeleteTextures) {
    private var deferredUpdate: BufferedImage? = null

    /**
     * Creates a texture and assign an image to it.
     */
    constructor(image: BufferedImage) : this() {
        deferredUpdate = image
    }

    /**
     * Binds this texture to the active sampler. If an update is pending, upload the
     * new image to it
     */
    fun bind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.id)

        val update = this.deferredUpdate

        // Is there an update pending?
        if (update != null) {
            upload(update)

            // Updated. No need to save the image
            this.deferredUpdate = null
        }
    }

    /**
     * Unlinks textures from the current sampler
     */
    fun unbind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
    }

    /**
     * Associates this texture with an image. The texture must be bound via [bind] before.
     *
     * @param image An image with the type [BufferedImage.TYPE_INT_ARGB]
     * @throws IllegalArgumentException If [image]'s type is invalid
     */
    private fun upload(image: BufferedImage) {
        val data = IntArray(image.width * image.height)

        image.getRGB(0, 0, image.width, image.height, data, 0, image.width)

        val buf = BufferUtils.createByteBuffer(data.size * 4)

        for (i in data.indices) {
            val rgba = data[i]

            buf.put(i * 4, ((rgba shr 16) and 255).toByte())
            buf.put(i * 4 + 1, ((rgba shr 8) and 255).toByte())
            buf.put(i * 4 + 2, (rgba and 255).toByte())
            buf.put(i * 4 + 3, ((rgba shr 24) and 255).toByte())
        }

        // We don't want mipmapping
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)

        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA,
            image.width,
            image.height,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            buf
        )
    }

}
