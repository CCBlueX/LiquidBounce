/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.Render2DEvent;
import net.ccbluex.liquidbounce.features.module.modules.render.AntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.ccbluex.liquidbounce.features.module.modules.render.NoScoreboard;
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer;
import net.ccbluex.liquidbounce.utils.ClassUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiInGame extends MixinGui
{

	@Shadow
	@Final
	protected static ResourceLocation WIDGETS_TEX_PATH;
	@Shadow
	@Final
	protected Minecraft mc;

	@Shadow
	protected abstract void renderHotbarItem(int xPos, int yPos, float partialTicks, EntityPlayer player, ItemStack stack);

	@Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
	private void renderScoreboard(final CallbackInfo callbackInfo)
	{
		if (LiquidBounce.moduleManager.get(HUD.class).getState() || NoScoreboard.INSTANCE.getState())
			callbackInfo.cancel();
	}

	@Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
	private void renderTooltip(final ScaledResolution sr, final float partialTicks, final CallbackInfo callbackInfo)
	{
		final HUD hud = (HUD) LiquidBounce.moduleManager.get(HUD.class);

		if (Minecraft.getMinecraft().getRenderViewEntity() instanceof EntityPlayer && hud.getState() && hud.getBlackHotbarValue().get())
		{
			final EntityPlayer entityPlayer = (EntityPlayer) mc.getRenderViewEntity();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			final ItemStack offHandItemStack = entityPlayer.getHeldItemOffhand();
			final EnumHandSide enumhandside = entityPlayer.getPrimaryHand().opposite();
			final int middleScreen = sr.getScaledWidth() >> 1;
			final float f = zLevel;
			final int j = 182;
			final int k = 91;
			zLevel = -90.0F;

			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			Gui.drawRect(middleScreen - 91, sr.getScaledHeight() - 24, middleScreen + 90, sr.getScaledHeight(), Integer.MIN_VALUE);
			Gui.drawRect(middleScreen - 91 - 1 + entityPlayer.inventory.currentItem * 20 + 1, sr.getScaledHeight() - 24, middleScreen - 91 - 1 + entityPlayer.inventory.currentItem * 20 + 22, sr.getScaledHeight() - 22 - 1 + 24, Integer.MAX_VALUE);

			mc.getTextureManager().bindTexture(WIDGETS_TEX_PATH);

			if (!offHandItemStack.isEmpty())
			{
				final int x;

				if (enumhandside == EnumHandSide.LEFT)
				{
					x = middleScreen - 91 - 29;
				}
				else
				{
					x = middleScreen + 91;
				}

				final int y = sr.getScaledHeight() - 23;
				Gui.drawRect(x, y, x + 29, y + 24, Integer.MIN_VALUE);
			}

			zLevel = f;
			GlStateManager.enableRescaleNormal();
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
			RenderHelper.enableGUIStandardItemLighting();

			for (int l = 0; l < 9; ++l)
			{
				final int i1 = middleScreen - 90 + l * 20 + 2;
				final int j1 = sr.getScaledHeight() - 16 - 3;
				renderHotbarItem(i1, j1, partialTicks, entityPlayer, entityPlayer.inventory.mainInventory.get(l));
			}

			if (!offHandItemStack.isEmpty())
			{
				final int l1 = sr.getScaledHeight() - 16 - 3;

				if (enumhandside == EnumHandSide.LEFT)
				{
					renderHotbarItem(middleScreen - 91 - 26, l1, partialTicks, entityPlayer, offHandItemStack);
				}
				else
				{
					renderHotbarItem(middleScreen + 91 + 10, l1, partialTicks, entityPlayer, offHandItemStack);
				}
			}

			if (mc.gameSettings.attackIndicator == 2)
			{
				final float f1 = mc.player.getCooledAttackStrength(0.0F);

				if (f1 < 1.0F)
				{
					final int i2 = sr.getScaledHeight() - 20;
					int j2 = middleScreen + 91 + 6;

					if (enumhandside == EnumHandSide.RIGHT)
					{
						j2 = middleScreen - 91 - 22;
					}

					mc.getTextureManager().bindTexture(Gui.ICONS);
					final int k1 = (int) (f1 * 19.0F);
					GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
					drawTexturedModalRect(j2, i2, 0, 94, 18, 18);
					drawTexturedModalRect(j2, i2 + 18 - k1, 18, 112 - k1, 18, k1);
				}
			}

			RenderHelper.disableStandardItemLighting();
			GlStateManager.disableRescaleNormal();
			GlStateManager.disableBlend();

			callRender2DEvent(partialTicks);
			callbackInfo.cancel();
		}
	}

	@Inject(method = "renderHotbar", at = @At("RETURN"))
	private void renderTooltipPost(final ScaledResolution sr, final float partialTicks, final CallbackInfo callbackInfo)
	{
		callRender2DEvent(partialTicks);
	}

	private void callRender2DEvent(final float partialTicks)
	{
		if (!ClassUtils.hasClass("net.labymod.api.LabyModAPI"))
		{
			LiquidBounce.eventManager.callEvent(new Render2DEvent(partialTicks));
			AWTFontRenderer.Companion.garbageCollectionTick();
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
