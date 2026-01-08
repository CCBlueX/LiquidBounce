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
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.ccbluex.liquidbounce.common.ClientLogoTexture;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.render.ClientRenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.IntSupplier;

/**
 * LiquidBounce Splash Screen
 */
@Mixin(LoadingOverlay.class)
public abstract class MixinLoadingOverlay {

    @Unique
    private static final IntSupplier CLIENT_ARGB = () -> ARGB.color(255, 24, 26, 27);

    @Inject(method = "registerTextures", at = @At("RETURN"))
    private static void initializeTexture(TextureManager textureManager, CallbackInfo ci) {
        textureManager.registerAndLoad(ClientLogoTexture.CLIENT_LOGO, new ClientLogoTexture());
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void render(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ScreenRenderEvent(context, delta));
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V"))
    private boolean drawMojangLogo(GuiGraphics instance, RenderPipeline renderPipeline, Identifier sprite, int x, int y, float u, float v, int width, int height, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
        return HideAppearance.INSTANCE.isHidingNow();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/ReloadInstance;getActualProgress()F"))
    private void drawClientLogo(
            GuiGraphics context,
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

        int screenWidth = context.guiWidth();
        int screenHeight = context.guiHeight();

        float scaleFactor = Math.min(screenWidth * 0.4f / ClientLogoTexture.WIDTH, screenHeight * 0.25f / ClientLogoTexture.HEIGHT);

        int displayWidth = (int)(ClientLogoTexture.WIDTH * scaleFactor);
        int displayHeight = (int)(ClientLogoTexture.HEIGHT * scaleFactor);

        int x = (screenWidth - displayWidth) / 2;
        int y = (screenHeight - displayHeight) / 2;

        // TODO: Draw as SVG instead of PNG
        context.blit(
            ClientRenderPipelines.JCEF.SMOOTH_TEXTURE,
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

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;BRAND_BACKGROUND:Ljava/util/function/IntSupplier;"))
    private IntSupplier withClientColor(IntSupplier original) {
        return HideAppearance.INSTANCE.isHidingNow() ? original : CLIENT_ARGB;
    }

}
