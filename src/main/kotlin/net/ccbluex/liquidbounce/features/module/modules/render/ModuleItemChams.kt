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

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinGameRenderer
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.ClientUniformDefine
import net.ccbluex.liquidbounce.render.createRenderPass
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.kotlin.optional
import net.ccbluex.liquidbounce.utils.render.clearColor
import net.ccbluex.liquidbounce.utils.render.copyFrom
import net.ccbluex.liquidbounce.utils.render.putVec4
import net.ccbluex.liquidbounce.utils.render.writeStd140
import net.minecraft.client.renderer.LightTexture

/**
 * Module ItemChams
 *
 * Applies visual effects to your held items.
 *
 * @see MixinGameRenderer
 * @see LightTexture
 *
 * @author ccetl
 */
object ModuleItemChams : ClientModule("ItemChams", ModuleCategories.RENDER) {

    private val blendColor by color("BlendColor", Color4b(0, 64, 255, 186)).markDirtyOnChanged()
    private val alpha by int("Alpha", 95, 1..255).markDirtyOnChanged()
    private val glowColor by color("GlowColor", Color4b(0, 64, 255, 15)).markDirtyOnChanged()
    private val layers by int("Layers", 3, 1..10).markDirtyOnChanged()
    private val layerSize by float("LayerSize", 1.91f, 1f..5f).markDirtyOnChanged()
    private val falloff by float("Falloff", 6.83f, 0f..20f).markDirtyOnChanged()

    private var edited = false

    private val storedLightmapTexture = gpuDevice.createTexture(
        "$name - Lightmap Texture",
        GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_COPY_SRC,
        TextureFormat.RGBA8,
        16, 16, 1, 1,
    )

    private val UBO = ClientUniformDefine.HAND_ITEM_LIGHTMAP.createSingleBuffer()

    private var uboDirty = true
    private fun <T : Any> Value<T>.markDirtyOnChanged() = onChanged { uboDirty = true }

    private val sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, false)

    fun applyToTexture(textureView: GpuTextureView) {
        if (!this.running || edited) return

        this.storedLightmapTexture.copyFrom(source = textureView.texture())

        if (uboDirty) {
            UBO.writeStd140 {
                putInt(0)
                putFloat(alpha / 255f)
                putVec4(blendColor)
                putFloat(layerSize)
                putVec4(glowColor)
                putFloat(falloff)
                putInt(layers)
            }
            uboDirty = false
        }

        textureView.createRenderPass(
            { "$name Pass" },
            clearColor = optional(-1),
        ).use { pass ->
            pass.setPipeline(ClientRenderPipelines.ItemChams)

            pass.bindTexture("texture0", textureView, sampler)
            pass.bindTexture("image", textureView, sampler)
            pass.setUniform(ClientUniformDefine.HAND_ITEM_LIGHTMAP.uboName, UBO)

            pass.draw(0, 3)
        }

        edited = true
    }

    fun resetTexture(texture: GpuTextureView) {
        if (!edited) return

        texture.texture().copyFrom(source = this.storedLightmapTexture)
        storedLightmapTexture.clearColor(-1)

        edited = false
    }

    override fun onDisabled() {
        uboDirty = true
        super.onDisabled()
    }


}
