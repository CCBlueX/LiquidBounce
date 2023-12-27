/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import org.checkerframework.checker.units.qual.A;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Custom ultralight splash screen
 */
@Mixin(SplashOverlay.class)
public class MixinSplashOverlay {

    @Shadow
    @Final
    private ResourceReload reload;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void hookInit(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new SplashOverlayEvent(SplashOverlayEvent.Action.SHOW));
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
        EventManager.INSTANCE.callEvent(new ScreenRenderEvent(null, context, mouseX, mouseY, delta));
    }

}
