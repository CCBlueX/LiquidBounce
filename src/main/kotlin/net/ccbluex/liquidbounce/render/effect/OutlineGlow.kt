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

package net.ccbluex.liquidbounce.render.effect

import com.mojang.blaze3d.framegraph.FrameGraphBuilder
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.renderpearl.api.buffers.GpuBufferSlice
import com.mojang.renderpearl.api.pipeline.BindGroupLayout
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.pipeline.UniformType
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.render.ClientShaders
import net.ccbluex.liquidbounce.render.engine.LazyRenderTargetHolder
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.renderer.LevelTargetBundle
import net.minecraft.client.renderer.PostChain
import net.minecraft.client.renderer.PostPass
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.UniformValue
import net.minecraft.resources.Identifier

/**
 * Style choices of the glow modes.
 *
 * Models of glowing entities and block entities all go into the game's one entity outline target. Each mode tags the
 * outline color it gives them, so when several modes drew, every mode's silhouettes are cut out and styled on their
 * own. Block meshes go into a target of their own instead, see [drawMeshes].
 */
object OutlineGlow {

    // Smooth
    private const val DEFAULT_STYLE = 1

    // Tags live in the lowest bits of blue, which the style passes drop
    private const val TAG_BITS = 7

    // Inputs of the combine shader
    private const val MAX_COMBINED = 4

    private class Owner(val mode: Mode, val tag: Int, val styles: ModeValueGroup<ShaderStyle>) {
        val chain = StyleChain(styles, OUTLINE)
    }

    private val OUTLINE = LevelTargetBundle.ENTITY_OUTLINE_TARGET_ID
    private val VANILLA = Identifier.withDefaultNamespace("entity_outline")
    private val mask = LiquidBounce.identifier("effect/mask")
    private val styled = Array(MAX_COMBINED) { LiquidBounce.identifier("effect/styled_$it") }

    private val owners = ArrayList<Owner>()
    private val ownerOf = Reference2ObjectOpenHashMap<Mode, Owner>()

    /**
     * Modes that drew into the outline target since the chain was last picked.
     */
    private val drawn = ReferenceOpenHashSet<Mode>()

    private var combined: PostChain? = null
    private var combinedOwners: List<Owner> = emptyList()
    private val combinedViews = ArrayList<GpuBufferSlice>()
    private var stylesChanged = false

    private val meshes = LazyRenderTargetHolder("Glow meshes", useDepth = false)

    fun styles(mode: Mode): ModeValueGroup<ShaderStyle> {
        check(owners.size < TAG_BITS) { "No tag left for ${mode.name}" }

        val styles = mode.modes(mode, "Style", { DEFAULT_STYLE }) {
            arrayOf<ShaderStyle>(VanillaStyle(it)) + postStyles(it)
        }
        styles.onChanged { stylesChanged = true }
        for (style in styles.modes) {
            for (value in style.containedValues) {
                value.onChanged { stylesChanged = true }
            }
        }

        val owner = Owner(mode, owners.size + 1, styles)
        owners += owner
        ownerOf[mode] = owner
        return styles
    }

    /**
     * [argb] as outline color of a model [mode] makes glow.
     */
    @JvmStatic
    fun outlineColor(mode: Mode, argb: Int): Int {
        drawn += mode
        return argb and TAG_BITS.inv() or ownerOf[mode]!!.tag
    }

    /**
     * The glow chain in place of the game's entity outline post chain. Untagged outlines, like those of TNT, take the
     * style of the first mode.
     */
    @JvmStatic
    fun replace(original: PostChain?): PostChain? {
        val drawing = owners.filter { it.mode.running && it.mode in drawn }
            .ifEmpty { listOfNotNull(owners.firstOrNull { it.mode.running }) }
            .take(MAX_COMBINED)
        drawn.clear()

        return when (drawing.size) {
            0 -> original
            1 -> drawing[0].chain.get() ?: original
            else -> combined(drawing) ?: original
        }
    }

