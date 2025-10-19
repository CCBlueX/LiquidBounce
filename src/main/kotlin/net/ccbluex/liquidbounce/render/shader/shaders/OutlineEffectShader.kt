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
 *
 */

package net.ccbluex.liquidbounce.render.shader.shaders

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.shader.FramebufferShader
import net.ccbluex.liquidbounce.render.shader.Shader
import net.ccbluex.liquidbounce.render.shader.UniformProvider
import net.ccbluex.liquidbounce.utils.io.resourceToString
import org.lwjgl.opengl.GL20

object OutlineEffectShaderData  {
    var blendColor = Color4b.WHITE
    var sampleMul = 1f
    var glowColor = Color4b.BLUE
    var falloff = 1f
    var layerCount = 2
    var alpha = 1f
}

object OutlineEffectShader : FramebufferShader(
    Shader(
        resourceToString("/resources/liquidbounce/shaders/plane_projection.vert"),
        resourceToString("/resources/liquidbounce/shaders/glow/glow.frag"),
        arrayOf(
            UniformProvider("texture0") { pointer -> GL20.glUniform1i(pointer, 0) },
            UniformProvider("image") { pointer -> GL20.glUniform1i(pointer, 0) },
            UniformProvider("useImage") { pointer -> GL20.glUniform1i(pointer, 0) },
            UniformProvider("blendColor") { pointer -> OutlineEffectShaderData.blendColor.putToUniform(pointer) },
            UniformProvider("alpha") { pointer -> GL20.glUniform1f(pointer, OutlineEffectShaderData.alpha) },
            UniformProvider("sampleMul") { pointer ->
                GL20.glUniform1f(pointer, OutlineEffectShaderData.sampleMul)
            },

            UniformProvider("glowColor") { pointer -> OutlineEffectShaderData.glowColor.putToUniform(pointer) },
            UniformProvider("falloff") { pointer -> GL20.glUniform1f(pointer, OutlineEffectShaderData.falloff) },
            UniformProvider("layerCount") { pointer ->
                GL20.glUniform1i(pointer, OutlineEffectShaderData.layerCount)
            }
        )
    )
)
