/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 *
 * This code belongs to WYSI-Foundation. Please give credits when using this in your repository.
 */
package net.ccbluex.liquidbounce.features.module.modules.render;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.utils.render.BlendUtils;
import net.ccbluex.liquidbounce.value.IntegerValue;

import java.awt.*;
import java.lang.reflect.Field;

public class ColorMixer extends Module {

    public ColorMixer() {
        super("ColorMixer", ModuleCategory.RENDER);
    }

    private static float[] lastFraction = new float[]{};
    public static Color[] lastColors = new Color[]{};

    public final IntegerValue blendAmount = new IntegerValue("Mixer-Amount", 2, 2, 10) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            regenerateColors(oldValue != newValue);
        }
    };
    /*
    @Override
    public void onInitialize() {
        regenerateColors();
    }
    */
    public final ColorElement col1RedValue = new ColorElement(1, ColorElement.Material.RED);
    public final ColorElement col1GreenValue = new ColorElement(1, ColorElement.Material.GREEN);
    public final ColorElement col1BlueValue = new ColorElement(1, ColorElement.Material.BLUE);

    public final ColorElement col2RedValue = new ColorElement(2, ColorElement.Material.RED);
    public final ColorElement col2GreenValue = new ColorElement(2, ColorElement.Material.GREEN);
    public final ColorElement col2BlueValue = new ColorElement(2, ColorElement.Material.BLUE);

    public final ColorElement col3RedValue = new ColorElement(3, ColorElement.Material.RED, blendAmount);
    public final ColorElement col3GreenValue = new ColorElement(3, ColorElement.Material.GREEN, blendAmount);
    public final ColorElement col3BlueValue = new ColorElement(3, ColorElement.Material.BLUE, blendAmount);

    public final ColorElement col4RedValue = new ColorElement(4, ColorElement.Material.RED, blendAmount);
    public final ColorElement col4GreenValue = new ColorElement(4, ColorElement.Material.GREEN, blendAmount);
    public final ColorElement col4BlueValue = new ColorElement(4, ColorElement.Material.BLUE, blendAmount);

    public final ColorElement col5RedValue = new ColorElement(5, ColorElement.Material.RED, blendAmount);
    public final ColorElement col5GreenValue = new ColorElement(5, ColorElement.Material.GREEN, blendAmount);
    public final ColorElement col5BlueValue = new ColorElement(5, ColorElement.Material.BLUE, blendAmount);

    public final ColorElement col6RedValue = new ColorElement(6, ColorElement.Material.RED, blendAmount);
    public final ColorElement col6GreenValue = new ColorElement(6, ColorElement.Material.GREEN, blendAmount);
    public final ColorElement col6BlueValue = new ColorElement(6, ColorElement.Material.BLUE, blendAmount);

    public final ColorElement col7RedValue = new ColorElement(7, ColorElement.Material.RED, blendAmount);
    public final ColorElement col7GreenValue = new ColorElement(7, ColorElement.Material.GREEN, blendAmount);
    public final ColorElement col7BlueValue = new ColorElement(7, ColorElement.Material.BLUE, blendAmount);

    public final ColorElement col8RedValue = new ColorElement(8, ColorElement.Material.RED, blendAmount);
    public final ColorElement col8GreenValue = new ColorElement(8, ColorElement.Material.GREEN, blendAmount);
    public final ColorElement col8BlueValue = new ColorElement(8, ColorElement.Material.BLUE, blendAmount);

    public final ColorElement col9RedValue = new ColorElement(9, ColorElement.Material.RED, blendAmount);
    public final ColorElement col9GreenValue = new ColorElement(9, ColorElement.Material.GREEN, blendAmount);
    public final ColorElement col9BlueValue = new ColorElement(9, ColorElement.Material.BLUE, blendAmount);

    public final ColorElement col10RedValue = new ColorElement(10, ColorElement.Material.RED, blendAmount);
    public final ColorElement col10GreenValue = new ColorElement(10, ColorElement.Material.GREEN, blendAmount);
    public final ColorElement col10BlueValue = new ColorElement(10, ColorElement.Material.BLUE, blendAmount);

    public static Color getMixedColor(int index, int seconds) {
        final ColorMixer colMixer = (ColorMixer) LiquidBounce.INSTANCE.getModuleManager().getModule(ColorMixer.class);
        if (colMixer == null) return Color.white;

        if (lastColors.length <= 0 || lastFraction.length <= 0) regenerateColors(true); // just to make sure it won't go white

        return BlendUtils.blendColors(lastFraction, lastColors, (System.currentTimeMillis() + index) % (seconds * 1000) / (float) (seconds * 1000));
    }

    public static void regenerateColors(boolean forceValue) {
        final ColorMixer colMixer = (ColorMixer) LiquidBounce.INSTANCE.getModuleManager().getModule(ColorMixer.class);

        if (colMixer == null) return;

        // color generation
        if (forceValue || lastColors.length <= 0 || lastColors.length != (colMixer.blendAmount.get() * 2) - 1) {
            Color[] generator = new Color[(colMixer.blendAmount.get() * 2) - 1];

            // reflection is cool
            for (int i = 1; i <= colMixer.blendAmount.get(); i++) {
                Color result = Color.white;
                try {
                    Field red = ColorMixer.class.getField("col"+i+"RedValue");
                    Field green = ColorMixer.class.getField("col"+i+"GreenValue");
                    Field blue = ColorMixer.class.getField("col"+i+"BlueValue");

                    int r = ((ColorElement)red.get(colMixer)).get();
                    int g = ((ColorElement)green.get(colMixer)).get();
                    int b = ((ColorElement)blue.get(colMixer)).get();

                    result = new Color(Math.max(0, Math.min(r, 255)), Math.max(0, Math.min(g, 255)), Math.max(0, Math.min(b, 255)));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                generator[i - 1] = result;
            }

            int h = colMixer.blendAmount.get();
            for (int z = colMixer.blendAmount.get() - 2; z >= 0; z--) {
                generator[h] = generator[z];
                h++;
            }

            lastColors = generator;
        }

        // cache thingy
        if (forceValue || lastFraction.length <= 0 || lastFraction.length != (colMixer.blendAmount.get() * 2) - 1) {
            // color frac regenerate if necessary
            float[] colorFraction = new float[(colMixer.blendAmount.get() * 2) - 1];

            for (int i = 0; i <= (colMixer.blendAmount.get() * 2) - 2; i++)
            {
                colorFraction[i] = (float)i / (float)((colMixer.blendAmount.get() * 2) - 2);
            }

            lastFraction = colorFraction;
        }
    }

}