package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoArmor;
import net.ccbluex.liquidbounce.features.module.modules.player.InventoryCleaner;
import net.minecraft.client.gui.inventory.GuiInventory;

import org.lwjgl.opengl.GL11;
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

		final String autoArmorInfo = autoArmor.getAdvancedInformations();
		final String invCleanerInfo = invCleaner.getAdvancedInformations();

		GL11.glScalef(0.5F, 0.5F, 0.5F);
		fontRendererObj.drawString(autoArmorInfo, -40.0f, -18.0f, 0xFFFFFF, true);
		fontRendererObj.drawString(invCleanerInfo, -40.0f, -12.0f, 0xFFFFFF, true);
		GL11.glScalef(2.0F, 2.0F, 2.0F);
	}
}
