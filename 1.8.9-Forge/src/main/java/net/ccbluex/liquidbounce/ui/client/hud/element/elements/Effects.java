package net.ccbluex.liquidbounce.ui.client.hud.element.elements;

import net.ccbluex.liquidbounce.ui.client.hud.GuiHudDesigner;
import net.ccbluex.liquidbounce.ui.client.hud.element.Element;
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ElementInfo(name = "Effects")
public class Effects extends Element {

    private FontRenderer fontRenderer = Fonts.font35;
    private final BoolValue shadow = new BoolValue("Shadow", true);

    private int x2;
    private int y2;

    @Override
    public void drawElement() {
        final int[] location = getLocationFromFacing();

        int y = location[1];
        int width = 0;
        for (final PotionEffect effect : mc.thePlayer.getActivePotionEffects()) {
            final Potion potion = Potion.potionTypes[effect.getPotionID()];
            String name = I18n.format(potion.getName());

            if(effect.getAmplifier() == 1)
                name = name + " II";
            else if(effect.getAmplifier() == 2)
                name = name + " III";
            else if(effect.getAmplifier() == 3)
                name = name + " IV";
            else if(effect.getAmplifier() == 4)
                name = name + " V";
            else if(effect.getAmplifier() == 5)
                name = name + " VI";
            else if(effect.getAmplifier() == 6)
                name = name + " VII";
            else if(effect.getAmplifier() == 7)
                name = name + " VIII";
            else if(effect.getAmplifier() == 8)
                name = name + " IX";
            else if(effect.getAmplifier() == 9)
                name = name + " X";
            else if(effect.getAmplifier() > 10)
                name = name + " X+";
            else
                name = name + " I";

            name = name + "§f: §7" + Potion.getDurationString(effect);

            if(width < fontRenderer.getStringWidth(name))
                width = fontRenderer.getStringWidth(name);

            fontRenderer.drawString(name, location[0] - fontRenderer.getStringWidth(name), y, potion.getLiquidColor(), shadow.get());
            y -= fontRenderer.FONT_HEIGHT;
        }

        if(width == 0)
            width = 40;

        if(location[1] == y)
            y = location[1] - 10;

        this.y2 = y + fontRenderer.FONT_HEIGHT - 2;
        this.x2 = location[0] - width;

        if (mc.currentScreen instanceof GuiHudDesigner)
            RenderUtils.drawBorderedRect(location[0] + 2, location[1] + fontRenderer.FONT_HEIGHT, x2 - 2, y2, 3, Integer.MIN_VALUE, 0);
    }

    @Override
    public void destroyElement() {

    }

    @Override
    public void updateElement() {

    }

    @Override
    public void handleMouseClick(int mouseX, int mouseY, int mouseButton) {

    }

    @Override
    public void handleKey(char c, int keyCode) {

    }

    @Override
    public boolean isMouseOverElement(int mouseX, int mouseY) {
        final int[] location = getLocationFromFacing();

        return mouseX <= location[0] + 2 && mouseY <= location[1] + fontRenderer.FONT_HEIGHT && mouseX >= x2 - 2 && mouseY >= y2;
    }

    public FontRenderer getFontRenderer() {
        return fontRenderer;
    }

    public Effects setFontRenderer(FontRenderer fontRenderer) {
        this.fontRenderer = fontRenderer;
        return this;
    }

    public boolean isShadow() {
        return shadow.get();
    }

    public Effects setShadow(final boolean b) {
        shadow.set(b);
        return this;
    }
}