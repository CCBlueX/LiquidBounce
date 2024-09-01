/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.fabric.mixins.gui;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.command.CommandManager;
import net.ccbluex.liquidbounce.features.module.modules.misc.ComponentOnHover;
import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.ccbluex.liquidbounce.ui.client.GuiClientConfiguration;
import net.ccbluex.liquidbounce.utils.Background;
import net.ccbluex.liquidbounce.utils.render.ParticleUtils;
import net.ccbluex.liquidbounce.utils.render.shader.shaders.BackgroundShader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
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

import java.util.Collections;
import java.util.List;

import static com.mojang.blaze3d.platform.GlStateManager.disableFog;
import static com.mojang.blaze3d.platform.GlStateManager.disableLighting;

@Mixin(Screen.class)
@SideOnly(Side.CLIENT)
public abstract class MixinScreen {
    @Shadow
    public Minecraft mc;

    @Shadow
    protected List<ButtonWidget> buttonList;

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    protected TextRenderer fontRendererObj;

    @Shadow
    public void updateScreen() {
    }

    @Shadow
    public abstract void handleComponentHover(IChatComponent component, int x, int y);

    @Shadow
    protected abstract void drawHoveringText(List<String> textLines, int x, int y);

    @Inject(method = "drawWorldBackground", at = @At("HEAD"))
    private void drawWorldBackground(final CallbackInfo callbackInfo) {
        final HUD hud = HUD.INSTANCE;

        if (hud.getInventoryParticle() && mc.player != null) {
            final Window scaledResolution = new Window(mc);
            final int width = scaledResolution.getScaledWidth();
            final int height = scaledResolution.getScaledHeight();
            ParticleUtils.INSTANCE.drawParticles(Mouse.getX() * width / mc.displayWidth, height - Mouse.getY() * height / mc.displayHeight - 1);
        }
    }

    /**
     * @author CCBlueX
     */
    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
    private void drawClientBackground(final CallbackInfo callbackInfo) {
        disableLighting();
        disableFog();

        if (GuiClientConfiguration.Companion.getEnabledCustomBackground()) {
            final Background background = LiquidBounce.INSTANCE.getBackground();

            if (background == null) {
                // Use default background shader

                BackgroundShader.Companion.getBACKGROUND_SHADER().startShader();

                final Tessellator instance = Tessellator.getInstance();
                final WorldRenderer worldRenderer = instance.getWorldRenderer();
                worldRenderer.begin(7, VertexFormats.POSITION);
                worldRenderer.pos(0, height, 0).endVertex();
                worldRenderer.pos(width, height, 0).endVertex();
                worldRenderer.pos(width, 0, 0).endVertex();
                worldRenderer.pos(0, 0, 0).endVertex();
                instance.draw();

                BackgroundShader.Companion.getBACKGROUND_SHADER().stopShader();
            } else {
                // Use custom background
                background.drawBackground(width, height);
            }

            if (GuiClientConfiguration.Companion.getParticles()) {
                ParticleUtils.INSTANCE.drawParticles(Mouse.getX() * width / mc.displayWidth, height - Mouse.getY() * height / mc.displayHeight - 1);
            }

            callbackInfo.cancel();
        }
    }

    @Inject(method = "drawBackground", at = @At("RETURN"))
    private void drawParticles(final CallbackInfo callbackInfo) {
        if (GuiClientConfiguration.Companion.getParticles())
            ParticleUtils.INSTANCE.drawParticles(Mouse.getX() * width / mc.displayWidth, height - Mouse.getY() * height / mc.displayHeight - 1);
    }

    @Inject(method = "sendChatMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void messageSend(String msg, boolean addToChat, final CallbackInfo callbackInfo) {
        if (msg.startsWith(String.valueOf(CommandManager.INSTANCE.getPrefix())) && addToChat) {
            mc.ingameGUI.getChatGUI().addToSentMessages(msg);

            CommandManager.INSTANCE.executeCommands(msg);
            callbackInfo.cancel();
        }
    }

    @Inject(method = "handleComponentHover", at = @At("HEAD"))
    private void handleHoverOverComponent(IChatComponent component, int x, int y, final CallbackInfo callbackInfo) {
        if (component == null || component.getChatStyle().getChatClickEvent() == null || !ComponentOnHover.INSTANCE.handleEvents())
            return;

        final ChatStyle chatStyle = component.getChatStyle();

        final ClickEvent clickEvent = chatStyle.getChatClickEvent();
        final HoverEvent hoverEvent = chatStyle.getChatHoverEvent();

        drawHoveringText(Collections.singletonList("§c§l" + clickEvent.getAction().getCanonicalName().toUpperCase() + ": §a" + clickEvent.getValue()), x, y - (hoverEvent != null ? 17 : 0));
    }

    /**
     * @author CCBlueX (superblaubeere27)
     * @reason Making it possible for other mixins to receive actions
     */
    @Overwrite
    protected void actionPerformed(ButtonWidget button) {
        injectedActionPerformed(button);
    }

    protected void injectedActionPerformed(ButtonWidget button) {

    }
}