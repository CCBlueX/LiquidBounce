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
package net.ccbluex.liquidbounce.render.engine

import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureSilentScreen
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.ClientUniformDefine
import net.ccbluex.liquidbounce.render.CachedUniform
import net.ccbluex.liquidbounce.render.createRenderPass
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.client.gui.screens.ChatScreen
import kotlin.math.ceil
import kotlin.math.exp

object BlurEffectRenderer : MinecraftShortcuts, EventListener {

    var isDrawingHudFramebuffer = false

    val overlayRenderTargetHolder = LazyRenderTargetHolder(
        "${LiquidBounce.CLIENT_NAME} BlurOverlay",
        useDepth = true,
    )

    private val intermediateTarget = LazyRenderTargetHolder(
        "${LiquidBounce.CLIENT_NAME} BlurIntermediate",
        useDepth = false,
    )

    private val overlaySampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)

    private val lastTimeScreenOpened = Chronometer()
    private var wasScreenOpen = false

    private data class BlurBlendUniform(val minAlpha: Float, val maxAlpha: Float)

    private data class BlurKernelUniform(val sigma: Float, val radius: Int)

    private val blurBlendUniform = CachedUniform<BlurBlendUniform>(ClientUniformDefine.GUI_BLUR) { value ->
        putFloat(value.minAlpha)
        putFloat(value.maxAlpha)
    }

    private val blurKernelUniform = CachedUniform<BlurKernelUniform>(ClientUniformDefine.GUI_BLUR_KERNEL) { value ->
        writeKernel(value.sigma, value.radius)
    }

    private fun hasNoFullScreen(): Boolean =
        mc.gui.screen() == null || mc.gui.screen() is ChatScreen || FeatureSilentScreen.shouldHide

    fun shouldDrawBlur(): Boolean = inGame && hasNoFullScreen() &&
        ModuleHud.running && ModuleHud.isBlurEffectActive

    fun blitBlurOverlay() {
        if (!isDrawingHudFramebuffer) {
            return
        }
        isDrawingHudFramebuffer = false

        val sigma = getSigma()
        val alphaBlendRange = ModuleHud.Blur.alphaBlendRange
        val kernelRadius = calculateKernelRadius(sigma)
        val blendUniform = blurBlendUniform.get(
            BlurBlendUniform(alphaBlendRange.start, alphaBlendRange.endInclusive)
        )
        val kernelUniform = blurKernelUniform.get(BlurKernelUniform(sigma, kernelRadius))

        val mainTarget = mc.gameRenderer.mainRenderTarget()
        val mainTexture = mainTarget.colorTextureView
        val overlayTexture = overlayRenderTargetHolder.get()!!.colorTextureView

        // Pass 1: Horizontal Gaussian blur into intermediate target
        val intermediate = intermediateTarget.initAndGet()
        intermediate.createRenderPass({ "GUI blur H pass" })
            .use { pass ->
                pass.setPipeline(ClientRenderPipelines.GuiBlurH)
                pass.bindTexture("texture0", mainTexture, overlaySampler)
                pass.setUniform(ClientUniformDefine.GUI_BLUR_KERNEL.uboName, kernelUniform)
                pass.draw(3, 1, 0, 0)
            }

        // Pass 2: Vertical Gaussian blur + overlay composite into main target
        mainTarget.createRenderPass({ "GUI blur V pass" })
            .use { pass ->
                pass.setPipeline(ClientRenderPipelines.GuiBlurV)
                pass.bindTexture("texture0", intermediate.colorTextureView, overlaySampler)
                pass.bindTexture("overlay", overlayTexture, overlaySampler)
                pass.setUniform(ClientUniformDefine.GUI_BLUR.uboName, blendUniform)
                pass.setUniform(ClientUniformDefine.GUI_BLUR_KERNEL.uboName, kernelUniform)
                pass.draw(3, 1, 0, 0)
            }

        // Blit overlay texture on top
        mainTexture!!
            .createRenderPass({ "GUI blur overlay blit pass" })
            .use { pass ->
                pass.setPipeline(ClientRenderPipelines.JCEF.Blit)
                pass.bindTexture("InSampler", overlayTexture, overlaySampler)
                pass.draw(3, 1, 0, 0)
            }
    }

    private fun getBlurRadiusFactor(): Float {
        val isScreenOpen = !hasNoFullScreen()

        if (isScreenOpen && !wasScreenOpen) {
            lastTimeScreenOpened.reset()
        }
        wasScreenOpen = isScreenOpen

        return if (isScreenOpen) {
            val x = (lastTimeScreenOpened.elapsed.toFloat() / 333.0F + 0.1F).coerceIn(0.0F, 1.0F)
            Easing.QUAD_OUT.transform(x)
        } else {
            1.0F
        }
    }

    private fun getSigma(): Float {
        return (ModuleHud.Blur.sigma * getBlurRadiusFactor()).coerceAtLeast(1.0F)
    }

    private fun calculateKernelRadius(sigma: Float): Int {
        return ceil(sigma * 3.0).toInt()
    }

    /**
     * Precomputes normalized Gaussian kernel weights packed as vec4[23] into the kernel UBO.
     * Weights are already normalized so the shader just sums weighted samples — no exp(), no division.
     */
    private fun Std140Builder.writeKernel(sigma: Float, kernelRadius: Int) {
        val count = kernelRadius * 2 + 1
        val raw = FloatArray(count)
        var total = 0.0f
        for (i in -kernelRadius..kernelRadius) {
            val w = exp(-0.5f * i * i / (sigma * sigma))
            raw[i + kernelRadius] = w
            total += w
        }
        // Normalize
        for (i in raw.indices) {
            raw[i] /= total
        }

        // Pack into vec4[23] = 92 slots, we have up to 91 weights
        for (vecIdx in 0 until 23) {
            val base = vecIdx * 4
            putVec4(
                raw.getOrElse(base) { 0.0f },
                raw.getOrElse(base + 1) { 0.0f },
                raw.getOrElse(base + 2) { 0.0f },
                raw.getOrElse(base + 3) { 0.0f },
            )
        }
        putInt(kernelRadius)
    }

}
