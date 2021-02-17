package net.vitox;

import java.util.Random;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.client.IMinecraft;
import net.ccbluex.liquidbounce.api.minecraft.util.IScaledResolution;
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
class Particle
{

	public float x;
	public float y;
	public final float size;
	private final float ySpeed = new Random().nextInt(5);
	private final float xSpeed = new Random().nextInt(5);
	private int height;
	private int width;

	Particle(final int x, final int y)
	{
		this.x = x;
		this.y = y;
		size = genRandom();
	}

	private static float lint1(final float f)
	{
		return 1.02f * (1.0f - f) + f;
	}

	private static float lint2(final float f)
	{
		return 1.02f + f * (1.0f - 1.02f);
	}

	void connect(final float x, final float y)
	{
		RenderUtils.connectPoints(this.x, this.y, x, y);
	}

	public int getHeight()
	{
		return height;
	}

	public void setHeight(final int height)
	{
		this.height = height;
	}

	public int getWidth()
	{
		return width;
	}

	public void setWidth(final int width)
	{
		this.width = width;
	}

	public float getX()
	{
		return x;
	}

	public void setX(final int x)
	{
		this.x = x;
	}

	public float getY()
	{
		return y;
	}

	public void setY(final int y)
	{
		this.y = y;
	}

	void interpolation()
	{
		for (int n = 0; n <= 64; ++n)
		{
			final float f = n * 0.015625f;
			final float p1 = lint1(f);
			final float p2 = lint2(f);

			if (p1 != p2)
			{
				y -= f;
				x -= f;
			}
		}
	}

	void fall()
	{
		final IMinecraft mc = LiquidBounce.wrapper.getMinecraft();
		final IScaledResolution scaledResolution = LiquidBounce.wrapper.getClassProvider().createScaledResolution(mc);
		y += ySpeed;
		x += xSpeed;

		if (y > mc.getDisplayHeight())
			y = 1;

		if (x > mc.getDisplayWidth())
			x = 1;

		if (x < 1)
			x = scaledResolution.getScaledWidth();

		if (y < 1)
			y = scaledResolution.getScaledHeight();
	}

	private static float genRandom()
	{
		return 0.3f + (float) Math.random() * (0.6f - 0.3f + 1.0F);
	}
}
