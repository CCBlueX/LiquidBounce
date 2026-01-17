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

@file:Suppress("NOTHING_TO_INLINE", "TooManyFunctions")

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTextureView
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import org.joml.Matrix4fc
import java.util.OptionalDouble
import java.util.OptionalInt
import java.util.function.Supplier

inline fun RenderPass.bindDefaultUniforms() = RenderSystem.bindDefaultUniforms(this)

inline fun RenderPass.bindProjectionUniform() {
    RenderSystem.getProjectionMatrixBuffer()?.let { setUniform("Projection", it) }
}

inline fun RenderPass.bindFogUniform() {
    RenderSystem.getShaderFog()?.let { setUniform("Fog", it) }
}

inline fun RenderPass.bindGlobalsUniform() {
    RenderSystem.getGlobalSettingsUniform()?.let { setUniform("Globals", it) }
}

inline fun RenderPass.bindLightingUniform() {
    RenderSystem.getShaderLights()?.let { setUniform("Lighting", it) }
}

inline fun RenderPass.bindDynamicTransformsUniform(gpuBufferSlice: GpuBufferSlice) {
    setUniform("DynamicTransforms", gpuBufferSlice)
}

inline fun RenderPass.setupGlobalScissor() {
    val scissorState = RenderSystem.getScissorStateForRenderTypeDraws()
    if (scissorState.enabled()) {
        enableScissor(
            scissorState.x(),
            scissorState.y(),
            scissorState.width(),
            scissorState.height()
        )
    }
}

@JvmOverloads
fun getDynamicTransformsUniform(
    modelView: Matrix4fc? = null,
    colorModulator: Color4b = Color4b.WHITE,
): GpuBufferSlice {
    val slice = RenderSystem.getDynamicUniforms()
        .writeTransform(
            modelView ?: RenderSystem.getModelViewMatrix(),
            colorModulator.toVector4f(RenderPassRenderState.colorModulator),
            RenderPassRenderState.modelOffset,
            RenderPassRenderState.textureMatrix,
        )

    return slice
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
