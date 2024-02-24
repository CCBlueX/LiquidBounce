/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.render.shader.shaders

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.MinecraftFramebufferShader

object BlurUIShader : MinecraftFramebufferShader("blur_ui") {

    fun begin(shaderTexture: Int) {
        this.beginInternal()

        RenderSystem.setShaderTexture(1, shaderTexture)
    }

    fun setColor(color: Color4b) {
        this.vertexConsumerProvider?.setColor(color.r, color.g, color.b, color.a)
    }

}
