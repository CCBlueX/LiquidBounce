package net.ccbluex.liquidbounce.utils;

import net.minecraft.client.gui.GuiTextField;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
public final class TabUtils {

    public static void tab(final GuiTextField... textFields) {
        for(int i = 0; i < textFields.length; i++) {
            final GuiTextField textField = textFields[i];

            if(textField.isFocused()) {
                textField.setFocused(false);
                i++;

                if(i >= textFields.length)
                    i = 0;

                textFields[i].setFocused(true);
                break;
            }
        }
    }
}
