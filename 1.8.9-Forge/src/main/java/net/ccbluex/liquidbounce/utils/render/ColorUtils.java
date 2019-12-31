package net.ccbluex.liquidbounce.utils.render;

import java.awt.*;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
public final class ColorUtils {

    public static Color rainbow() {
        Color currentColor = new Color(Color.HSBtoRGB((System.nanoTime() + 400000L) / 10000000000F % 1, 1f, 1f));
        return new Color((currentColor.getRed() / 255f) * 1F, (currentColor.getGreen() / 255f) * 1F, (currentColor.getBlue() / 255f) * 1F, currentColor.getAlpha() / 255f);
    }

    public static Color rainbow(final long offset) {
        Color currentColor = new Color(Color.HSBtoRGB((System.nanoTime() + offset) / 10000000000F % 1, 1f, 1f));
        return new Color((currentColor.getRed() / 255f) * 1F, (currentColor.getGreen() / 255f) * 1F, (currentColor.getBlue() / 255f) * 1F, currentColor.getAlpha() / 255f);
    }

    public static Color rainbow(final float alpha) {
        return rainbow(400000L, alpha);
    }

    public static Color rainbow(final int alpha) {
        return rainbow(400000L, alpha / 255);
    }

    public static Color rainbow(final long offset, final int alpha) {
        return rainbow(offset, (float) alpha / 255);
    }

    public static Color rainbow(final long offset, final float alpha) {
        final Color currentColor = new Color(Color.HSBtoRGB((System.nanoTime() + offset) / 10000000000F % 1, 1f, 1f));
        return new Color((currentColor.getRed() / 255F) * 1F, (currentColor.getGreen() / 255f) * 1F, (currentColor.getBlue() / 255f) * 1F, alpha);
    }
}
