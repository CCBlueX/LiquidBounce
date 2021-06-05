package net.vitox

import net.ccbluex.liquidbounce.LiquidBounce.wrapper
import net.ccbluex.liquidbounce.api.minecraft.client.IMinecraft
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import net.vitox.particle.util.RenderUtils
import java.util.*

/**
 * Particle API This Api is free2use But u have to mention me.
 *
 * @author  Vitox
 * @version 3.0
 */
@SideOnly(Side.CLIENT)
class ParticleGenerator(private val amount: Int, private val mouseOverRange: Int)
{
	private val particles: MutableCollection<Particle>

	private var prevWidth = 0
	private var prevHeight = 0

	fun draw(mouseX: Int, mouseY: Int)
	{
		val mc: IMinecraft = wrapper.minecraft

		val displayWidth = mc.displayWidth
		val displayHeight = mc.displayHeight

		val displayWidthF = displayWidth.toFloat()
		val displayHeightF = displayHeight.toFloat()

		val resolution = wrapper.classProvider.createScaledResolution(mc)
		val scaledWidth = resolution.scaledWidth.toFloat()
		val scaledHeight = resolution.scaledHeight.toFloat()

		if (particles.isEmpty() || prevWidth != displayWidth || prevHeight != displayHeight)
		{
			particles.clear()
			create()
		}

		prevWidth = displayWidth
		prevHeight = displayHeight

		particles.forEach { particle: Particle ->
			particle.fall(displayWidthF, displayHeightF, scaledWidth, scaledHeight)
			particle.interpolation()

			val particleX = particle.x
			val particleY = particle.y

			val mouseOver = mouseX >= particleX - mouseOverRange && mouseY >= particleY - mouseOverRange && mouseX <= particleX + mouseOverRange && mouseY <= particleY + mouseOverRange

			if (mouseOver) particles.filter {
				val x = it.x
				val y = it.y

				(x > particleX && x - particleX < mouseOverRange && particleX - x < mouseOverRange) && (y > particleY && y - particleY < mouseOverRange || particleY > y && particleY - y < mouseOverRange)
			}.forEach { particle.connect(it.x, it.y) }

			RenderUtils.drawCircle(particleX, particleY, particle.size, -0x1)
		}
	}

	private fun create()
	{
		val random = Random()
		val mc: IMinecraft = wrapper.minecraft
		val displayWidth = mc.displayWidth
		val displayHeight = mc.displayHeight

		repeat(amount) { particles.add(Particle(random.nextInt(displayWidth).toFloat(), random.nextInt(displayHeight).toFloat())) }
	}

	init
	{
		particles = ArrayDeque(amount)
	}
}
