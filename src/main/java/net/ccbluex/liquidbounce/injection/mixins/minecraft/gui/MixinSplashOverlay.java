/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.render.ultralight.RenderLayer;
import net.ccbluex.liquidbounce.render.ultralight.UltralightEngine;
import net.ccbluex.liquidbounce.render.ultralight.View;
import net.ccbluex.liquidbounce.render.ultralight.theme.Page;
import net.ccbluex.liquidbounce.render.ultralight.theme.ThemeManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceReload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Custom ultralight splash screen
 */
@Mixin(SplashOverlay.class)
public class MixinSplashOverlay {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ResourceReload reload;

    private View view = null;
    private boolean closing = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void hookSplashInit(CallbackInfo callbackInfo) {
        final Page page = ThemeManager.INSTANCE.page("splashscreen");
        if (page == null)
            return;

        view = UltralightEngine.INSTANCE.newSplashView();
        view.loadPage(page);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hookSplashRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo callbackInfo) {
        if (view != null) {
            if (this.reload.isComplete()) {
                if (this.client.currentScreen != null) {
                    this.client.currentScreen.render(matrices, 0, 0, delta);
                }

                if (!closing) {
                    closing = true;
                    if (view.getContext().getEvents()._fireViewClose()) {
                        UltralightEngine.INSTANCE.removeView(view);
                        this.client.setOverlay(null);
                    }
                }
            }

            UltralightEngine.INSTANCE.render(RenderLayer.SPLASH_LAYER, matrices);
            callbackInfo.cancel();
        }
    }

}
