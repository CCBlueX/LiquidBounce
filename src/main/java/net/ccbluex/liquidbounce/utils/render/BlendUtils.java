/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.utils.render;

import java.awt.*;

public enum BlendUtils {
    GREEN("§A"),
    GOLD("§6"),
    RED("§C");

    String colorCode;

    BlendUtils(String colorCode) {
        this.colorCode = colorCode;
    }

    public static Color getColorWithOpacity(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Color getHealthColor(float health, float maxHealth) {
        float[] fractions = new float[]{0.0F, 0.5F, 1.0F};
        Color[] colors = new Color[]{new Color(108, 0, 0), new Color(255, 51, 0), Color.GREEN};
        float progress = health / maxHealth;
        return blendColors(fractions, colors, progress).brighter();
    }

    public static Color blendColors(float[] fractions, Color[] colors, float progress) {
        if (fractions.length == colors.length) {
            int[] indices = getFractionIndices(fractions, progress);
            float[] range = new float[]{fractions[indices[0]], fractions[indices[1]]};
            Color[] colorRange = new Color[]{colors[indices[0]], colors[indices[1]]};
            float max = range[1] - range[0];
            float value = progress - range[0];
            float weight = value / max;
            Color color = blend(colorRange[0], colorRange[1], 1.0F - weight);
            return color;
        } else {
            throw new IllegalArgumentException("Fractions and colours must have equal number of elements");
        }
    }

    public static int[] getFractionIndices(float[] fractions, float progress) {
        int[] range = new int[2];

        int startPoint;
        for(startPoint = 0; startPoint < fractions.length && fractions[startPoint] <= progress; ++startPoint) {
        }

        if (startPoint >= fractions.length) {
            startPoint = fractions.length - 1;
        }

        range[0] = startPoint - 1;
        range[1] = startPoint;
        return range;
    }

    public static Color blend(Color color1, Color color2, double ratio) {
        float r = (float)ratio;
        float ir = 1.0F - r;
        float[] rgb1 = color1.getColorComponents(new float[3]);
        float[] rgb2 = color2.getColorComponents(new float[3]);
        float red = rgb1[0] * r + rgb2[0] * ir;
        float green = rgb1[1] * r + rgb2[1] * ir;
        float blue = rgb1[2] * r + rgb2[2] * ir;
        if (red < 0.0F) {
            red = 0.0F;
        } else if (red > 255.0F) {
            red = 255.0F;
        }

        if (green < 0.0F) {
            green = 0.0F;
        } else if (green > 255.0F) {
            green = 255.0F;
        }

        if (blue < 0.0F) {
            blue = 0.0F;
        } else if (blue > 255.0F) {
            blue = 255.0F;
        }

        Color color3 = null;

        try {
            color3 = new Color(red, green, blue);
        } catch (IllegalArgumentException var13) {
        }

        return color3;
    }
}
