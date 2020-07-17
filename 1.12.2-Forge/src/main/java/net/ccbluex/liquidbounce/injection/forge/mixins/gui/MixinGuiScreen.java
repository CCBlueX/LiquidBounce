/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.misc.ComponentOnHover;
import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.ccbluex.liquidbounce.injection.backend.ResourceLocationImplKt;
import net.ccbluex.liquidbounce.ui.client.GuiBackground;
import net.ccbluex.liquidbounce.utils.render.ParticleUtils;
import net.ccbluex.liquidbounce.utils.render.shader.shaders.BackgroundShader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Mixin(GuiScreen.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiScreen {
    @Shadow
    public Minecraft mc;

    @Shadow
    public List<GuiButton> buttonList;

    @Shadow
    public int width;

    @Shadow
    public int height;
    @Shadow
    public FontRenderer fontRenderer;

    @Shadow
    public void updateScreen() {
    }

    @Shadow
    protected abstract void handleComponentHover(ITextComponent component, int x, int y);

    @Shadow
    public abstract void drawHoveringText(List<String> textLines, int x, int y);

    @Shadow
    public abstract void drawDefaultBackground();

    @Inject(method = "drawWorldBackground", at = @At("HEAD"))
    private void drawWorldBackground(final CallbackInfo callbackInfo) {
        final HUD hud = (HUD) LiquidBounce.moduleManager.getModule(HUD.class);

        if (hud.getInventoryParticle().get() && mc.player != null) {
            final ScaledResolution scaledResolution = new ScaledResolution(mc);
            final int width = scaledResolution.getScaledWidth();
            final int height = scaledResolution.getScaledHeight();
            ParticleUtils.drawParticles(Mouse.getX() * width / mc.displayWidth, height - Mouse.getY() * height / mc.displayHeight - 1);
        }
    }

    /**
     * @author CCBlueX
     */
    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
    private void drawClientBackground(final CallbackInfo callbackInfo) {
        GlStateManager.disableLighting();
        GlStateManager.disableFog();

        if (GuiBackground.Companion.getEnabled()) {
            if (LiquidBounce.INSTANCE.getBackground() == null) {
                BackgroundShader.BACKGROUND_SHADER.startShader();

                final Tessellator instance = Tessellator.getInstance();
                final BufferBuilder worldRenderer = instance.getBuffer();
                worldRenderer.begin(7, DefaultVertexFormats.POSITION);
                worldRenderer.pos(0, height, 0.0D).endVertex();
                worldRenderer.pos(width, height, 0.0D).endVertex();
                worldRenderer.pos(width, 0, 0.0D).endVertex();
                worldRenderer.pos(0, 0, 0.0D).endVertex();
                instance.draw();

                BackgroundShader.BACKGROUND_SHADER.stopShader();
            } else {
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
    private void drawParticles(final CallbackInfo callbackInfo) {
        if (GuiBackground.Companion.getParticles())
            ParticleUtils.drawParticles(Mouse.getX() * width / mc.displayWidth, height - Mouse.getY() * height / mc.displayHeight - 1);
    }

    @Inject(method = "sendChatMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void messageSend(String msg, boolean addToChat, final CallbackInfo callbackInfo) {
        if (msg.startsWith(String.valueOf(LiquidBounce.commandManager.getPrefix())) && addToChat) {
            this.mc.ingameGUI.getChatGUI().addToSentMessages(msg);

            LiquidBounce.commandManager.executeCommands(msg);
            callbackInfo.cancel();
        }
    }

    @Inject(method = "handleComponentHover", at = @At("HEAD"))
    private void handleHoverOverComponent(ITextComponent component, int x, int y, final CallbackInfo callbackInfo) {
        if (component == null || component.getStyle().getClickEvent() == null || !LiquidBounce.moduleManager.getModule(ComponentOnHover.class).getState())
            return;

        final Style chatStyle = component.getStyle();

        final ClickEvent clickEvent = chatStyle.getClickEvent();
        final HoverEvent hoverEvent = chatStyle.getHoverEvent();

        drawHoveringText(Collections.singletonList("§c§l" + clickEvent.getAction().getCanonicalName().toUpperCase() + ": §a" + clickEvent.getValue()), x, y - (hoverEvent != null ? 17 : 0));
    }

    /**
     * @author CCBlueX (superblaubeere27)
     * @reason Making it possible for other mixins to receive actions
     */
    @Overwrite
    protected void actionPerformed(GuiButton button) throws IOException {
        this.injectedActionPerformed(button);
    }

    protected void injectedActionPerformed(GuiButton button) {

    }
}