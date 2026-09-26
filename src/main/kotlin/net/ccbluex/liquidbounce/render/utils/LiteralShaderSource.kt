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

package net.ccbluex.liquidbounce.render.utils

import com.mojang.renderpearl.api.pipeline.ShaderSource
import com.mojang.renderpearl.api.pipeline.ShaderType
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.resources.Identifier
import kotlin.jvm.optionals.getOrNull

/**
 * Supplies in-memory shader text by [Identifier].
 *
 * @param useFallback whether to get from vanilla resource manager for absent identifiers
 */
class LiteralShaderSource @JvmOverloads constructor(
    private val idToShader: Map<Identifier, String>,
    private val useFallback: Boolean = true,
) : ShaderSource {
    /**
     * @see net.minecraft.client.renderer.GameRenderer.preloadUiShader
     */
    override fun getShader(id: Identifier, type: ShaderType): String? {
        idToShader[id]?.let { return it }

        // The pipeline also references a vertex shader (e.g. "minecraft:core/screenquad").
        // Resolve it from the resource pack instead of throwing,
        // mirroring the ShaderSource used by GameRenderer.preloadUiShader.
        if (!useFallback) return null

        val location = type.idConverter().idToFile(id)
        return try {
            mc.resourceManager.getResource(location).getOrNull()?.readAllAsString()
        } catch (_: Exception) {
            null
        }
    }

    override fun getInclude(id: Identifier): ShaderSource.CachedIncludeSource? = null
    override fun close() {
        // NOOP
    }
}
