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
package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.TextureFormat
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinGameRenderer
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.setUniform
import net.ccbluex.liquidbounce.render.shader.shaders.OutlineEffectShaderData
import net.ccbluex.liquidbounce.render.trianglePosTexVertexBuffer
import net.ccbluex.liquidbounce.utils.kotlin.optional
import kotlin.use

/**
 * Module ItemChams
 *
 * Applies visual effects to your held items.
 *
 * @see MixinGameRenderer
 *
 * @author ccetl
 */
object ModuleItemChams : ClientModule("ItemChams", Category.RENDER) {

    private val blendColor by color("BlendColor", Color4b(0, 64, 255, 186))
    private val alpha by int("Alpha", 95, 1..255)
    private val glowColor by color("GlowColor", Color4b(0, 64, 255, 15))
    private val layers by int("Layers", 3, 1..10)
    private val layerSize by float("LayerSize", 1.91f, 1f..5f)
    private val falloff by float("Falloff", 6.83f, 0f..20f)

    /**
     * @see net.minecraft.client.render.LightmapTextureManager
     */
    private val texture = gpuDevice.createTexture(
        "ItemChams Light Texture",
        TextureFormat.RGBA8,
        16, 16, 1
    ).apply {
        setTextureFilter(FilterMode.LINEAR, false)
        gpuDevice.createCommandEncoder().clearColorTexture(this, -1)
    }

    fun applyToTexture(texture: GpuTexture) {
        if (!this.running) return

        gpuDevice.createCommandEncoder()
            .copyTextureToTexture(
                this.texture,
                texture,
                0,
                0, 0,
                0, 0,
                16, 16,
            )

        gpuDevice.createCommandEncoder().createRenderPass(
            texture,
            optional(-1),
        ).use { renderPass ->
            renderPass.setPipeline(ClientRenderPipelines.ItemChams)

            renderPass.bindSampler("texture0", this.texture)
            renderPass.bindSampler("image", this.texture)
            renderPass.setUniform("useImage", 0)
            renderPass.setUniform("blendColor", blendColor)
            renderPass.setUniform("alpha", alpha / 255f)
            renderPass.setUniform("sampleMul", layerSize)
            renderPass.setUniform("glowColor", glowColor)
            renderPass.setUniform("falloff", falloff)
            renderPass.setUniform("layerCount", layers)

            renderPass.setVertexBuffer(0, trianglePosTexVertexBuffer)
            renderPass.draw(0, 3)
        }
    }

    fun restoreToTexture(texture: GpuTexture) {
        gpuDevice.createCommandEncoder()
            .copyTextureToTexture(
                texture,
                this.texture,
                0,
                0, 0,
                0, 0,
                16, 16,
            )
    }

    var active = false

    fun setData() {
        // LightmapTextureManager
        active = true
        with(OutlineEffectShaderData) {
            falloff = ModuleItemChams.falloff
            sampleMul = layerSize
            layerCount = layers
            glowColor = ModuleItemChams.glowColor
            blendColor = ModuleItemChams.blendColor
            alpha = ModuleItemChams.alpha / 255f
        }
    }

}
