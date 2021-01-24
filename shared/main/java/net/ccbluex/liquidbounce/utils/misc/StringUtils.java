/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.misc;

import java.util.Arrays;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class StringUtils
{

	public static String toCompleteString(final String[] args, final int start)
	{
		if (args.length <= start)
			return "";

		return String.join(" ", Arrays.copyOfRange(args, start, args.length));
	}

	public static String replace(final String string, final String searchChars, String replaceChars)
	{
		if (string.isEmpty() || searchChars.isEmpty() || searchChars.equals(replaceChars))
			return string;

		if (replaceChars == null)
			replaceChars = "";

		final int stringLength = string.length();
		final int searchCharsLength = searchChars.length();
		final StringBuilder stringBuilder = new StringBuilder(string);

		for (int i = 0; i < stringLength; i++)
		{
			final int start = stringBuilder.indexOf(searchChars, i);

			if (start == -1)
			{
				if (i == 0)
					return string;

				return stringBuilder.toString();
			}

			stringBuilder.replace(start, start + searchCharsLength, replaceChars);
		}

		return stringBuilder.toString();
	}

	public static String getHorizontalFacing(final float yaw)
	{
		final String[] facings =
		{
				"S", "SE", "E", "NE", "N", "NW", "W", "SW"
		};
		return facings[(int) Math.abs(Math.floor(yaw * facings.length / 360 + 0.5)) & facings.length - 1];
	}

	public static String getHorizontalFacingAdv(final float yaw)
	{
		final String[] facings =
		{
				"S", "SSE", "SE", "SEE", "E", "NEE", "NE", "NNE", "N", "NNW", "NW", "NWW", "W", "SWW", "SW", "SSW"
		};
		return facings[(int) Math.abs(Math.floor(yaw * facings.length / 360 + 0.5)) & facings.length - 1];
	}

	public static String getHorizontalFacingTowards(final float yaw)
	{
		final String[] facings =
		{
				"+Z", "+X +Z", "+X", "+X -Z", "-Z", "-X -Z", "-Z", "-X +Z"
		};
		return facings[(int) Math.abs(Math.floor(yaw * facings.length / 360 + 0.5)) & facings.length - 1];
	}

	private StringUtils() {
	}
}
