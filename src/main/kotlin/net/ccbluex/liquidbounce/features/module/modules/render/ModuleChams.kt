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
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.annotations.ValueClassCandidate
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.toTextureProperty
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinRenderTypeAccessor
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.ClientRenderPipelines.screenQuadSnippet
import net.ccbluex.liquidbounce.render.ClientUniformDefine
import net.ccbluex.liquidbounce.render.buffers.CachedUniform
import net.ccbluex.liquidbounce.render.createRenderPass
import net.ccbluex.liquidbounce.render.engine.LazyRenderTargetHolder
import net.ccbluex.liquidbounce.render.withOutputTarget
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.io.PNG_AND_JPG
import net.ccbluex.liquidbounce.utils.kotlin.optional
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.feature.ItemFeatureRenderer
import net.minecraft.client.renderer.rendertype.OutputTarget
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.util.Util
import net.minecraft.world.entity.Entity
import org.joml.Vector2f
import java.util.function.Function

object ModuleChams : ClientModule("Chams", ModuleCategories.RENDER) {

    private val modes = choices("Mode", Normal, arrayOf(Normal, Image))

    private val supportedRenderTypes: Set<String> = hashSetOf(
        "armor_cutout_no_cull",
        "armor_decal_cutout_no_cull",
        "armor_entity_glint",
        "entity_translucent",
        "entity_cutout",
        "entity_cutout_cull",
        "entity_cutout_no_cull",
        "entity_solid",
        "entity_glint",
        "glint",
        "glint_translucent",
        "item_cutout",
        "item_translucent"
    )

    private val renderTargetHolder = LazyRenderTargetHolder(this.name, useDepth = true)
    private val blitSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
    private val outputTarget = OutputTarget("liquidbounce_chams", renderTargetHolder)

    @ValueClassCandidate
    @JvmRecord
    private data class ImageUniform(
        val scaleX: Float,
        val scaleY: Float,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f,
    )

    private val pipelineBlit: RenderPipeline =
        ClientRenderPipelines.newPipeline("chams/blit") {
            screenQuadSnippet()
            withFragmentShader("core/blit_screen")
            withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
            withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            withDepthStencilState(optional())
        }

    private val remapRenderType: Function<RenderType, RenderType> =
        Util.memoize { original ->
            val renderTypeAccessor = original as MixinRenderTypeAccessor

            RenderType.create(
                "liquidbounce_chams/${renderTypeAccessor.name}",
                renderTypeAccessor.state.withOutputTarget(outputTarget),
            )
        }

    private val heldItemEntityContext = ScopedValue.newInstance<Entity>()
    private val heldItemSubmits = ReferenceOpenHashSet<ItemFeatureRenderer.Submit>()

    private var dirty = false

    private fun supports(renderType: RenderType): Boolean =
        supportedRenderTypes.contains((renderType as MixinRenderTypeAccessor).name)

    /** Remaps an entity render type to the chams target when applicable. */
    fun remapIfNeeded(renderType: RenderType, entity: Entity?): RenderType {
        if (!running || !entity.shouldBeShown() || !supports(renderType)) {
            return renderType
        }

        dirty = true
        return remapRenderType.apply(renderType)
    }

    /** Runs a third-person held-item submission with the current entity bound. */
    fun withHeldItemContext(entity: Entity?, block: Runnable) {
        if (running && entity.shouldBeShown()) {
            ScopedValue.where(heldItemEntityContext, entity).run(block)
        } else {
            block.run()
        }
    }

    /** Marks an item submit as coming from the current held-item context. */
    fun markHeldItemSubmitIfActive(submit: ItemFeatureRenderer.Submit) {
        if (!heldItemEntityContext.isBound) {
            return
        }

        heldItemSubmits.add(submit)
    }

    /** Returns whether the submit was created from a held-item context. */
    fun isHeldItemSubmit(submit: ItemFeatureRenderer.Submit): Boolean =
        heldItemSubmits.contains(submit)

    /** Remaps a deferred held-item render type to the chams target when applicable. */
    fun remapHeldItemRenderTypeIfNeeded(submit: ItemFeatureRenderer.Submit, renderType: RenderType): RenderType {
        if (!isHeldItemSubmit(submit) || !supports(renderType)) {
            return renderType
        }

        dirty = true
        return remapRenderType.apply(renderType)
    }

    /** Remaps an immediate held-item render type using the current scoped entity. */
    fun remapCurrentHeldItemRenderTypeIfNeeded(renderType: RenderType): RenderType {
        val entity = if (heldItemEntityContext.isBound) heldItemEntityContext.get() else return renderType
        return remapIfNeeded(renderType, entity)
    }

    /** Ensures the chams target exists before any remapped draws in this frame. */
    fun beginFrameIfNeeded() {
        if (!running || !dirty) {
            return
        }

        renderTargetHolder.initAndGet()
    }

    /** Blits the accumulated chams target into the main render target. */
    fun compositeIfNeeded(target: RenderTarget) {
        if (!dirty) {
            heldItemSubmits.clear()
            return
        }

        dirty = false

        try {
            renderTargetHolder.get()?.let { modes.activeMode.render(target, it) }
        } finally {
            heldItemSubmits.clear()
        }
    }

