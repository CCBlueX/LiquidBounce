package net.ccbluex.liquidbounce.injection.fabric.mixins.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = GuiIngameForge.class, remap = false)
public abstract class MixinGuiInGameForge extends GuiIngame {
    public MixinGuiInGameForge(Minecraft p_i46325_1_) {
        super(p_i46325_1_);
    }
/*
    @Shadow
    private TextRenderer fontrenderer;

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


    @Redirect(method = "renderGameOverlay(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;renderExperience(II)V", remap = false))
    private void hookRenderEXP(GuiIngameForge instance, int x, int y) {
        if (HUD.INSTANCE.handleEvents() && HUD.INSTANCE.getBlackHotbar()) {
            renderExperience(x, y - 6);
        } else {
            renderExperience(x, y);
        }
    }

    @Redirect(method = "renderGameOverlay(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;renderJumpBar(II)V", remap = false))
    private void hookRenderHorseEXP(GuiIngameForge instance, int x, int y) {
        if (HUD.INSTANCE.handleEvents() && HUD.INSTANCE.getBlackHotbar()) {
            renderJumpBar(x, y - 6);
        } else {
            renderJumpBar(x, y);
        }
    }

    @Redirect(method = "renderGameOverlay(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;renderArmor(II)V", remap = false))
    private void hookRenderArmor(GuiIngameForge instance, int x, int y) {
        if (HUD.INSTANCE.handleEvents() && HUD.INSTANCE.getBlackHotbar()) {
            renderArmor(x, y - 12);
        } else {
            renderArmor(x, y);
        }
    }

    @Redirect(method = "renderExperience(II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/TextRenderer;drawString(Ljava/lang/String;III)I", remap = false))
    private int hookExperienceLevelYText(TextRenderer instance, String text, int x, int y, int color) {
        return HUD.INSTANCE.handleEvents() && HUD.INSTANCE.getBlackHotbar() ? instance.drawString(text, x, y - 6, color) : instance.drawString(text, x, y, color);
    }*/
}