/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent;
import net.ccbluex.liquidbounce.event.events.SplashOverlayEvent;
import net.ccbluex.liquidbounce.event.events.SplashProgressEvent;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.IntSupplier;

/**
 * Custom ultralight splash screen
 */
@Mixin(SplashOverlay.class)
public class MixinSplashOverlay {

    @Shadow
    @Final
    private ResourceReload reload;

    @Mutable
    @Shadow
    @Final
    private static IntSupplier BRAND_ARGB;

    @Shadow
    @Final
    private static int MONOCHROME_BLACK;

    @Shadow
    @Final
    private static int MOJANG_RED;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void hookInit(CallbackInfo ci) {
        if (!HideAppearance.INSTANCE.isHidingNow()) {
            EventManager.INSTANCE.callEvent(new SplashOverlayEvent(true));
            BRAND_ARGB = () -> ColorHelper.Argb.getArgb(255, 24, 26, 27);
        } else {
            BRAND_ARGB = () -> MinecraftClient.getInstance().options.getMonochromeLogo().getValue() ? MONOCHROME_BLACK : MOJANG_RED;
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setOverlay(Lnet/minecraft/client/gui/screen/Overlay;)V"))
    private void hookEnd(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new SplashOverlayEvent(false));
    }

    @Unique
    private float splashProgressBefore = -1;

    @Unique
    private boolean hasBeenCompleted = false;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ResourceReload;getProgress()F"))
    private void hookProgress(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (HideAppearance.INSTANCE.isHidingNow()) {
            return;
        }

        var progress = reload.getProgress();
        var isComplete = reload.isComplete();

        if (hasBeenCompleted) {
            return;
        }

        if (splashProgressBefore != progress || isComplete) {
            EventManager.INSTANCE.callEvent(new SplashProgressEvent(progress, isComplete));
            splashProgressBefore = progress;

            if (isComplete) {
                hasBeenCompleted = true;
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ScreenRenderEvent(context, delta));
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIFFIIII)V"))
    private boolean drawTexture(DrawContext instance, Identifier texture, int x, int y, int width, int height,
                                float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        // do not draw texture - only when hiding
        return HideAppearance.INSTANCE.isHidingNow();
    }



}
