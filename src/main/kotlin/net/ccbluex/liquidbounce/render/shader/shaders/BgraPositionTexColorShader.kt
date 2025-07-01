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
package net.ccbluex.liquidbounce.render.shader.shaders

import net.ccbluex.liquidbounce.render.shader.ModelViewMatUniform
import net.ccbluex.liquidbounce.render.shader.ProjMatUniform
import net.ccbluex.liquidbounce.render.shader.Shader
import net.ccbluex.liquidbounce.render.shader.UniformProvider
import net.ccbluex.liquidbounce.utils.io.resourceToString
import org.lwjgl.opengl.GL20

object BgraPositionTexColorShader : Shader(
    resourceToString("/resources/liquidbounce/shaders/position_tex_color.vert"),
    resourceToString("/resources/liquidbounce/shaders/bgra_position_tex_color.frag"),
    arrayOf(
        UniformProvider("Sampler0") { pointer -> GL20.glUniform1i(pointer, 0) },
        ModelViewMatUniform,
        ProjMatUniform,
        UniformProvider("ColorModulator") { pointer ->
            GL20.glUniform4f(pointer, 1.0f, 1.0f, 1.0f, 1.0f)
        }
    )
)
