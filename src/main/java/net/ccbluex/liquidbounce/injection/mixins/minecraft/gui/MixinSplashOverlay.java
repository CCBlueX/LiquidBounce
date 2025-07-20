/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import net.ccbluex.liquidbounce.common.ClientLogoTexture;
import net.ccbluex.liquidbounce.common.RenderLayerExtensions;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;
import java.util.function.IntSupplier;

/**
 * LiquidBounce Splash Screen
 */
@Mixin(SplashOverlay.class)
public class MixinSplashOverlay {

    @Unique
    private static final IntSupplier CLIENT_ARGB = () -> ColorHelper.getArgb(255, 24, 26, 27);

    @Inject(method = "init", at = @At("RETURN"))
    private static void initializeTexture(TextureManager textureManager, CallbackInfo ci) {
        textureManager.registerTexture(ClientLogoTexture.CLIENT_LOGO, new ClientLogoTexture());
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ScreenRenderEvent(context, delta));
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIIIIII)V"))
    private boolean drawMojangLogo(DrawContext instance, Function<Identifier, RenderLayer> renderLayers, Identifier sprite, int x, int y, float u, float v, int width, int height, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
        return HideAppearance.INSTANCE.isHidingNow();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ResourceReload;getProgress()F"))
    private void drawClientLogo(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci,
            @Local(name = "i", index = 5) int scaledWindowWidth,
            @Local(name = "j", index = 6) int scaledWindowHeight,
            @Local(name = "s", index = 20) int color
    ) {
        // Don't draw the logo if the appearance is hidden
        if (HideAppearance.INSTANCE.isHidingNow()) {
            return;
        }

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        float scaleFactor = Math.min(screenWidth * 0.4f / ClientLogoTexture.WIDTH, screenHeight * 0.25f / ClientLogoTexture.HEIGHT);

        int displayWidth = (int)(ClientLogoTexture.WIDTH * scaleFactor);
        int displayHeight = (int)(ClientLogoTexture.HEIGHT * scaleFactor);

        int x = (screenWidth - displayWidth) / 2;
        int y = (screenHeight - displayHeight) / 2;

        // TODO: Draw as SVG instead of PNG
        context.drawTexture(
                RenderLayerExtensions::getSmoothTextureLayer,
                ClientLogoTexture.CLIENT_LOGO,
                x,
                y,
                0.0F,
                0.0F,
                displayWidth,
                displayHeight,
                ClientLogoTexture.WIDTH,
                ClientLogoTexture.HEIGHT,
                ClientLogoTexture.WIDTH,
                ClientLogoTexture.HEIGHT,
                color
        );
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/SplashOverlay;BRAND_ARGB:Ljava/util/function/IntSupplier;"))
    private IntSupplier withClientColor(IntSupplier original) {
        return HideAppearance.INSTANCE.isHidingNow() ? original : CLIENT_ARGB;
    }

}
