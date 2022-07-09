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
public class MixinFontRenderer
{
    private boolean rainbowEnabled0;
    private boolean rainbowEnabled1;

    @Debug(print = true)
    @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;renderString(Ljava/lang/String;FFIZ)I", ordinal = 0), require = 1, allow = 1)
    private void drawString_injectRainbowFontShaderPre(final String text, final float x, final float y, final int color, final boolean dropShadow, final CallbackInfoReturnable<Integer> cir)
    {
        rainbowEnabled0 = RainbowFontShader.INSTANCE.isInUse();

        if (rainbowEnabled0)
            GL20.glUseProgram(0);
    }

    @Debug(print = true)
    @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;renderString(Ljava/lang/String;FFIZ)I", ordinal = 1), require = 1, allow = 1)
    private void drawString_injectRainbowFontShaderPost(final String text, final float x, final float y, final int color, final boolean dropShadow, final CallbackInfoReturnable<Integer> cir)
    {
        if (rainbowEnabled0)
            GL20.glUseProgram(RainbowFontShader.INSTANCE.getProgramId());
    }

    @Debug(print = true)
    @Inject(method = "renderStringAtPos", at = @At("HEAD"), require = 1, allow = 1)
    private void renderStringAtPos_injectRainbowFontShaderPre(final String text, final boolean shadow, final CallbackInfo ci)
    {
        rainbowEnabled1 = RainbowFontShader.INSTANCE.isInUse();
    }

    @Debug(print = true)
    @Inject(method = "renderStringAtPos", at = @At("RETURN"), require = 1, allow = 1)
    private void renderStringAtPos_injectRainbowFontShaderPost(final String text, final boolean shadow, final CallbackInfo ci)
    {
        if (rainbowEnabled1)
            GL20.glUseProgram(RainbowFontShader.INSTANCE.getProgramId());
    }

    @Debug(print = true)
    @Inject(method = "renderStringAtPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;setColor(FFFF)V", ordinal = 0), require = 1, allow = 1)
    private void renderStringAtPos_injectRainbowFontShader0(final String text, final boolean shadow, final CallbackInfo ci)
    {
        if (rainbowEnabled1)
            GL20.glUseProgram(0);
    }

    @Debug(print = true)
    @Inject(method = "renderStringAtPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;setColor(FFFF)V", ordinal = 1), require = 1, allow = 1)
    private void renderStringAtPos_injectRainbowFontShader1(final String text, final boolean shadow, final CallbackInfo ci)
    {
        if (rainbowEnabled1)
            GL20.glUseProgram(RainbowFontShader.INSTANCE.getProgramId());
    }

    @ModifyVariable(method = "renderString", at = @At("HEAD"), require = 1, ordinal = 0)
    private String renderString_handleTextEvent(final String string)
    {
        if (string == null)
            return null;

        if (LiquidBounce.eventManager == null)
            return string;

        final TextEvent textEvent = new TextEvent(string);
        LiquidBounce.eventManager.callEvent(textEvent);
        return textEvent.getText();
    }

    @ModifyVariable(method = "getStringWidth", at = @At("HEAD"), require = 1, ordinal = 0)
    private String getStringWidth_handleTextEvent(final String string)
    {
        if (string == null)
            return null;

        if (LiquidBounce.eventManager == null)
            return string;

        final TextEvent textEvent = new TextEvent(string);
        LiquidBounce.eventManager.callEvent(textEvent);
        return textEvent.getText();
    }
}
