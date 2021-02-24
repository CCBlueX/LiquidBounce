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
class ParticleGenerator(amount: Int)
{
	private val particles: MutableCollection<Particle>
	private val amount: Int
	private var prevWidth = 0
	private var prevHeight = 0
	fun draw(mouseX: Int, mouseY: Int)
	{
		val mc: IMinecraft = wrapper.minecraft
		val displayWidth = mc.displayWidth
		val displayHeight = mc.displayHeight
		if (particles.isEmpty() || prevWidth != displayWidth || prevHeight != displayHeight)
		{
			particles.clear()
			create()
		}
		prevWidth = displayWidth
		prevHeight = displayHeight
		particles.forEach { particle: Particle ->
			particle.fall()
			particle.interpolation()

			val range = 50
			val mouseOver = mouseX >= particle.x - range && mouseY >= particle.y - range && mouseX <= particle.x + range && mouseY <= particle.y + range

			val particleX = particle.x
			val particleY = particle.y

			if (mouseOver) particles.filter { it.x > particleX }.filter { it.x - particleX < range }.filter { particleX - it.x < range }.filter { it.y > particleY && it.y - particleY < range || particleY > it.y && particleY - it.y < range }.forEach { particle.connect(it.x, it.y) }

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
		particles = ArrayList(amount)
		this.amount = amount
	}
}
