/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.shaders.ShaderSource
import com.mojang.blaze3d.shaders.ShaderType
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.ccbluex.liquidbounce.LiquidBounce
import net.minecraft.resources.Identifier

sealed class ClientShaders(val type: ShaderType) : ShaderSource {

    private val shaders = Object2ObjectOpenHashMap<Identifier, String>()

    protected operator fun String.invoke(path: String): Identifier = newShader("${type.getName()}/${this}", path = path)

    object Vertex : ClientShaders(ShaderType.VERTEX) {

        @JvmField
        val PlainPosTex = "plain_pos_tex"("shaders/position_tex.vert")

        @JvmField
        val PosRelativeToCamera = "pos_relative_to_camera"("shaders/relative_to_camera/position.vsh")

        @JvmField
        val PosColorRelativeToCamera = "pos_color_relative_to_camera"("shaders/relative_to_camera/position_color.vsh")

        @JvmField
        val Sobel = "sobel"("shaders/sobel.vert")

        @JvmField
        val PlainProjection = "plane_projection"("shaders/plane_projection.vert")

        @JvmField
        val Circle = "circle"("shaders/circle/circle.vsh")

    }

    object Fragment : ClientShaders(ShaderType.FRAGMENT) {

        @JvmField
        val BgraPosTex = "bgra_pos_tex_color"("shaders/bgra_position_tex_color.frag")

        @JvmField
        val PosRelativeToCamera = "pos_relative_to_camera"("shaders/relative_to_camera/position.fsh")

        @JvmField
        val Blit = "blit"("shaders/blit.frag")

        @JvmField
        val Blend = "blend"("shaders/blend.frag")

        @JvmField
        val GuiBlur = "blur"("shaders/blur/ui_blur.frag")

        @JvmField
        val Glow = "glow"("shaders/glow/glow.frag")

        @JvmField
        val EntityOutline = "outline"("shaders/outline/entity_outline.frag")

        @JvmField
        val RoundedRect = "rounded_rect"("shaders/circle/rounded_rect.fsh")

    }

    private fun newShader(id: String, path: String): Identifier {
        val k = LiquidBounce.identifier("shader/$id")
        shaders.put(
            k,
            LiquidBounce.resourceToString(path),
        )?.let { error("Duplicated shader: $k") }
        return k
    }

    override fun get(identifier: Identifier, type: ShaderType): String? {
        if (type != this.type) return null
        return shaders[identifier]
    }

    companion object : ShaderSource {
        override fun get(identifier: Identifier, shaderType: ShaderType): String? = when (shaderType) {
            ShaderType.VERTEX -> Vertex[identifier, shaderType]
            ShaderType.FRAGMENT -> Fragment[identifier, shaderType]
        }
    }

}
