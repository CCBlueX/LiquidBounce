package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
public class MixinRenderItem
{
    @Inject(method = "renderItemOverlayIntoGUI", at = @At("RETURN"))
    public void injectRenderItemEnchantments(final FontRenderer fr, final ItemStack stack, final int xPosition, final int yPosition, final String text, final CallbackInfo ci)
    {
        if (stack != null)
            RenderUtils.renderItemEnchantments(Fonts.INSTANCE.getMinecraftFont(), stack, xPosition, yPosition);
    }
}
