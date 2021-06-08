/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.gl.ShaderEffect
import net.minecraft.client.render.OutlineVertexConsumerProvider
import net.minecraft.util.Identifier

abstract class MinecraftFramebufferShader(private val shaderName: String) {
    var framebuffer: Framebuffer? = null
    var vertexConsumerProvider: OutlineVertexConsumerProvider? = null

    var isDirty = false
        private set

    private var shaderEffect: ShaderEffect? = null

    fun load() {
        val outlinesShader = ShaderEffect(
            mc.textureManager,
            mc.resourceManager,
            mc.framebuffer,
            Identifier("liquidbounce", "shaders/post/$shaderName.json")
        )

        outlinesShader.setupDimensions(mc.window.framebufferWidth, mc.window.framebufferHeight)

        framebuffer = outlinesShader.getSecondaryTarget("final")
        vertexConsumerProvider = OutlineVertexConsumerProvider(mc.bufferBuilders.entityVertexConsumers)

        this.shaderEffect = outlinesShader
    }

    fun close() {
        shaderEffect?.close()

        this.shaderEffect = null
    }

    protected fun beginInternal() {
        // Clear the buffer if it is dirty
        if (this.isDirty) {
            assureLoaded(framebuffer).clear(MinecraftClient.IS_SYSTEM_MAC)

            this.isDirty = false
        }

        mc.framebuffer.beginWrite(false)
    }

    fun end(tickDelta: Float) {
        // Render the framebuffer if something was rendered to it
        if (this.isDirty) {
            val framebuffer = assureLoaded(framebuffer)

            val originalFramebuffer = mc.worldRenderer.entityOutlinesFramebuffer

            mc.worldRenderer.entityOutlinesFramebuffer = framebuffer

            vertexConsumerProvider!!.draw()

            mc.worldRenderer.entityOutlinesFramebuffer = originalFramebuffer

            shaderEffect!!.render(tickDelta)
        }

        mc.framebuffer.beginWrite(false)
    }

    fun setDirty() {
        this.isDirty = true
    }

    fun drawFramebuffer() {
        assureLoaded(framebuffer).draw(mc.window.framebufferWidth, mc.window.framebufferHeight, false)
    }

    fun onResized(width: Int, height: Int) {
        this.shaderEffect?.setupDimensions(width, height)
    }

    protected fun setUniform1f(name: String, value: Float) {
        assureLoaded(this.shaderEffect).passes[0].program.getUniformByName(name)?.set(value)
            ?: throw IllegalArgumentException("There is no uniform with the name $name")
    }

    private inline fun <reified T> assureLoaded(t: T?): T =
        t ?: throw IllegalStateException("${this.shaderName} is not loaded")
}
