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

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.textures.FilterMode
import net.ccbluex.liquidbounce.annotations.ValueClassCandidate
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.toTextureProperty
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinRenderTypeAccessor
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.ClientUniformDefine
import net.ccbluex.liquidbounce.render.buffers.CachedUniform
import net.ccbluex.liquidbounce.render.createRenderPass
import net.ccbluex.liquidbounce.render.engine.LazyRenderTargetHolder
import net.ccbluex.liquidbounce.render.setPipeline
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.io.PNG_AND_JPG
import net.minecraft.client.renderer.RenderBuffers
import net.minecraft.client.renderer.SubmitNodeStorage
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.world.entity.Entity
import org.joml.Vector2f
import java.util.Optional
import java.util.OptionalDouble

@Suppress("TooManyFunctions")
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
    @ValueClassCandidate
    @JvmRecord
    private data class ImageUniform(
        val scaleX: Float,
        val scaleY: Float,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f,
    )

    private val heldItemEntityContext = ScopedValue.newInstance<Entity>()
    private var entityContext: Entity? = null
    private var captureDepth = 0
    private var chamsStorage = SubmitNodeStorage()
    private var chamsRenderBuffers: RenderBuffers? = null
    private var chamsDispatcher: FeatureRenderDispatcher? = null
    private var chamsFrame: FeatureRenderDispatcher.PreparedFrame? = null

    private fun supports(renderType: RenderType): Boolean =
        supportedRenderTypes.contains((renderType as MixinRenderTypeAccessor).name)

    /** Tracks the current entity for subsequent captures within one render pass. */
    private fun track(entity: Entity) {
        entityContext = entity
    }

    /**
     * Tracks an entity render submission while leaving the vanilla render type unchanged.
     *
     * A capture context is only established for entities that should be shown as chams, and only
     * up until the next submission (or the end of the level entity submissions, see [clearEntityContext]).
     */
    fun trackIfNeeded(renderType: RenderType, entity: Entity?): RenderType {
        entityContext = null
        if (running && entity != null && entity.shouldBeShown() && supports(renderType)) {
            track(entity)
        }
        return renderType
    }

    fun beginFrame() {
        chamsFrame?.close()
        chamsFrame = null
        resetStorage()
        entityContext = null
    }

    private fun resetStorage() {
        chamsStorage = SubmitNodeStorage().also {
            it.setUseImprovedTransparency(mc.gameRenderer.useImprovedTransparency())
        }
    }

    /**
     * Clears the tracked entity context once all level entity submissions are done.
     *
     * [entityContext] is set during level entity submission to the last chams target and must not
     * leak into later submissions that are unrelated to any chams target (e.g. first-person held
     * items rendered after the level), otherwise they would be mistaken for chams targets and
     * removed from the main render target.
     *
     * [heldItemEntityContext] needs no clearing: it is a [ScopedValue] that is only bound inside
     * [withHeldItemContext] and restored automatically when that block ends.
     */
    fun clearEntityContext() {
        entityContext = null
    }

    /**
     * Captures an entity model submission into the chams storage.
     *
     * @return `true` when the submission was captured (and the vanilla submission should be cancelled),
     *         `false` when the submission should keep flowing into the main render target.
     */
    @Suppress("UnusedParameter")
    fun captureModel(
        model: net.minecraft.client.model.Model<*>,
        state: Any,
        poseStack: com.mojang.blaze3d.vertex.PoseStack,
        renderType: RenderType,
        lightCoords: Int,
        overlayCoords: Int,
        tintedColor: Int,
        uvMapping: net.minecraft.client.renderer.texture.UvMapping?,
        outlineColor: Int,
    ): Boolean {
        if (!captureTargeted(renderType)) return false
        captureDepth++
        try {
            @Suppress("UNCHECKED_CAST")
            chamsStorage.submitModel(
                model as net.minecraft.client.model.Model<Any>, state, poseStack, renderType,
                lightCoords, overlayCoords, tintedColor, uvMapping, 0
            )
        } finally {
            captureDepth--
        }
        return true
    }

    /**
     * Captures an item submission into the chams storage.
     *
     * @return `true` when the submission was captured (and the vanilla submission should be cancelled),
     *         `false` when the submission should keep flowing into the main render target.
     */
    @Suppress("UnusedParameter")
    fun captureItem(
        poseStack: com.mojang.blaze3d.vertex.PoseStack,
        displayContext: net.minecraft.world.item.ItemDisplayContext,
        lightCoords: Int,
        overlayCoords: Int,
        outlineColor: Int,
        tintLayers: IntArray,
        quads: net.minecraft.client.resources.model.geometry.ItemQuads,
        foilType: net.minecraft.client.renderer.item.ItemStackRenderState.FoilType,
    ): Boolean {
        if (!captureTargeted()) return false
        captureDepth++
        try {
            chamsStorage.submitItem(
                poseStack,
                displayContext,
                lightCoords,
                overlayCoords,
                0,
                tintLayers,
                quads,
                foilType,
            )
        } finally {
            captureDepth--
        }
        return true
    }

    private fun captureTargeted(renderType: RenderType? = null): Boolean {
        if (captureDepth > 0) return false
        if (!running || (entityContext == null && !heldItemEntityContext.isBound)) return false
        return renderType == null || supports(renderType)
    }

    /** Runs a third-person held-item submission with the current entity bound. */
    fun withHeldItemContext(entity: Entity?, block: Runnable) {
        if (running && entity.shouldBeShown()) {
            ScopedValue.where(heldItemEntityContext, entity).run(block)
        } else {
            block.run()
        }
    }

    fun prepareFrame() {
        if (!running || chamsStorage.submitsPerOrder.isEmpty()) return
        renderTargetHolder.initAndGet()
        val buffers = chamsRenderBuffers ?: RenderBuffers(1).also { chamsRenderBuffers = it }
        val dispatcher = chamsDispatcher ?: FeatureRenderDispatcher(
            buffers,
            mc.modelManager,
            mc.atlasManager,
            mc.font,
            mc.gameRenderer.gameRenderState(),
        ).also { chamsDispatcher = it }
        chamsFrame = dispatcher.prepareFrame(chamsStorage)
    }

    fun renderChams() {
        val target = renderTargetHolder.get() ?: return
        val frame = chamsFrame ?: return
        target.createRenderPass(
            { "Chams" },
            Optional.of(org.joml.Vector4f(0f, 0f, 0f, 0f)),
            OptionalDouble.of(0.0),
        ).use { pass ->
            RenderSystem.bindDefaultUniforms(pass)
            FeatureRenderDispatcher.renderAllFeatures(pass, frame)
        }
    }

    /** Blits the accumulated chams target into the main render target. */
    fun compositeIfNeeded(target: RenderTarget) {
        if (chamsStorage.submitsPerOrder.isEmpty()) {
            return
        }

        try {
            renderTargetHolder.get()?.let { modes.activeMode.render(target, it) }
        } finally {
            chamsFrame?.close()
            chamsFrame = null
            chamsRenderBuffers?.endFrame()
            // Reset the accumulated submit storage so the next frame starts empty.
            // This also serves as the "composited" marker: no dirty flag needed,
            // emptiness of the storage is the source of truth.
            resetStorage()
        }
    }

    override fun onDisabled() {
        chamsFrame?.close()
        chamsFrame = null
        chamsDispatcher?.close()
        chamsRenderBuffers?.close()
        chamsDispatcher = null
        chamsRenderBuffers = null
        renderTargetHolder.close()
        resetStorage()
    }

    private object Normal : ChamsMode("Normal") {
        override fun render(target: RenderTarget, chamsTarget: RenderTarget) {
            val colorTexture = chamsTarget.colorTextureView ?: return

            target.createRenderPass({ "Chams blit pass" }, useDepthAttachment = false).use { pass ->
                pass.setPipeline(ClientRenderPipelines.ChamsBlit)
                pass.setUniform("InSampler", colorTexture, blitSampler)
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
                pass.setUniform("entityColor", colorTexture, blitSampler)
                pass.setUniform("entityDepth", chamsDepth, blitSampler)
                pass.setUniform("sceneDepth", sceneDepth, blitSampler)
                pass.setUniform("image", imageView, sampler)
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
