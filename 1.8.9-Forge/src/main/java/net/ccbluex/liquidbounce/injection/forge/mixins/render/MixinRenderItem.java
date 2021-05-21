package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import java.util.stream.Collectors;

import net.ccbluex.liquidbounce.utils.Maps;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import kotlin.Pair;

@Mixin(RenderItem.class)
public class MixinRenderItem
{
	@Inject(method = "renderItemOverlayIntoGUI", at = @At("RETURN"))
	public void renderItemOverlayIntoGUI(final FontRenderer fr, final ItemStack stack, final int xPosition, final int yPosition, final String text, final CallbackInfo ci)
	{
		if (stack != null)
		{
			final String s = EnchantmentHelper.getEnchantments(stack).entrySet().stream().map(entry ->
			{
				final Pair<String, String> pair = Maps.ENCHANTMENT_SHORT_NAME.get(entry.getKey());

				if (pair == null)
					return "";

				final String name = pair.getSecond();

				if (name == null)
					return "";

				return name + entry.getValue();
			}).collect(Collectors.joining(" "));

			GlStateManager.disableLighting();
			GlStateManager.disableDepth();
			GlStateManager.disableBlend();

			GL11.glScalef(0.5F, 0.5F, 0.5F);
			fr.drawString(s, xPosition << 1, yPosition << 1, 16777215, false);
			GL11.glScalef(2.0F, 2.0F, 2.0F);

			GlStateManager.enableLighting();
			GlStateManager.enableDepth();
		}
	}
}