    override fun onDisabled() {
        dirty = false
        heldItemSubmits.clear()
        renderTargetHolder.close()
    }

    private object Normal : ChamsMode("Normal") {
        override fun render(target: RenderTarget, chamsTarget: RenderTarget) {
            val colorTexture = chamsTarget.colorTextureView ?: return

            target.createRenderPass({ "Chams blit pass" }, useDepthAttachment = false).use { pass ->
                pass.setPipeline(pipelineBlit)
                pass.bindTexture("InSampler", colorTexture, blitSampler)
                pass.draw(3, 1, 0, 0)
            }
        }
    }

    private object Image : ChamsMode("Image") {
        private val texture by file("File", supportedExtensions = PNG_AND_JPG).toTextureProperty(this)
        val mapping = modes("Mapping", Repeat, arrayOf(Repeat, Stretch, Cover))
        private val filtering by enumChoice("Filtering", ImageFiltering.NEAREST)
        private val offset by vec2f("Offset", Vector2f())

        private val imageUniform = CachedUniform<ImageUniform>(ClientUniformDefine.CHAMS) { value ->
            putVec2(value.scaleX, value.scaleY)
            putVec2(value.offsetX, value.offsetY)
        }

        override fun render(target: RenderTarget, chamsTarget: RenderTarget) {
            val colorTexture = chamsTarget.colorTextureView
            val chamsDepth = chamsTarget.depthTextureView
            val sceneDepth = target.depthTextureView
            val imageView = texture?.textureView
            if (colorTexture == null || chamsDepth == null || sceneDepth == null || imageView == null) {
                Normal.render(target, chamsTarget)
                return
            }

            val imageWidth = imageView.getWidth(0)
            val imageHeight = imageView.getHeight(0)
            val mappingData = mapping.activeMode.uniform(target.width, target.height, imageWidth, imageHeight)
            val imageData = mappingData.copy(
                offsetX = mappingData.offsetX + offset.x(),
                offsetY = mappingData.offsetY + offset.y(),
            )
            val sampler = filtering.sampler(mapping.activeMode.repeats)
            val ubo = imageUniform.get(imageData)

            target.createRenderPass({ "Chams image blit pass" }, useDepthAttachment = false).use { pass ->
                pass.setPipeline(ClientRenderPipelines.ChamsImage)
                pass.bindTexture("entityColor", colorTexture, blitSampler)
                pass.bindTexture("entityDepth", chamsDepth, blitSampler)
                pass.bindTexture("sceneDepth", sceneDepth, blitSampler)
                pass.bindTexture("image", imageView, sampler)
                pass.setUniform(ClientUniformDefine.CHAMS.uboName, ubo)
                pass.draw(3, 1, 0, 0)
            }
        }
    }

    private abstract class ChamsMode(name: String) : Mode(name) {
        override val parent: ModeValueGroup<*>
            get() = modes

        abstract fun render(target: RenderTarget, chamsTarget: RenderTarget)
    }

    private abstract class ImageMappingMode(name: String) : Mode(name) {
        override val parent: ModeValueGroup<*>
            get() = Image.mapping

        open val repeats: Boolean get() = false

        abstract fun uniform(targetWidth: Int, targetHeight: Int, imageWidth: Int, imageHeight: Int): ImageUniform
    }

    private object Repeat : ImageMappingMode("Repeat") {
        private val tileWidth by int("TileWidth", 256, 16..2048, "px")

        override val repeats: Boolean get() = true

        override fun uniform(targetWidth: Int, targetHeight: Int, imageWidth: Int, imageHeight: Int): ImageUniform {
            val width = tileWidth.toFloat()
            return ImageUniform(width, width * imageHeight / imageWidth)
        }
    }

    private object Stretch : ImageMappingMode("Stretch") {
        override fun uniform(targetWidth: Int, targetHeight: Int, imageWidth: Int, imageHeight: Int) =
            ImageUniform(targetWidth.toFloat(), targetHeight.toFloat())
    }

    private object Cover : ImageMappingMode("Cover") {
        override fun uniform(targetWidth: Int, targetHeight: Int, imageWidth: Int, imageHeight: Int): ImageUniform {
            val scale = maxOf(targetWidth.toFloat() / imageWidth, targetHeight.toFloat() / imageHeight)
            val width = imageWidth * scale
            val height = imageHeight * scale
            return ImageUniform(width, height, (targetWidth - width) * 0.5f, (targetHeight - height) * 0.5f)
        }
    }

    private enum class ImageFiltering(override val tag: String, val filterMode: FilterMode) : Tagged {
        NEAREST("Nearest", FilterMode.NEAREST),
        LINEAR("Linear", FilterMode.LINEAR),
        ;

        fun sampler(repeat: Boolean) = RenderSystem.getSamplerCache().let { cache ->
            if (repeat) cache.getRepeat(filterMode) else cache.getClampToEdge(filterMode)
        }
    }

}
