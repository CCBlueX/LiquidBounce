package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryEffectRenderer.class)
public abstract class MixinInventoryEffectRenderer extends MixinGuiContainer
{
    @Shadow
    private boolean hasActivePotionEffects;

    /**
     * @reason Vanilla Enhancements
     * @author OrangeMarshall
     */
    @Inject(method = "updateActivePotionEffects", at = @At("RETURN"))
    protected void updateActivePotionEffects(final CallbackInfo ci)
    {
        guiLeft = width - xSize >> 1;
    }
}
