/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import java.util.stream.IntStream;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.Render2DEvent;
import net.ccbluex.liquidbounce.features.module.modules.render.AntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.ccbluex.liquidbounce.features.module.modules.render.NoScoreboard;
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer;
import net.ccbluex.liquidbounce.utils.ClassUtils;
import net.ccbluex.liquidbounce.utils.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiIngame
{
	@Shadow
	@Final
	protected Minecraft mc;

	@Shadow
	protected abstract void renderHotbarItem(int index, int xPos, int yPos, float partialTicks, EntityPlayer player);

	@Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
	private void renderScoreboard(final CallbackInfo callbackInfo)
	{
		if (LiquidBounce.moduleManager.get(HUD.class).getState() || NoScoreboard.INSTANCE.getState())
			callbackInfo.cancel();
	}

	@Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
	private void renderTooltip(final ScaledResolution sr, final float partialTicks, final CallbackInfo callbackInfo)
	{
		final HUD hud = (HUD) LiquidBounce.moduleManager.get(HUD.class);

		if (Minecraft.getMinecraft().getRenderViewEntity() instanceof EntityPlayer && hud.getState() && hud.getBlackHotbarValue().get())
		{
			final EntityPlayer entityPlayer = (EntityPlayer) Minecraft.getMinecraft().getRenderViewEntity();

			final int middleScreen = sr.getScaledWidth() >> 1;

			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			Gui.drawRect(middleScreen - 91, sr.getScaledHeight() - 24, middleScreen + 90, sr.getScaledHeight(), Integer.MIN_VALUE);

			final int currentHeldItem = entityPlayer.inventory.currentItem;

			Gui.drawRect(middleScreen - 91 - 1 + currentHeldItem * 20 + 1, sr.getScaledHeight() - 24, middleScreen - 91 - 1 + currentHeldItem * 20 + 22, sr.getScaledHeight() - 22 - 1 + 24, Integer.MAX_VALUE);
			if (InventoryUtils.targetSlot != null)
			{
				final int serverSlot = InventoryUtils.targetSlot;
				Gui.drawRect(middleScreen - 91 - 1 + serverSlot * 20 + 1, sr.getScaledHeight() - 24, middleScreen - 91 - 1 + serverSlot * 20 + 22, sr.getScaledHeight() - 22 - 1 + 24, InventoryUtils.targetSlot == currentHeldItem ? -65536 : -16776961);
			}

			GlStateManager.enableRescaleNormal();
			GL11.glEnable(GL11.GL_BLEND);
			GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
			RenderHelper.enableGUIStandardItemLighting();

			final int hotbarItemXPos = (sr.getScaledWidth() >> 1) - 90 + 2;
			final int hotbarItemYPos = sr.getScaledHeight() - 16 - 3;

			IntStream.range(0, 9).forEach(index ->
			{
				final int xPos = hotbarItemXPos + index * 20;
				renderHotbarItem(index, xPos, hotbarItemYPos, partialTicks, entityPlayer);
			});

			RenderHelper.disableStandardItemLighting();
			GlStateManager.disableRescaleNormal();
			GlStateManager.disableBlend();

			mc.mcProfiler.startSection("LiquidBounce-Render2DEvent");
			LiquidBounce.eventManager.callEvent(new Render2DEvent(partialTicks));

			mc.mcProfiler.endStartSection("LiquidBounce-FontRendererGC");
			AWTFontRenderer.Companion.garbageCollectionTick();

			mc.mcProfiler.endSection();

			callbackInfo.cancel();
		}
	}

	@Inject(method = "renderTooltip", at = @At("RETURN"))
	private void renderTooltipPost(final ScaledResolution sr, final float partialTicks, final CallbackInfo callbackInfo)
	{
		if (!ClassUtils.hasLabyMod())
		{
			mc.mcProfiler.startSection("LiquidBounce-Render2DEvent");
			LiquidBounce.eventManager.callEvent(new Render2DEvent(partialTicks));

			mc.mcProfiler.endStartSection("LiquidBounce-FontRendererGC");
			AWTFontRenderer.Companion.garbageCollectionTick();

			mc.mcProfiler.endSection();
		}
	}

	@Inject(method = "renderPumpkinOverlay", at = @At("HEAD"), cancellable = true)
	private void renderPumpkinOverlay(final CallbackInfo callbackInfo)
	{
		final AntiBlind antiBlind = (AntiBlind) LiquidBounce.moduleManager.get(AntiBlind.class);

		if (antiBlind.getState() && antiBlind.getPumpkinEffect().get())
			callbackInfo.cancel();
	}
}
