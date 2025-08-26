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
package net.ccbluex.liquidbounce.render.shader

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gl.GlUniform
import org.joml.Matrix4f
import org.lwjgl.opengl.GL20
import org.lwjgl.system.MemoryUtil

private val BUFFER = MemoryUtil.memAllocFloat(16)
var currentProjectionMatrix: Matrix4f? = null
    get() = field ?: RenderSystem.getProjectionMatrix()
var currentModelViewMatrix: Matrix4f? = null
    get() = field ?: RenderSystem.getModelViewMatrix()

val ProjMatUniform = UniformProvider("ProjMat") { pointer ->
    BUFFER.position(0)
    currentProjectionMatrix?.get(BUFFER)
    GL20.glUniformMatrix4fv(pointer, false, BUFFER)
    currentProjectionMatrix = null
}

val ModelViewMatUniform = UniformProvider("ModelViewMat") { pointer ->
    BUFFER.position(0)
    currentModelViewMatrix?.get(BUFFER)
    GL20.glUniformMatrix4fv(pointer, false, BUFFER)
    currentModelViewMatrix = null
}

class UniformProvider(val name: String, val set: (pointer: Int) -> Unit) {

    var pointer = -1

    fun init(program: Int) {
        pointer = GlUniform.getUniformLocation(program, name)
    }

}
