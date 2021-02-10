package net.vitox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.client.IMinecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.vitox.particle.util.RenderUtils;

/**
 * Particle API This Api is free2use But u have to mention me.
 *
 * @author  Vitox
 * @version 3.0
 */
@SideOnly(Side.CLIENT)
public class ParticleGenerator
{

	private final Collection<Particle> particles;
	private final int amount;

	private int prevWidth;
	private int prevHeight;

	public ParticleGenerator(final int amount)
	{
		particles = new ArrayList<>(amount);
		this.amount = amount;
	}

	public void draw(final int mouseX, final int mouseY)
	{
		final IMinecraft mc = LiquidBounce.wrapper.getMinecraft();
		final int displayWidth = mc.getDisplayWidth();
		final int displayHeight = mc.getDisplayHeight();

		if (particles.isEmpty() || prevWidth != displayWidth || prevHeight != displayHeight)
		{
			particles.clear();
			create();
		}

		prevWidth = displayWidth;
		prevHeight = displayHeight;

		for (final Particle particle : particles)
		{
			particle.fall();
			particle.interpolation();

			final int range = 50;
			final boolean mouseOver = mouseX >= particle.x - range && mouseY >= particle.y - range && mouseX <= particle.x + range && mouseY <= particle.y + range;

			if (mouseOver)
				particles.stream().filter(part -> part.getX() > particle.getX() && part.getX() - particle.getX() < range && particle.getX() - part.getX() < range && (part.getY() > particle.getY() && part.getY() - particle.getY() < range || particle.getY() > part.getY() && particle.getY() - part.getY() < range)).forEach(connectable -> particle.connect(connectable.getX(), connectable.getY()));

			RenderUtils.drawCircle(particle.getX(), particle.getY(), particle.size, 0xffFFFFFF);
		}
	}

	private void create()
	{
		final Random random = new Random();

		final IMinecraft mc = LiquidBounce.wrapper.getMinecraft();
		final int displayWidth = mc.getDisplayWidth();
		final int displayHeight = mc.getDisplayHeight();

		for (int i = 0; i < amount; i++)
			particles.add(new Particle(random.nextInt(displayWidth), random.nextInt(displayHeight)));
	}
}
