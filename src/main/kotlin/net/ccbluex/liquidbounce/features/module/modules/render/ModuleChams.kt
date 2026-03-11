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
package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderSetup.OutlineProperty
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.resources.Identifier
import net.minecraft.util.Util
import java.util.function.BiFunction
import java.util.function.Function

/**
 * TODO: Known issue: player armor + hand items
 */
object ModuleChams: ClientModule("Chams", ModuleCategories.RENDER) {

    private inline fun RenderPipeline.Builder.forChams() {
//        withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        withDepthBias(1f, -10000000F)
    }

    private val PIPELINE_ENTITY_TRANSLUCENT: RenderPipeline =
        ClientRenderPipelines.newPipeline("chams/entity_translucent") {
            withSnippet(RenderPipelines.ENTITY_SNIPPET)
            withShaderDefine("ALPHA_CUTOUT", 0.1F)
            withShaderDefine("PER_FACE_LIGHTING")
            withSampler("Sampler1")
            withBlend(BlendFunction.TRANSLUCENT)
            withCull(false)
            forChams()
        }

    private val PIPELINE_ENTITY_CUTOUT: RenderPipeline =
        ClientRenderPipelines.newPipeline("chams/entity_cutout") {
            withSnippet(RenderPipelines.ENTITY_SNIPPET)
            withShaderDefine("ALPHA_CUTOUT", 0.1f)
            withSampler("Sampler1")
            forChams()
        }

    private val PIPELINE_ENTITY_CUTOUT_NO_CULL: RenderPipeline =
        ClientRenderPipelines.newPipeline("chams/entity_cutout_no_cull") {
            withSnippet(RenderPipelines.ENTITY_SNIPPET)
            withShaderDefine("ALPHA_CUTOUT", 0.1f)
            withShaderDefine("PER_FACE_LIGHTING")
            withSampler("Sampler1")
            withCull(false)
            forChams()
        }

    @JvmField
    val ENTITY_TRANSLUCENT: BiFunction<Identifier, Boolean, RenderType> =
        Util.memoize { identifier, affectsOutline ->
            val renderSetup: RenderSetup = RenderSetup.builder(PIPELINE_ENTITY_TRANSLUCENT)
                .withTexture("Sampler0", identifier)
                .useLightmap()
                .useOverlay()
                .affectsCrumbling()
                .sortOnUpload()
                .setOutline(if (affectsOutline) OutlineProperty.AFFECTS_OUTLINE else OutlineProperty.NONE)
                .createRenderSetup()
            RenderType.create("entity_translucent", renderSetup)
        }

    @JvmField
    val ENTITY_CUTOUT: Function<Identifier, RenderType> =
        Util.memoize { identifier ->
            val renderSetup = RenderSetup.builder(PIPELINE_ENTITY_CUTOUT)
                .withTexture("Sampler0", identifier)
                .useLightmap()
                .useOverlay()
                .affectsCrumbling()
                .setOutline(OutlineProperty.AFFECTS_OUTLINE)
                .createRenderSetup()
            RenderType.create("entity_cutout", renderSetup)
        }

    @JvmField
    val ENTITY_CUTOUT_NO_CULL: BiFunction<Identifier, Boolean, RenderType> =
        Util.memoize { identifier, affectsOutline ->
            val renderSetup = RenderSetup.builder(PIPELINE_ENTITY_CUTOUT_NO_CULL)
                .withTexture("Sampler0", identifier)
                .useLightmap()
                .useOverlay()
                .affectsCrumbling()
                .setOutline(if (affectsOutline) OutlineProperty.AFFECTS_OUTLINE else OutlineProperty.NONE)
                .createRenderSetup()
            RenderType.create("entity_cutout_no_cull", renderSetup)
        }

}
