/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.Render2DEvent;
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSpectator;
import net.minecraft.client.gui.ScaledResolution;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiSpectator.class)
public class MixinGuiSpectator
{
	@Shadow
	@Final
	private Minecraft field_175268_g;

	@Inject(method = "renderTooltip", at = @At("RETURN"))
	private void renderTooltipPost(final ScaledResolution scaledResolution, final float partialTicks, final CallbackInfo callbackInfo)
	{
		field_175268_g.mcProfiler.startSection("LiquidBounce-Render2DEvent");
		LiquidBounce.eventManager.callEvent(new Render2DEvent(partialTicks));

		field_175268_g.mcProfiler.endStartSection("LiquidBounce-FontRendererGC");
		AWTFontRenderer.Companion.garbageCollectionTick();

		field_175268_g.mcProfiler.endSection();
	}
}
