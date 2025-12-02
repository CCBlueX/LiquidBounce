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

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.shaders.ShaderType
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.ccbluex.liquidbounce.LiquidBounce
import net.minecraft.util.Identifier
import java.util.function.BiFunction

object ClientShaders : BiFunction<Identifier, ShaderType, String?> {

    private val shaders = Object2ObjectOpenHashMap<Identifier, String>()

    @JvmField
    val BGRA_FSH_ID = newShader(
        "fsh/bgra_pos_tex_color",
        path = "shaders/bgra_position_tex_color.frag",
    )

    @JvmField
    val PLAIN_POSITION_TEX_VSH_ID = newShader(
        "vsh/plain_pos_tex",
        path = "shaders/position_tex.vert",
    )

    @JvmField
    val BLIT_FSH_ID = newShader(
        "fsh/blit",
        path = "shaders/blit.frag",
    )

    @JvmField
    val BLEND_FSH_ID = newShader(
        "fsh/blend",
        path = "shaders/blend.frag",
    )

    @JvmField
    val SOBEL_VSH_ID = newShader(
        "vsh/sobel",
        path = "shaders/sobel.vert",
    )

    @JvmField
    val BLUR_FSH_ID = newShader(
        "fsh/blur",
        path = "shaders/blur/ui_blur.frag",
    )

    @JvmField
    val PLANE_PROJECTION_VSH_ID = newShader(
        "vsh/plane_projection",
        path = "shaders/plane_projection.vert",
    )

    @JvmField
    val GLOW_FSH_ID = newShader(
        "fsh/glow",
        path = "shaders/glow/glow.frag",
    )

    @JvmField
    val OUTLINE_FSH_ID = newShader(
        "fsh/outline",
        path = "shaders/outline/entity_outline.frag",
    )

    private fun newShader(id: String, path: String): Identifier {
        val k = LiquidBounce.identifier("shader/$id")
        shaders.put(
            k,
            LiquidBounce.resourceToString(path),
        )?.let { error("Duplicated shader: $k") }
        return k
    }

    override fun apply(identifier: Identifier, type: ShaderType): String? {
        return shaders[identifier] ?: error("Unknown identifier: $identifier")
    }

}
