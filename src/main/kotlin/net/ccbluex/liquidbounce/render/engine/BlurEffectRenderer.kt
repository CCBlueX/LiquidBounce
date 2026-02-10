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

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureSilentScreen
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.ClientUniformDefine
import net.ccbluex.liquidbounce.render.createRenderPass
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.math.Easing
import net.ccbluex.liquidbounce.utils.render.writeStd140
import net.minecraft.client.gui.screens.ChatScreen

object BlurEffectRenderer : MinecraftShortcuts, EventListener {

    var isDrawingHudFramebuffer = false

    val overlayRenderTargetHolder = LazyRenderTargetHolder(
        "${LiquidBounce.CLIENT_NAME} BlurOverlay",
        useDepth = true,
    )

    private val overlaySampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)

    private val lastTimeScreenOpened = Chronometer()
    private var wasScreenOpen = false

    private val GUI_BLUR_UNIFORM_BUFFER = ClientUniformDefine.GUI_BLUR.createSingleBuffer()

    private var lastBlurRadius = Float.MIN_VALUE
    private var lastAlphaBlendRange = 0f..1f

    private fun hasNoFullScreen(): Boolean =
        mc.screen == null || mc.screen is ChatScreen || FeatureSilentScreen.shouldHide

    fun shouldDrawBlur(): Boolean = inGame && hasNoFullScreen() &&
        ModuleHud.running && ModuleHud.isBlurEffectActive

    fun blitBlurOverlay() {
        if (!isDrawingHudFramebuffer) {
            return
        }
        isDrawingHudFramebuffer = false

        // Write UBO
        val blurRadius = getBlurRadius()
        val alphaBlendRange = ModuleHud.Blur.alphaBlendRange
        if (blurRadius != lastBlurRadius || alphaBlendRange != lastAlphaBlendRange) {
            GUI_BLUR_UNIFORM_BUFFER.writeStd140 {
                putFloat(blurRadius)
                putFloat(ModuleHud.Blur.alphaBlendRange.start)
                putFloat(ModuleHud.Blur.alphaBlendRange.endInclusive)
            }
            lastBlurRadius = blurRadius
            lastAlphaBlendRange = alphaBlendRange
        }

        val overlayTexture = overlayRenderTargetHolder.raw!!.colorTextureView
        mc.mainRenderTarget
            .createRenderPass({ "GUI blur pass" })
            .use { pass ->
                // Draw blur areas
                pass.setPipeline(ClientRenderPipelines.GuiBlur)
                pass.bindTexture("texture0", mc.mainRenderTarget.colorTextureView, overlaySampler)
                pass.bindTexture("overlay", overlayTexture, overlaySampler)
                pass.setUniform(ClientUniformDefine.GUI_BLUR.uboName, GUI_BLUR_UNIFORM_BUFFER)
                pass.draw(0, 3)
            }

        mc.mainRenderTarget.colorTextureView!!
            .createRenderPass({ "GUI blur overlay blit pass" })
            .use { pass ->
                // Blit overlay texture
                // @see RenderTarget.blitAndBlendToTexture
                pass.setPipeline(ClientRenderPipelines.JCEF.Blit)
                pass.bindTexture("InSampler", overlayTexture, overlaySampler)
                pass.draw(0, 3)
            }
    }

    private fun getBlurRadiusFactor(): Float {
        val isScreenOpen = !hasNoFullScreen()

        if (isScreenOpen && !wasScreenOpen) {
            lastTimeScreenOpened.reset()
        }
        wasScreenOpen = isScreenOpen

        return if (isScreenOpen) {
            val x = (lastTimeScreenOpened.elapsed.toFloat() / 333.0F + 0.1F).coerceIn(0.0F..1.0F)
            Easing.QUAD_OUT.transform(x)
        } else {
            1.0F
        }
    }

    private fun getBlurRadius(): Float {
        return (this.getBlurRadiusFactor() * 20.0F).coerceIn(5.0F, 20.0F)
    }

}
