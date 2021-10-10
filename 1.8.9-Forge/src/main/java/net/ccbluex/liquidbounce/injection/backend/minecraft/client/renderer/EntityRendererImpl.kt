/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.client.renderer

import net.ccbluex.liquidbounce.api.minecraft.client.renderer.IEntityRenderer
import net.ccbluex.liquidbounce.api.minecraft.client.shader.IShaderGroup
import net.ccbluex.liquidbounce.api.minecraft.util.IResourceLocation
import net.ccbluex.liquidbounce.injection.backend.minecraft.client.shader.wrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.util.unwrap
import net.minecraft.client.renderer.EntityRenderer

class EntityRendererImpl(val wrapped: EntityRenderer) : IEntityRenderer
{
	override val shaderGroup: IShaderGroup?
		get() = wrapped.shaderGroup?.wrap()

	override fun disableLightmap() = wrapped.disableLightmap()

	override fun isShaderActive(): Boolean = wrapped.isShaderActive

	override fun loadShader(resourceLocation: IResourceLocation) = wrapped.loadShader(resourceLocation.unwrap())

	override fun stopUseShader() = wrapped.stopUseShader()
	override fun setupCameraTransform(partialTicks: Float, pass: Int) = wrapped.setupCameraTransform(partialTicks, pass)

	override fun setupOverlayRendering() = wrapped.setupOverlayRendering()

	override fun equals(other: Any?): Boolean = other is EntityRendererImpl && other.wrapped == wrapped
}

fun IEntityRenderer.unwrap(): EntityRenderer = (this as EntityRendererImpl).wrapped
fun EntityRenderer.wrap(): IEntityRenderer = EntityRendererImpl(this)
