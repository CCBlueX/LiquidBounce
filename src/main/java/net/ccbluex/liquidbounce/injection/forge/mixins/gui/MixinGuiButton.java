/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import java.awt.*;

import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiButton.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiButton extends Gui
{

    @Shadow
    public boolean visible;

    @Shadow
    public int xPosition;

    @Shadow
    public int yPosition;

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    protected boolean hovered;

    @Shadow
    public boolean enabled;

    @Shadow
    protected abstract void mouseDragged(Minecraft mc, int mouseX, int mouseY);

    @Shadow
    public String displayString;

    @Shadow
    @Final
    protected static ResourceLocation buttonTextures;
    private float cut;
    private float alpha;

    /**
     * @author CCBlueX
     * @reason
     */
    @Overwrite
    public void drawButton(final Minecraft mc, final int mouseX, final int mouseY)
    {
        if (visible)
        {
            final FontRenderer fontRenderer = mc.getLanguageManager().isCurrentLocaleUnicode() ? mc.fontRendererObj : Fonts.font35; // TODO: Changeable font
            hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;

            final int delta = RenderUtils.getFrameTime();

            if (enabled && hovered)
            {
                cut += 0.05F * delta;

                if (cut >= 4)
                    cut = 4;

                alpha += 0.3F * delta;

                if (alpha >= 210)
                    alpha = 210;
            }
            else
            {
                cut -= 0.05F * delta;

                if (cut <= 0)
                    cut = 0;

                alpha -= 0.3F * delta;

                if (alpha <= 120)
                    alpha = 120;
            }

            Gui.drawRect(xPosition + (int) cut, yPosition, xPosition + width - (int) cut, yPosition + height, (enabled ? new Color(0.0F, 0.0F, 0.0F, alpha / 255.0F) : new Color(0.5F, 0.5F, 0.5F, 0.5F)).getRGB());

            mc.getTextureManager().bindTexture(buttonTextures);
            mouseDragged(mc, mouseX, mouseY);

            AWTFontRenderer.Companion.setAssumeNonVolatile(true);

            fontRenderer.drawStringWithShadow(displayString, xPosition + (width >> 1) - (fontRenderer.getStringWidth(displayString) >> 1), yPosition + (height - 5 >> 1), 14737632);

            AWTFontRenderer.Companion.setAssumeNonVolatile(false);

            GlStateManager.resetColor();
        }
    }
}
