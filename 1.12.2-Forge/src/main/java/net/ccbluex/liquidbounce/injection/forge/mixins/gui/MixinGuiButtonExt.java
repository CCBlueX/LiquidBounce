/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.injection.backend.FontRendererImpl;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GuiButtonExt.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiButtonExt extends GuiButton
{
	private float cut;
	private float alpha;

	public MixinGuiButtonExt(final int p_i1020_1_, final int p_i1020_2_, final int p_i1020_3_, final String p_i1020_4_)
	{
		super(p_i1020_1_, p_i1020_2_, p_i1020_3_, p_i1020_4_);
	}

	public MixinGuiButtonExt(final int p_i46323_1_, final int p_i46323_2_, final int p_i46323_3_, final int p_i46323_4_, final int p_i46323_5_, final String p_i46323_6_)
	{
		super(p_i46323_1_, p_i46323_2_, p_i46323_3_, p_i46323_4_, p_i46323_5_, p_i46323_6_);
	}

	/**
	 * @author CCBlueX
	 */
	@Override
	@Overwrite
	public void drawButton(final Minecraft mc, final int mouseX, final int mouseY, final float partialTicks)
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

			fontRenderer.drawStringWithShadow(displayString, (float) (x + (width >> 1) - (fontRenderer.getStringWidth(displayString) >> 1)), y + (height - 5) * 0.5f, 14737632);
			GlStateManager.resetColor();
		}
	}
}
