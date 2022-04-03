/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils;

import net.minecraft.client.gui.GuiTextField;

public final class TabUtils {

    public static void tab(final GuiTextField... textFields) {
        for (int i = 0; i < textFields.length; i++) {
            final GuiTextField textField = textFields[i];

            if (textField.isFocused()) {
                textField.setFocused(false);
                i++;

                if (i >= textFields.length)
                    i = 0;

                textFields[i].setFocused(true);
                break;
            }
        }
    }
}
