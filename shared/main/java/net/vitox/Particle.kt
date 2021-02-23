package net.vitox

import net.ccbluex.liquidbounce.LiquidBounce.wrapper
import net.ccbluex.liquidbounce.api.minecraft.client.IMinecraft
import net.ccbluex.liquidbounce.api.minecraft.util.IScaledResolution
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import net.vitox.particle.util.RenderUtils
import kotlin.random.Random

/**
 * Particle API This Api is free2use But u have to mention me.
 *
 * @author  Vitox
 * @version 3.0
 */
@SideOnly(Side.CLIENT)
internal class Particle(var x: Float, var y: Float)
{
	private val ySpeed = Random.nextInt(5).toFloat()
	private val xSpeed = Random.nextInt(5).toFloat()

	val size = genRandom()

	var height = 0
	var width = 0

	fun connect(x: Float, y: Float)
	{
		RenderUtils.connectPoints(this.x, this.y, x, y)
	}

	fun interpolation()
	{
		(0..64).asSequence().map { it * 0.015625f }.forEach { f ->
			val p1 = lint1(f)
			val p2 = lint2(f)

			if (p1 != p2)
			{
				y -= f
				x -= f
			}
		}
	}

	fun fall()
	{
		val mc: IMinecraft = wrapper.minecraft
		val resolution: IScaledResolution = wrapper.classProvider.createScaledResolution(mc)

		y += ySpeed
		x += xSpeed

		if (y > mc.displayHeight) y = 1f
		if (x > mc.displayWidth) x = 1f

		if (x < 1) x = resolution.scaledWidth.toFloat()
		if (y < 1) y = resolution.scaledHeight.toFloat()
	}

	companion object
	{
		private fun lint1(f: Float): Float = 1.02f * (1.0f - f) + f
		private fun lint2(f: Float): Float = 1.02f + f * (1.0f - 1.02f)
		private fun genRandom(): Float = 0.3f + Random.nextFloat() * (0.6f - 0.3f + 1.0f)
	}
}
