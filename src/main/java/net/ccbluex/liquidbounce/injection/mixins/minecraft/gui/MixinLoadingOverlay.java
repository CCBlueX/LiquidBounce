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
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent;
import net.ccbluex.liquidbounce.features.misc.SelfDestruct;
import net.ccbluex.liquidbounce.render.splash.MenuWait;
import net.ccbluex.liquidbounce.render.splash.Splash;
import net.ccbluex.liquidbounce.render.splash.SplashPhaseScreen;
import net.ccbluex.liquidbounce.render.splash.SplashProgress;
import net.ccbluex.liquidbounce.render.splash.SplashTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.IntSupplier;

/**
 * LiquidBounce Splash Screen: plays the active theme's splash while resources load. When a {@link SplashPhaseScreen}
 * follows (browser choice, startup tasks), it takes over the same splash instead of the overlay fading into it.
 */
@Mixin(LoadingOverlay.class)
public abstract class MixinLoadingOverlay {

    @Unique
    private static final IntSupplier CLIENT_ARGB = Splash.INSTANCE::getBackground;

    /**
     * Once loading is done, the overlay stays opaque for this long before it fades out.
     */
    @Unique
    private static final long FADE_OUT_DELAY = 1000L;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private long fadeOutStart;

    @Shadow
    private float currentProgress;

    @Unique
    private boolean begun;

    @Unique
    private boolean outroRequested;

    @Unique
    private MenuWait menu;

    @Inject(method = "registerTextures", at = @At("RETURN"))
    private static void prepareSplash(TextureManager textureManager, CallbackInfo ci) {
        Splash.INSTANCE.prepare();
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
        return showsVanilla();
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
        if (SelfDestruct.INSTANCE.isDestructed() || handsOver()) {
            return;
        }

        if (!begun) {
            begun = true;
            Splash.INSTANCE.begin();
        }

        // Loading is done: the intro hurries to finish while the overlay stays opaque, then the outro plays
        if (fadeOutStart > -1L && !(minecraft.gui.screen() instanceof SplashPhaseScreen)) {
            Splash.INSTANCE.hurry();
            if (menu == null) {
                menu = new MenuWait();
            }

            // Stays opaque until the menu behind it shows
            var now = Util.getMillis();
            if (!outroRequested && now - fadeOutStart >= FADE_OUT_DELAY) {
                if (menu.isOver()) {
                    outroRequested = true;
                    Splash.INSTANCE.request(SplashTimeline.Phase.OUTRO);
                } else {
                    fadeOutStart = now - FADE_OUT_DELAY;
                }
            }
        }

        Splash.INSTANCE.render(graphics, SplashProgress.loading(currentProgress), ARGB.alphaFloat(color));
    }

    /**
     * The splash phase behind the fading overlay draws the same logo and bar, so the overlay steps aside.
     */
    @WrapWithCondition(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
    private boolean drawFadingBackground(GuiGraphicsExtractor instance, int x0, int y0, int x1, int y1, int color) {
        return SelfDestruct.INSTANCE.isDestructed() || !handsOver();
    }

    /**
     * Minecraft's own loading screen, for a destructed client or when there is no splash to draw.
     */
    @Unique
    private static boolean showsVanilla() {
        return SelfDestruct.INSTANCE.isDestructed() || Splash.isVanilla();
    }

    @Unique
    private boolean handsOver() {
        return fadeOutStart > -1L && Util.getMillis() - fadeOutStart >= FADE_OUT_DELAY
                && minecraft.gui.screen() instanceof SplashPhaseScreen;
    }

    @WrapWithCondition(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;extractProgressBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIF)V"))
    private boolean drawProgressBar(LoadingOverlay instance, GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, float fade) {
        return showsVanilla();
    }

    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;BRAND_BACKGROUND:Ljava/util/function/IntSupplier;", opcode = Opcodes.GETSTATIC))
    private IntSupplier withClientColor(IntSupplier original) {
        return showsVanilla() ? original : CLIENT_ARGB;
    }

}