    /**
     * Draws block meshes of [mode] with [draw] into a target of their own, styles them and blends them onto [scene].
     * [draw] tells whether it drew anything.
     */
    fun drawMeshes(mode: Mode, scene: RenderTarget, draw: (RenderTarget) -> Boolean) {
        val target = meshes.initAndGet()
        if (!draw(target)) {
            return
        }

        val owner = ownerOf[mode]!!
        val chain = owner.chain.get()
            ?: mc.shaderManager.getPostChain(VANILLA, setOf(OUTLINE))
        chain?.let { process(it, target) }
        target.blitAndBlendToTexture(scene.colorTextureView!!, scene.depthTextureView!!)
    }

    private fun process(chain: PostChain, target: RenderTarget) {
        val frame = FrameGraphBuilder()
        val bundle = PostChain.TargetBundle.of(OUTLINE, frame.importExternal("glow meshes", target))
        chain.addToFrame(frame, target.width, target.height, bundle)
        frame.execute(mc.gameRenderer.resourcePool)
    }

    private fun combined(drawing: List<Owner>): PostChain? {
        if (stylesChanged || drawing != combinedOwners) {
            combined?.close()
            combinedViews.clear()
            combined = buildCombined(drawing)
            combinedOwners = drawing
            stylesChanged = false
        }
        combinedViews.forEach(StyleChain::writeView)
        return combined
    }

    private fun buildCombined(drawing: List<Owner>): PostChain? {
        val passes = ArrayList<PostPass>()
        for ((index, owner) in drawing.withIndex()) {
            val untagged = if (index == 0) 1 else 0
            passes += StyleChain.pass(
                Mask,
                OUTLINE,
                mask,
                mapOf("EffectMask" to listOf(UniformValue.IntUniform(owner.tag), UniformValue.IntUniform(untagged))),
            )

            when (val style = owner.styles.activeMode) {
                is PostStyle -> {
                    if (!StyleChain.compiles(style)) return null
                    passes += StyleChain.stylePasses(style, mask, styled[index], combinedViews)
                }
                is VanillaStyle -> passes += StyleChain.pass(Sobel, mask, styled[index])
            }
        }

        passes += PostPass(
            combine(drawing.size),
            OUTLINE,
            emptyMap(),
            drawing.indices.map { PostPass.TargetInput("In$it", styled[it], false, false) },
        )

        val scratch = StyleChain.scratchTargets + mask + styled.take(drawing.size)
        return StyleChain.postChain(passes, scratch.associateWith { StyleChain.scratch }, setOf(OUTLINE))
    }

    private val Mask = postPipeline(ClientShaders.Fragment.EffectMask, listOf("In")) {
        withUniform("EffectMask", UniformType.UNIFORM_BUFFER)
    }

    // The game's own outline edge, which its blur would otherwise follow
    private val Sobel = postPipeline(Identifier.withDefaultNamespace("post/entity_sobel"), listOf("In"))

    private val combines = arrayOfNulls<RenderPipeline>(MAX_COMBINED + 1)

    private fun combine(count: Int) = combines[count] ?: postPipeline(
        ClientShaders.Fragment.EffectCombine,
        List(count) { "In$it" },
        location = ClientShaders.Fragment.EffectCombine.withSuffix("/$count"),
        count = count,
    ).also { combines[count] = it }

    private inline fun postPipeline(
        shader: Identifier,
        samplers: List<String>,
        location: Identifier = shader,
        count: Int = 0,
        uniforms: BindGroupLayout.Builder.() -> Unit = {},
    ): RenderPipeline = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
        .withLocation(location)
        .withVertexShader(ClientShaders.Vertex.ScreenQuad)
        .withFragmentShader(shader)
        .apply { if (count > 0) withShaderDefine("COUNT", count) }
        .withBindGroupLayout(
            BindGroupLayout.builder()
                .apply { samplers.forEach { withUniform("${it}Sampler", UniformType.COMBINED_IMAGE_SAMPLER) } }
                .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                .apply(uniforms)
                .build()
        )
        .withColorTargetState(ColorTargetState.DEFAULT)
        .build()

}
