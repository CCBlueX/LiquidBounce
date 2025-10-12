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

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.systems.RenderSystem
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap
import net.ccbluex.fastutil.fastIterator
import net.ccbluex.liquidbounce.LiquidBounce
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.SynchronousResourceReloader
import net.minecraft.util.Identifier
import okio.buffer
import okio.source

object ClientRenderPipelines : SynchronousResourceReloader {

    private val renderPipelines = Object2ObjectRBTreeMap<Identifier, RenderPipeline>()

    private inline fun create(name: String, builderAction: RenderPipeline.Builder.() -> Unit): RenderPipeline {
        val id = LiquidBounce.identifier("pipeline/$name")
        return RenderPipeline.builder()
            .withLocation(id)
            .apply(builderAction)
            .build().also { r ->
                renderPipelines.put(id, r)?.let { error("Duplicated render pipeline: $it") }
            }
    }

    @JvmField
    val WORLD_RENDER_ENV = create("world_render") {
        withBlend(BlendFunction.TRANSLUCENT)
        withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
    }

    @JvmField
    val GUI_RENDER_ENV = create("gui_render") {
//        withBlend(BlendFunction.TRANSLUCENT)
    }

    /**
     * Precompile
     */
    override fun reload(manager: ResourceManager) {
        val device = RenderSystem.getDevice()

        renderPipelines.fastIterator().forEach { (_, pipeline) ->
            device.precompilePipeline(pipeline) { identifier, _ ->
                val resource = manager.getResource(identifier).get()
                resource.inputStream.source().buffer().use { it.readUtf8() }
            }
        }
    }

}
