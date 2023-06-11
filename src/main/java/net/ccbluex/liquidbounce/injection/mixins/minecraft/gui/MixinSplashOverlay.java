/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

import net.ccbluex.liquidbounce.base.ultralight.RenderLayer;
import net.ccbluex.liquidbounce.base.ultralight.UltralightEngine;
import net.ccbluex.liquidbounce.base.ultralight.ViewOverlay;
import net.ccbluex.liquidbounce.base.ultralight.theme.Page;
import net.ccbluex.liquidbounce.base.ultralight.theme.ThemeManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceReload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Custom ultralight splash screen
 */
@Mixin(SplashOverlay.class)
public class MixinSplashOverlay {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ResourceReload reload;
    @Shadow @Final private Consumer<Optional<Throwable>> exceptionHandler;
    private ViewOverlay viewOverlay = null;
    private boolean closing = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void hookSplashInit(CallbackInfo callbackInfo) {
        final Page page = ThemeManager.INSTANCE.page("splashscreen");
        if (page == null)
            return;

        viewOverlay = UltralightEngine.INSTANCE.newSplashView();
        viewOverlay.loadPage(page);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hookSplashRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (viewOverlay != null) {
            if (this.reload.isComplete()) {
                if (this.client.currentScreen != null) {
                    this.client.currentScreen.render(context, 0, 0, delta);
                }

                if (!closing) {
                    closing = true;
                    this.client.setOverlay(null);

                    UltralightEngine.INSTANCE.removeView(viewOverlay);
                }

                try {
                    this.reload.throwException();
                    this.exceptionHandler.accept(Optional.empty());
                } catch (Throwable throwable) {
                    this.exceptionHandler.accept(Optional.of(throwable));
                }
            }

            UltralightEngine.INSTANCE.render(RenderLayer.SPLASH_LAYER, context);
            ci.cancel();
        }
    }

}
