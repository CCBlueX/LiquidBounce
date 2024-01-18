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

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent;
import net.ccbluex.liquidbounce.event.events.SplashOverlayEvent;
import net.ccbluex.liquidbounce.event.events.SplashProgressEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.math.ColorHelper;
import org.checkerframework.checker.units.qual.A;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Inject(method = "<init>", at = @At("RETURN"))
    private void hookInit(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new SplashOverlayEvent(SplashOverlayEvent.Action.SHOW));
        BRAND_ARGB = () -> ColorHelper.Argb.getArgb(255, 24, 26, 27);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setOverlay(Lnet/minecraft/client/gui/screen/Overlay;)V"))
    private void hookEnd(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new SplashOverlayEvent(SplashOverlayEvent.Action.HIDE));
    }

    @Unique
    private float splashProgressBefore = -1;

    @Unique
    private boolean hasBeenCompleted = false;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ResourceReload;getProgress()F"))
    private void hookProgress(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
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
        EventManager.INSTANCE.callEvent(new ScreenRenderEvent());
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIFFIIII)V"))
    private void drawTexture(DrawContext context, net.minecraft.util.Identifier identifier,
                             int x, int y, int width, int height, float u1, float v1, int u2,
                             int v2, int textureWidth, int textureHeight) {
        // do not draw
    }



}
