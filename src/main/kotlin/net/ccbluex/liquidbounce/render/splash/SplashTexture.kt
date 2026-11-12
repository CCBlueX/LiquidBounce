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
package net.ccbluex.liquidbounce.render.splash

import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.textures.AddressMode
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuTexture
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.client.renderer.texture.MipmapGenerator
import net.minecraft.client.renderer.texture.MipmapStrategy
import net.minecraft.resources.Identifier

/**
 * A splash image with mipmaps: splashes draw their images far smaller than they come, which aliases without.
 */
internal class SplashTexture(id: String, image: NativeImage) : AbstractTexture() {

    val setup: TextureSetup

    init {
        val levels = MipmapGenerator.generateMipLevels(
            Identifier.fromNamespaceAndPath("liquidbounce", "splash/${id.lowercase().filter(Char::isLetterOrDigit)}"),
            arrayOf(image),
            mipLevels(image.width, image.height),
            MipmapStrategy.MEAN,
            0f,
            image.computeTransparency(),
        )

        try {
            val device = RenderSystem.getDevice()
            val texture = device.createTexture(
                { "LiquidBounce splash $id" },
                GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING,
                GpuFormat.RGBA8_UNORM,
                image.width,
                image.height,
                1,
                levels.size,
            )
            this.texture = texture
            textureView = device.createTextureView(texture)
            val encoder = device.createCommandEncoder()
            levels.forEachIndexed { level, mip -> encoder.writeToTexture(texture, mip, level, 0, 0, 0) }
        } finally {
            // Level 0 is the bundle's image, which the bundle closes
            levels.drop(1).forEach(NativeImage::close)
        }

        sampler = RenderSystem.getSamplerCache().getSampler(
            AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, true
        )
        setup = TextureSetup.singleTexture(textureView!!, sampler)
    }

    private companion object {
        const val MAX_MIP_LEVEL = 6

        fun mipLevels(width: Int, height: Int) =
            (31 - Integer.numberOfLeadingZeros(minOf(width, height))).coerceIn(0, MAX_MIP_LEVEL)
    }

}
