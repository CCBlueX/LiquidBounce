/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render;

import static java.lang.Math.pow;

import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper;

public final class AnimationUtils
{
	/**
	 * In-out-easing function https://github.com/jesusgollonet/processing-penner-easing
	 *
	 * @param  t
	 *           Current iteration
	 * @param  d
	 *           Total iterations
	 * @return   Eased value
	 */
	public static float easeOut(float t, final float d)
	{
		return (t = t / d - 1) * t * t + 1;
	}

	/**
	 * Source: https://easings.net/#easeOutElastic
	 *
	 * @return A value larger than 0
	 */
	public static float easeOutElastic(final float x)
	{
		final float c4 = 2 * (float) Math.PI / 3.0f;

		return x == 0 ? 0 : (float) (x == 1 ? 1 : StrictMath.pow(2, -10 * x) * WMathHelper.sin((x * 10 - 0.75f) * c4) + 1);
	}

	private AnimationUtils() {
	}
}
