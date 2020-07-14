/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.TextEvent;
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontRenderer.class)
@Debug(export = true, print = true)
@SideOnly(Side.CLIENT)
public class MixinFontRenderer {
    // Local Variable
    private boolean rainbowEnabled0 = false;
    // Local Variable
    private boolean rainbowEnabled1 = false;

    @Debug(print = true)
    @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;renderString(Ljava/lang/String;FFIZ)I", ordinal = 0), require = 1, allow = 1)
    private void injectShadow1(String text, float x, float y, int color, boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        rainbowEnabled0 = RainbowFontShader.INSTANCE.isInUse();

        if (rainbowEnabled0) {
            GL20.glUseProgram(0);
        }
    }

    @Debug(print = true)
    @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;renderString(Ljava/lang/String;FFIZ)I", ordinal = 1), require = 1, allow = 1)
    private void injectShadow2(String text, float x, float y, int color, boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        if (rainbowEnabled0) {
            GL20.glUseProgram(RainbowFontShader.INSTANCE.getProgramId());
        }
    }

    @Debug(print = true)
    @Inject(method = "renderStringAtPos", at = @At(value = "HEAD"), require = 1, allow = 1)
    private void injectRainbow5(String text, boolean shadow, CallbackInfo ci) {
        rainbowEnabled1 = RainbowFontShader.INSTANCE.isInUse();
    }

    @Debug(print = true)
    @Inject(method = "renderStringAtPos", at = @At(value = "RETURN"), require = 1, allow = 1)
    private void injectRainbow6(String text, boolean shadow, CallbackInfo ci) {
        if (rainbowEnabled1) {
            GL20.glUseProgram(RainbowFontShader.INSTANCE.getProgramId());
        }
    }

    @Debug(print = true)
    @Inject(method = "renderStringAtPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;setColor(FFFF)V", ordinal = 0), require = 1, allow = 1)
    private void injectRainbow3(String text, boolean shadow, CallbackInfo ci) {
        if (rainbowEnabled1) {
            GL20.glUseProgram(0);
        }
    }

    @Debug(print = true)
    @Inject(method = "renderStringAtPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;setColor(FFFF)V", ordinal = 1), require = 1, allow = 1)
    private void injectRainbow4(String text, boolean shadow, CallbackInfo ci) {
        if (rainbowEnabled1) {
            GL20.glUseProgram(RainbowFontShader.INSTANCE.getProgramId());
        }
    }

    @ModifyVariable(method = "renderString", at = @At("HEAD"), require = 1, ordinal = 0)
    private String renderString(final String string) {
        if (string == null)
            return null;
        if (LiquidBounce.eventManager == null)
            return string;

        final TextEvent textEvent = new TextEvent(string);
        LiquidBounce.eventManager.callEvent(textEvent);
        return textEvent.getText();
    }

    @ModifyVariable(method = "getStringWidth", at = @At("HEAD"), require = 1, ordinal = 0)
    private String getStringWidth(final String string) {
        if (string == null)
            return null;
        if (LiquidBounce.eventManager == null)
            return string;

        final TextEvent textEvent = new TextEvent(string);
        LiquidBounce.eventManager.callEvent(textEvent);
        return textEvent.getText();
    }
}