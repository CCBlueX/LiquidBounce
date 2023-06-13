/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.base.ultralight.impl.renderer

import com.labymedia.ultralight.UltralightView
import com.labymedia.ultralight.config.UltralightViewConfig
import com.labymedia.ultralight.gpu.UltralightOpenGLGPUDriverNative
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext

/**
 * A gpu view renderer
 */
class GpuViewRenderer(val driver: UltralightOpenGLGPUDriverNative) : ViewRenderer {

    var window = 0L

    override fun setupConfig(viewConfig: UltralightViewConfig) {
        viewConfig.isAccelerated(true)

        window = mc.window.handle
    }

    override fun render(view: UltralightView, context: DrawContext) {
        driver.setActiveWindow(window)

        // DrawableHelper.fill(matrices, 0, 0, mc.window.scaledWidth, mc.window.scaledHeight, Color(1, 1, 1, 1).rgb)

        RenderSystem.clearColor(0f, 0f, 0f, 0f)

        if (driver.hasCommandsPending()) {
            driver.drawCommandList()
        }

        RenderSystem.clearColor(0f, 1f, 0f, 1f)

        val renderTarget = view.renderTarget()
        val textureId = renderTarget.textureId

        driver.bindTexture(0, textureId)

        // context.drawTexture(matrices, 0, 0, 1, 0.0f, 0.0f, mc.window.scaledWidth, mc.window.scaledHeight, mc.window.scaledWidth, mc.window.scaledHeight)
    }

    override fun delete() {
    }

}
