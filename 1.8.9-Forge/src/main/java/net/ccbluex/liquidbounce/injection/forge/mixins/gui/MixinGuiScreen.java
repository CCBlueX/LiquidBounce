/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.misc.ComponentOnHover;
import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.ccbluex.liquidbounce.injection.backend.minecraft.util.ResourceLocationImplKt;
import net.ccbluex.liquidbounce.ui.client.GuiBackground;
import net.ccbluex.liquidbounce.utils.render.ParticleUtils;
import net.ccbluex.liquidbounce.utils.render.shader.shaders.BackgroundShader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiScreen extends Gui
{
	@Shadow
	public Minecraft mc;

	@Shadow
	public List<GuiButton> buttonList;

	@Shadow
	public int width;

	@Shadow
	public int height;

	@Shadow
	public FontRenderer fontRendererObj;

	@SuppressWarnings("NoopMethodInAbstractClass")
	@Shadow
	public void updateScreen()
	{
	}

	@Shadow
	public abstract void drawDefaultBackground();

	@SuppressWarnings("NoopMethodInAbstractClass")
	@Shadow
	public void drawScreen(int mouseX, int mouseY, float partialTicks)
	{
	}

	@Shadow
	protected abstract void renderToolTip(ItemStack stack, int x, int y);

	@Shadow
	public abstract void handleComponentHover(IChatComponent component, int x, int y);

	@Shadow
	protected abstract void drawHoveringText(List<String> textLines, int x, int y);

	@Inject(method = "drawWorldBackground", at = @At("HEAD"))
	private void drawWorldBackground(final CallbackInfo callbackInfo)
	{
		final HUD hud = (HUD) LiquidBounce.moduleManager.get(HUD.class);

		if (hud.getInventoryParticle().get() && mc.thePlayer != null)
		{
			final ScaledResolution scaledResolution = new ScaledResolution(mc);

			ParticleUtils.drawParticles(Mouse.getX() * scaledResolution.getScaledWidth() / mc.displayWidth, height - Mouse.getY() * scaledResolution.getScaledHeight() / mc.displayHeight - 1);
		}
	}

	/**
	 * @author CCBlueX
	 */
	@Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
	private void drawClientBackground(final CallbackInfo callbackInfo)
	{
		GlStateManager.disableLighting();
		GlStateManager.disableFog();

		if (GuiBackground.Companion.getEnabled())
		{
			if (LiquidBounce.INSTANCE.getBackground() == null)
			{
				// Use Shader background
				BackgroundShader.INSTANCE.startShader();

				final Tessellator instance = Tessellator.getInstance();
				final WorldRenderer worldRenderer = instance.getWorldRenderer();
				worldRenderer.begin(7, DefaultVertexFormats.POSITION);
				worldRenderer.pos(0, height, 0.0D).endVertex();
				worldRenderer.pos(width, height, 0.0D).endVertex();
				worldRenderer.pos(width, 0, 0.0D).endVertex();
				worldRenderer.pos(0, 0, 0.0D).endVertex();
				instance.draw();

				BackgroundShader.INSTANCE.stopShader();
			}
			else
			{
				// Use custom background picture
				final ScaledResolution scaledResolution = new ScaledResolution(mc);
				final int width = scaledResolution.getScaledWidth();
				final int height = scaledResolution.getScaledHeight();

				mc.getTextureManager().bindTexture(ResourceLocationImplKt.unwrap(LiquidBounce.INSTANCE.getBackground()));
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				Gui.drawScaledCustomSizeModalRect(0, 0, 0.0F, 0.0F, width, height, width, height, width, height);
			}

			if (GuiBackground.Companion.getParticles())
				ParticleUtils.drawParticles(Mouse.getX() * width / mc.displayWidth, height - Mouse.getY() * height / mc.displayHeight - 1);

			callbackInfo.cancel();
		}
	}

	@Inject(method = "drawBackground", at = @At("RETURN"))
	private void drawParticles(final CallbackInfo callbackInfo)
	{
		if (GuiBackground.Companion.getParticles())
			ParticleUtils.drawParticles(Mouse.getX() * width / mc.displayWidth, height - Mouse.getY() * height / mc.displayHeight - 1);
	}

	@Inject(method = "sendChatMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
	private void messageSend(final String msg, final boolean addToChat, final CallbackInfo callbackInfo)
	{
		if (msg.startsWith(String.valueOf(LiquidBounce.commandManager.getPrefix())) && addToChat)
		{
			mc.ingameGUI.getChatGUI().addToSentMessages(msg);

			LiquidBounce.commandManager.executeCommands(msg);
			callbackInfo.cancel();
		}
	}

	@Inject(method = "handleComponentHover", at = @At("HEAD"))
	private void handleHoverOverComponent(final IChatComponent component, final int x, final int y, final CallbackInfo callbackInfo)
	{
		if (component == null || component.getChatStyle().getChatClickEvent() == null || !LiquidBounce.moduleManager.get(ComponentOnHover.class).getState())
			return;

		final ChatStyle chatStyle = component.getChatStyle();

		final ClickEvent clickEvent = chatStyle.getChatClickEvent();
		final HoverEvent hoverEvent = chatStyle.getChatHoverEvent();

		drawHoveringText(Collections.singletonList("\u00A7c\u00A7l" + clickEvent.getAction().getCanonicalName().toUpperCase(Locale.ENGLISH) + ": \u00A7a" + clickEvent.getValue()), x, y - (hoverEvent != null ? 17 : 0));
	}

	/**
	 * @author CCBlueX (superblaubeere27)
	 * @reason Making it possible for other mixins to receive actions
	 */
	@Overwrite
	protected void actionPerformed(final GuiButton button)
	{
		injectedActionPerformed(button);
	}

	@SuppressWarnings("NoopMethodInAbstractClass")
	protected void injectedActionPerformed(final GuiButton button)
	{

	}
}
