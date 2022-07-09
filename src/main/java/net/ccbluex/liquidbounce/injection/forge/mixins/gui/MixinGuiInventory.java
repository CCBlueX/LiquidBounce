package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoArmor;
import net.ccbluex.liquidbounce.features.module.modules.player.InventoryCleaner;
import net.minecraft.client.gui.inventory.GuiInventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiInventory.class)
public abstract class MixinGuiInventory extends MixinInventoryEffectRenderer
{
	@Inject(method = "drawGuiContainerForegroundLayer", at = @At("RETURN"))
	protected void drawGuiContainerForegroundLayer(final int mouseX, final int mouseY, final CallbackInfo ci)
	{
		final AutoArmor autoArmor = (AutoArmor) LiquidBounce.moduleManager.get(AutoArmor.class);
		final InventoryCleaner invCleaner = (InventoryCleaner) LiquidBounce.moduleManager.get(InventoryCleaner.class);

		final String autoArmorInfo = autoArmor.getDebug();
		final String invCleanerInfo = invCleaner.getDebug();

		fontRendererObj.drawString(autoArmorInfo, -(fontRendererObj.getStringWidth(autoArmorInfo) >> 1) + (xSize >> 1), -24.0F - fontRendererObj.FONT_HEIGHT, 0xFFFFFF, true);
		fontRendererObj.drawString(invCleanerInfo, -(fontRendererObj.getStringWidth(invCleanerInfo) >> 1) + (xSize >> 1), -24.0F, 0xFFFFFF, true);
	}
}
