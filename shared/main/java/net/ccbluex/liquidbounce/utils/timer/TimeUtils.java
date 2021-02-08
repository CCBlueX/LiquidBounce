/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.timer;

import net.ccbluex.liquidbounce.utils.misc.RandomUtils;

import java.util.concurrent.TimeUnit;

public final class TimeUtils
{

	public static long randomDelay(final int minDelay, final int maxDelay)
	{
		return RandomUtils.nextInt(minDelay, maxDelay);
	}

	public static long randomClickDelay(final int minCPS, final int maxCPS)
	{
		return (long) (Math.random() * (1000 / minCPS - 1000 / maxCPS + 1) + 1000 / maxCPS);
	}

	public static String NanosecondsToString(final long nanoseconds)
	{
		return nanoseconds + "ns, " + TimeUnit.NANOSECONDS.toMicros(nanoseconds) + "us, " + TimeUnit.NANOSECONDS.toMillis(nanoseconds) + "ms";
	}

	private TimeUtils() {
	}
}
