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

import net.minecraft.client.gui.screen.SplashScreen;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Custom ultralight splash screen
 */
@Mixin(SplashScreen.class)
public class MixinSplashScreen {

//    @Shadow @Final private MinecraftClient client;
//
//    @Shadow @Final private ResourceReload reload;
//    private View view = null;
//    private boolean closing = false;
//
//    @Inject(method = "<init>", at = @At("RETURN"))
//    private void hookSplashInit(CallbackInfo callbackInfo) {
//        final Page page = ThemeManager.INSTANCE.page("splashscreen");
//        if (page == null)
//            return;
//
//        view = UltralightEngine.INSTANCE.newSplashView();
//        view.loadPage(page);
//    }
//
//    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
//    private void hookSplashRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo callbackInfo) {
//        if (view != null) {
//            if (reload.isComplete()) {
//                if (this.client.currentScreen != null) {
//                    this.client.currentScreen.render(matrices, 0, 0, delta);
//                }
//
//                if (!closing) {
//                    closing = true;
//                    if (view.getContext().getEvents()._fireViewClose()) {
//                        UltralightEngine.INSTANCE.removeView(view);
//                        this.client.setOverlay(null);
//                    }
//                }
//            }
//
//            UltralightEngine.INSTANCE.render(RenderLayer.SPLASH_LAYER);
//
//            callbackInfo.cancel();
//        }
//    }

}
