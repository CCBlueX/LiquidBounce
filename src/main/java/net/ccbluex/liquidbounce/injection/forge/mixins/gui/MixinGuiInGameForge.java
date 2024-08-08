/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GuiIngameForge.class, remap = false)
public abstract class MixinGuiInGameForge extends Gui {

    @Shadow
    private FontRenderer fontrenderer;

    @Shadow
    protected abstract boolean pre(RenderGameOverlayEvent.ElementType type);

    @Shadow
    protected abstract void post(RenderGameOverlayEvent.ElementType type);

    @Shadow
    protected abstract void renderExperience(int width, int height);

    @Shadow
    protected abstract void renderJumpBar(int width, int height);

    @Shadow
    protected abstract void renderArmor(int width, int height);

    @Redirect(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;renderHealth(II)V"))
    private void hookRenderHealth(GuiIngameForge instance, int x, int y) {
        if (HUD.INSTANCE.handleEvents() && HUD.INSTANCE.getBlackHotbar()) {
            instance.renderHealth(x + 6 - fontrenderer.getStringWidth("" + Minecraft.getMinecraft().thePlayer.experienceLevel), y - 8);
        } else instance.renderHealth(x, y);
    }

    @Redirect(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;renderFood(II)V"))
    private void hookRenderFood(GuiIngameForge instance, int x, int y) {
        if (HUD.INSTANCE.handleEvents() && HUD.INSTANCE.getBlackHotbar()) {
            instance.renderFood(x - 6 + fontrenderer.getStringWidth("" + Minecraft.getMinecraft().thePlayer.experienceLevel), y - 8);
        } else instance.renderFood(x, y);
    }

    @Redirect(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;renderExperience(II)V"))
    private void hookRenderEXP(GuiIngameForge instance, int x, int y) {
        if (HUD.INSTANCE.handleEvents() && HUD.INSTANCE.getBlackHotbar()) {
            renderExperience(x, y - 6);
        } else renderExperience(x, y);
    }

    @Redirect(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;renderJumpBar(II)V"))
    private void hookRenderHorseEXP(GuiIngameForge instance, int x, int y) {
        if (HUD.INSTANCE.handleEvents() && HUD.INSTANCE.getBlackHotbar()) {
            renderJumpBar(x, y - 6);
        } else renderJumpBar(x, y);
    }

    @Redirect(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;renderArmor(II)V"))
    private void hookRenderArmor(GuiIngameForge instance, int x, int y) {
        if (HUD.INSTANCE.handleEvents() && HUD.INSTANCE.getBlackHotbar()) {
            renderArmor(x, y - 12);
        } else renderJumpBar(x, y);
    }

    @Redirect(method = "renderExperience", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"))
    private int hookExperienceLevelYText(FontRenderer instance, String text, int x, int y, int color) {
        return HUD.INSTANCE.handleEvents() && HUD.INSTANCE.getBlackHotbar() ? instance.drawString(text, x, y - 6, color) : instance.drawString(text, x, y, color);
    }

}
