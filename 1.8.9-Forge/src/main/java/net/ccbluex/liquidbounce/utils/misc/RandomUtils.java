/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.misc;

import java.util.Random;

public final class RandomUtils {

    public static int nextInt(final int startInclusive, final int endExclusive) {
        if (endExclusive - startInclusive <= 0)
            return startInclusive;

        return startInclusive + new Random().nextInt(endExclusive - startInclusive);
    }

    public static double nextDouble(final double startInclusive, final double endInclusive) {
        if(startInclusive == endInclusive || endInclusive - startInclusive <= 0D)
            return startInclusive;

        return startInclusive + ((endInclusive - startInclusive) * Math.random());
    }

    public static float nextFloat(final float startInclusive, final float endInclusive) {
        if(startInclusive == endInclusive || endInclusive - startInclusive <= 0F)
            return startInclusive;

        return (float) (startInclusive + ((endInclusive - startInclusive) * Math.random()));
    }

    public static String randomNumber(final int length) {
        return random(length, "123456789");
    }

    public static String randomString(final int length) {
        return random(length, "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
    }

    public static String random(final int length, final String chars) {
        return random(length, chars.toCharArray());
    }

    public static String random(final int length, final char[] chars) {
        final StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < length; i++)
            stringBuilder.append(chars[new Random().nextInt(chars.length)]);
        return stringBuilder.toString();
    }
}
