package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(InventoryEffectRenderer.class)
public abstract class MixinInventoryEffectRenderer extends MixinGuiContainer
{
	@Shadow
	private boolean hasActivePotionEffects;

	/**
	 * @reason Vanilla Enhancements
	 * @author OrangeMarshall
	 */
	@Overwrite
	protected void updateActivePotionEffects()
	{
		boolean hasVisibleEffect = false;

		for (final PotionEffect potioneffect : mc.thePlayer.getActivePotionEffects())
		{
			final Potion potion = Potion.potionTypes[potioneffect.getPotionID()];

			if (potion.shouldRender(potioneffect))
			{
				hasVisibleEffect = true;
				break;
			}
		}

		guiLeft = width - xSize >> 1;

		hasActivePotionEffects = !mc.thePlayer.getActivePotionEffects().isEmpty() && hasVisibleEffect;
	}
}
