/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.timer;

public final class MSTimer
{

	private long time = -1L;

	public boolean hasTimePassed(final long millis)
	{
		return System.currentTimeMillis() >= time + millis;
	}

	public long hasTimeLeft(final long millis)
	{
		return millis + time - System.currentTimeMillis();
	}

	public long getTime()
	{
		return System.currentTimeMillis() - time;
	}

	public void reset()
	{
		time = System.currentTimeMillis();
	}
}
