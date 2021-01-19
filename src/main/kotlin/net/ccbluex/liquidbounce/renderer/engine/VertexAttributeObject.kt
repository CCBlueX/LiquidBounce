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

package net.ccbluex.liquidbounce.renderer.engine

import org.lwjgl.opengl.GL32

/**
 * A wrapper for VAOs
 */
class VertexAttributeObject {
    /**
     * OpenGL's id for the buffer
     */
    val id: Int

    init {
        // Get an id for our VBO
        id = GL32.glGenVertexArrays()
    }

    /**
     * Binds this buffer
     */
    fun bind() {
        GL32.glBindVertexArray(id)
    }

    /**
     * Unbinds all buffers for this target
     */
    fun unbind() {
        GL32.glBindVertexArray(0)
    }

    /**
     * Deletes the buffer
     */
    fun delete() {
        GL32.glDeleteVertexArrays(id)
    }

    fun finalize() {
        val id = this.id

        RenderEngine.runOnGlContext {
            GL32.glDeleteVertexArrays(id)
        }
    }
}
