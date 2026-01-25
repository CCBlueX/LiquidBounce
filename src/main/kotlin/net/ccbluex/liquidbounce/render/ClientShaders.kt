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
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.resources.Identifier

object ClientShaders : ShaderSource {

    private val shaders = Object2ObjectOpenHashMap<Identifier, String>()

    object Vertex {
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

        private operator fun String.invoke(path: String): Identifier = newShader("vsh/${this}", path = path)
    }

    object Fragment {
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

        private operator fun String.invoke(path: String): Identifier = newShader("fsh/${this}", path = path)
    }

    init {
        Vertex
        Fragment

        logger.info("Loaded ${shaders.size} client shaders.")
    }

    private fun newShader(id: String, path: String): Identifier {
        val k = LiquidBounce.identifier("shader/$id")
        shaders.put(
            k,
            LiquidBounce.resourceToString(path),
        )?.let { error("Duplicated shader: $k") }
        return k
    }

    override fun get(identifier: Identifier, type: ShaderType): String {
        return shaders[identifier] ?: error("Unknown identifier: $identifier")
    }

}
