package net.ccbluex.liquidbounce.ui.client.hud.element.elements.notifications;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notifications;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@SideOnly(Side.CLIENT)
public class Notification {

    private final String message;
    public float x;
    public int textLength;
    private float stay;
    public FadeState fadeState = FadeState.IN;

    public Notification(String message) {
        this.message = message;
    }

    public void draw(final Notifications notifications) {
        final int[] location = notifications.getLocationFromFacing();

        this.textLength = Fonts.font35.getStringWidth(message);
        Gui.drawRect(location[0] - (int) x, location[1], location[0] - (int) x + 8 + textLength, location[1] - 20, Color.BLACK.getRGB());
        Gui.drawRect(location[0] - (int) x - 5, location[1], location[0] - (int) x, location[1] - 20, new Color(0, 160, 255).getRGB());
        Fonts.font35.drawString(message, location[0] - x + 4, location[1] - 14, Integer.MAX_VALUE);
        GlStateManager.resetColor();

        final int delta = RenderUtils.deltaTime;

        switch(fadeState) {
            case IN:
                if(x < (textLength + 8))
                    x += 0.2F * delta;
                else
                    fadeState = FadeState.STAY;
                stay = 60;

                if(x > (textLength + 8)) x = textLength + 8;
                break;
            case STAY:
                if(stay > 0)
                    stay -= 0.2F * delta;
                else
                    fadeState = FadeState.OUT;
                break;
            case OUT:
                if(x > 0)
                    x -= 0.2F * delta;
                else
                    fadeState = FadeState.END;
                break;
            case END:
                LiquidBounce.CLIENT.hud.removeNotification(this);
                break;
        }
    }
}
