package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.world.ChestStealer;
import net.minecraft.client.gui.inventory.GuiChest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChest.class)
public abstract class MixinGuiChest extends MixinGuiContainer
{
	@Inject(method = "drawGuiContainerForegroundLayer", at = @At("RETURN"))
	protected void drawGuiContainerForegroundLayer(final int mouseX, final int mouseY, final CallbackInfo ci)
	{
		final String advancedInformations = ((ChestStealer) LiquidBounce.moduleManager.get(ChestStealer.class)).getAdvancedInformations();

		fontRendererObj.drawString(advancedInformations, -(fontRendererObj.getStringWidth(advancedInformations) >> 1) + (xSize >> 1), -12.0f, 0xFFFFFF, true);
	}
}
