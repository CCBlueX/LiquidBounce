/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.util

import net.ccbluex.liquidbounce.api.minecraft.util.IScaledResolution
import net.minecraft.client.gui.ScaledResolution

class ScaledResolutionImpl(val wrapped: ScaledResolution) : IScaledResolution
{
	override val scaledWidth: Int
		get() = wrapped.scaledWidth
	override val scaledHeight: Int
		get() = wrapped.scaledHeight
	override val scaleFactor: Int
		get() = wrapped.scaleFactor

	override fun equals(other: Any?): Boolean = other is ScaledResolutionImpl && other.wrapped == wrapped
}

fun IScaledResolution.unwrap(): ScaledResolution = (this as ScaledResolutionImpl).wrapped
fun ScaledResolution.wrap(): IScaledResolution = ScaledResolutionImpl(this)
