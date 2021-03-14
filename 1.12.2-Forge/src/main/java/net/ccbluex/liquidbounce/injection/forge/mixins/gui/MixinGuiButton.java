/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.injection.backend.FontRendererImpl;
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
	@Final
	protected static ResourceLocation BUTTON_TEXTURES;
	@Shadow
	public boolean visible;
	@Shadow
	public int x;
	@Shadow
	public int y;
	@Shadow
	public int width;
	@Shadow
	public int height;
	@Shadow
	public boolean enabled;
	@Shadow
	public String displayString;
	@Shadow
	protected boolean hovered;
	private float cut;
	private float alpha;

	@Shadow
	protected abstract void mouseDragged(Minecraft mc, int mouseX, int mouseY);

	/**
	 * @author CCBlueX
	 */
	@Overwrite
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks)
	{
		if (visible)
		{
			final FontRenderer fontRenderer = mc.getLanguageManager().isCurrentLocaleUnicode() ? mc.fontRenderer : ((FontRendererImpl) Fonts.font35).getWrapped();
			hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;

			final int delta = RenderUtils.deltaTime;

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

			Gui.drawRect(x + (int) cut, y, x + width - (int) cut, y + height, enabled ? new Color(0F, 0F, 0F, alpha / 255F).getRGB() : new Color(0.5F, 0.5F, 0.5F, 0.5F).getRGB());

			mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
			mouseDragged(mc, mouseX, mouseY);

			AWTFontRenderer.Companion.setAssumeNonVolatile(true);

			fontRenderer.drawStringWithShadow(displayString, (float) (x + (width >> 1) - fontRenderer.getStringWidth(displayString) >> 1)), y + (height - 5) * 0.5f, 14737632)

			AWTFontRenderer.Companion.setAssumeNonVolatile(false);

			GlStateManager.resetColor();
		}
	}
}
