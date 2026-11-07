/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.ccbluex.liquidbounce.common.ClientLogoTexture;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent;
import net.ccbluex.liquidbounce.features.misc.SelfDestruct;
import net.ccbluex.liquidbounce.render.SplashScreenRenderer;
import net.ccbluex.liquidbounce.render.SplashScreenSounds;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.IntSupplier;

/**
 * LiquidBounce Splash Screen: the logo mark drops in, splashes and bounces into place.
 */
@Mixin(LoadingOverlay.class)
public abstract class MixinLoadingOverlay {

    @Unique
    private static final IntSupplier CLIENT_ARGB = () -> ARGB.color(255, 24, 26, 27);

    /**
     * Once loading is done, the overlay stays opaque for this long before it fades out.
     */
    @Unique
    private static final long FADE_OUT_DELAY = 1000L;

    @Shadow
    private long fadeOutStart;

    @Shadow
    private float currentProgress;

    @Unique
    private long lastFrame = -1L;

    @Unique
    private float playhead;

    @Unique
    private boolean outroStarted;

    @Inject(method = "registerTextures", at = @At("RETURN"))
    private static void initializeTexture(TextureManager textureManager, CallbackInfo ci) {
        textureManager.registerAndLoad(ClientLogoTexture.CLIENT_LOGO, new ClientLogoTexture());
        SplashScreenSounds.INSTANCE.load();
    }

    /**
     * Theme screens draw through this event; fired with the screen behind the overlay so the splash stays on top.
     */
    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", shift = At.Shift.AFTER))
    private void hookScreenRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ScreenRenderEvent(context, delta));
    }

    @WrapWithCondition(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V"))
    private boolean drawMojangLogo(GuiGraphicsExtractor instance, RenderPipeline renderPipeline, Identifier sprite, int x, int y, float u, float v, int width, int height, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
        return SelfDestruct.INSTANCE.isDestructed();
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/ReloadInstance;getActualProgress()F"))
    private void drawClientLogo(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float a,
            CallbackInfo ci,
            @Local(name = "color") int color
    ) {
        if (SelfDestruct.INSTANCE.isDestructed()) {
            return;
        }

        float time = advancePlayhead();

        // Vanilla fades the overlay out during the second after FADE_OUT_DELAY
        float outro = fadeOutStart > -1L ? Math.clamp((Util.getMillis() - fadeOutStart - FADE_OUT_DELAY) / 1000.0F, 0.0F, 1.0F) : 0.0F;
        if (outro > 0.0F && !outroStarted) {
            outroStarted = true;
            SplashScreenSounds.INSTANCE.playOutro();
        }

        SplashScreenRenderer.INSTANCE.render(graphics, time, currentProgress, ARGB.alphaFloat(color), outro);
    }

    @WrapWithCondition(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;extractProgressBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIF)V"))
    private boolean drawProgressBar(LoadingOverlay instance, GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, float fade) {
        return SelfDestruct.INSTANCE.isDestructed();
    }

    /**
     * Plays the intro in real time; loading never waits for it. When loading finishes first,
     * the rest of the intro speeds up to end before the overlay fades out.
     */
    @Unique
    private float advancePlayhead() {
        long now = Util.getMillis();
        if (lastFrame == -1L) {
            lastFrame = now;
        }

        float delta = Math.min(now - lastFrame, 100L);
        if (fadeOutStart > -1L) {
            long timeLeft = Math.max(fadeOutStart + FADE_OUT_DELAY - now, 1L);
            delta *= Math.max(1.0F, (SplashScreenRenderer.LENGTH - playhead) / timeLeft);
        }
        float previous = playhead;
        playhead = Math.min(playhead + delta, SplashScreenRenderer.LENGTH);
        lastFrame = now;
        SplashScreenSounds.INSTANCE.play(previous, playhead);
        return playhead;
    }

    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;BRAND_BACKGROUND:Ljava/util/function/IntSupplier;", opcode = Opcodes.GETSTATIC))
    private IntSupplier withClientColor(IntSupplier original) {
        return SelfDestruct.INSTANCE.isDestructed() ? original : CLIENT_ARGB;
    }

}
