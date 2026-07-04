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
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinRenderTypeAccessor
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.ClientRenderPipelines.screenQuadSnippet
import net.ccbluex.liquidbounce.render.createRenderPass
import net.ccbluex.liquidbounce.render.engine.LazyRenderTargetHolder
import net.ccbluex.liquidbounce.render.withOutputTarget
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.kotlin.optional
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.rendertype.OutputTarget
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.util.Util
import net.minecraft.world.entity.Entity
import java.util.function.Function

/**
 * TODO: Known issue: world hand items
 */
object ModuleChams : ClientModule("Chams", ModuleCategories.RENDER) {

    private val supportedRenderTypes = hashSetOf(
        "armor_cutout_no_cull",
        "armor_decal_cutout_no_cull",
        "armor_entity_glint",
        "entity_translucent",
        "entity_cutout",
        "entity_cutout_no_cull"
    )

    private val renderTargetHolder = LazyRenderTargetHolder("Chams", useDepth = true)
    private val blitSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
    private val outputTarget = OutputTarget("liquidbounce_chams") { renderTargetHolder.raw }

    private val pipelineBlit: RenderPipeline =
        ClientRenderPipelines.newPipeline("chams/blit") {
            screenQuadSnippet()
            withFragmentShader("core/blit_screen")
            withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
            withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            withDepthStencilState(optional())
        }

    private val remapRenderType: Function<RenderType, RenderType> =
        Util.memoize(Function<RenderType, RenderType> { original ->
            val renderTypeAccessor = original as MixinRenderTypeAccessor

            RenderType.create(
                "liquidbounce_chams/${renderTypeAccessor.name}",
                renderTypeAccessor.state.withOutputTarget(outputTarget),
            )
        })

    private var dirty = false

    private fun supports(renderType: RenderType): Boolean =
        supportedRenderTypes.contains((renderType as MixinRenderTypeAccessor).name)

    fun remapIfNeeded(renderType: RenderType, entity: Entity?): RenderType {
        if (!running || !entity.shouldBeShown() || !supports(renderType)) {
            return renderType
        }

        dirty = true
        return remapRenderType.apply(renderType)
    }

    fun beginFrameIfNeeded() {
        if (!running || !dirty) {
            return
        }

        renderTargetHolder.initAndGet()
    }

    fun compositeIfNeeded(target: RenderTarget) {
        if (!dirty) {
            return
        }

        dirty = false

        val colorTexture = renderTargetHolder.raw?.colorTextureView ?: return

        target.createRenderPass({ "Chams blit pass" }).use { pass ->
            pass.setPipeline(pipelineBlit)
            pass.bindTexture("InSampler", colorTexture, blitSampler)
            pass.draw(3, 1, 0, 0)
        }
    }

    override fun onDisabled() {
        dirty = false
        renderTargetHolder.close()
    }

}
