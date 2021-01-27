package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ServerListEntryLanScan;
import net.minecraft.client.resources.I18n;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerListEntryLanScan.class)
public class MixinServerListEntryLanScan
{
	@Shadow
	@Final
	private Minecraft mc;

	/**
	 * @author eric0210
	 * @reason Advertisement
	 */
	@Overwrite
	public void drawEntry(final int slotIndex, final int x, final int y, final int listWidth, final int slotHeight, final int mouseX, final int mouseY, final boolean isSelected)
	{
		final int ypos = y + slotHeight / 2 - mc.fontRendererObj.FONT_HEIGHT / 2;
		mc.fontRendererObj.drawString(I18n.format("lanServer.scanning"), mc.currentScreen.width / 2 - mc.fontRendererObj.getStringWidth(I18n.format("lanServer.scanning")) / 2, ypos, 16777215);

		final String text;
		switch ((int) (Minecraft.getSystemTime() / 300L % 3L))
		{
			case 0:
			default:
				text = "LiquidBounce";
				break;
			case 1:
				text = "the";
				break;
			case 2:
				text = "best";
		}

		mc.fontRendererObj.drawString(text, mc.currentScreen.width / 2 - mc.fontRendererObj.getStringWidth(text) / 2, ypos + mc.fontRendererObj.FONT_HEIGHT, 8421504);
	}
}
