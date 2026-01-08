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

@file:Suppress("NOTHING_TO_INLINE")

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTextureView
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import com.mojang.blaze3d.pipeline.RenderTarget
import java.util.OptionalDouble
import java.util.OptionalInt
import java.util.function.Supplier

inline fun RenderPass.bindDefaultUniforms() = RenderSystem.bindDefaultUniforms(this)

inline fun RenderPass.setUniformProjection() {
    RenderSystem.getProjectionMatrixBuffer()?.let { setUniform("Projection", it) }
}

inline fun RenderPass.setUniformFog() {
    RenderSystem.getShaderFog()?.let { setUniform("Fog", it) }
}

inline fun RenderPass.setUniformGlobals() {
    RenderSystem.getGlobalSettingsUniform()?.let { setUniform("Globals", it) }
}

inline fun RenderPass.setUniformLighting() {
    RenderSystem.getShaderLights()?.let { setUniform("Projection", it) }
}

private val RENDER_PASS_DEFAULT_LABEL = Supplier { LiquidBounce.CLIENT_NAME + " RenderPass" }

@JvmOverloads
fun RenderTarget.createRenderPass(
    labelGetter: Supplier<String> = RENDER_PASS_DEFAULT_LABEL,
    clearColor: OptionalInt = OptionalInt.empty(),
    clearDepth: OptionalDouble = OptionalDouble.empty(),
    useDepthAttachment: Boolean = true,
): RenderPass = newRenderPass(
    labelGetter,
    colorTextureView!!,
    clearColor,
    depthTextureView.takeIf { this.useDepth && useDepthAttachment },
    clearDepth,
)

/**
 * Color-only RenderPass.
 */
@JvmOverloads
fun GpuTextureView.createRenderPass(
    labelGetter: Supplier<String> = RENDER_PASS_DEFAULT_LABEL,
    clearColor: OptionalInt = OptionalInt.empty(),
): RenderPass = newRenderPass(labelGetter, colorAttachment = this, clearColor)

@Suppress("NOTHING_TO_INLINE")
private inline fun newRenderPass(
    labelGetter: Supplier<String> = RENDER_PASS_DEFAULT_LABEL,
    colorAttachment: GpuTextureView,
    clearColor: OptionalInt = OptionalInt.empty(),
    depthAttachment: GpuTextureView? = null,
    clearDepth: OptionalDouble = OptionalDouble.empty(),
): RenderPass = gpuDevice.createCommandEncoder().createRenderPass(
    labelGetter,
    colorAttachment,
    clearColor,
    depthAttachment,
    clearDepth,
)
