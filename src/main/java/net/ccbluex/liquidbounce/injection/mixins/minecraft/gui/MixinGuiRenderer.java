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
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.ccbluex.liquidbounce.render.engine.BlurEffectRenderer;
import net.ccbluex.liquidbounce.render.gui.GuiCircleLutAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@NullMarked
@Mixin(GuiRenderer.class)
public abstract class MixinGuiRenderer {

    @ModifyExpressionValue(method = "addElementToMesh", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;scissorChanged(Lnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)Z"))
    private boolean uploadPrimitivesIndividually(boolean original, @Local(name = "pipeline") RenderPipeline pipeline) {
        return original || pipeline.getPrimitiveTopology().connectedPrimitives;
    }

    @WrapOperation(
        method = "draw",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;mainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget injectBlurRenderTarget(GameRenderer instance, Operation<RenderTarget> original) {
        BlurEffectRenderer blurEffectRenderer = BlurEffectRenderer.INSTANCE;
        if (blurEffectRenderer.shouldDrawBlur()) {
            blurEffectRenderer.setDrawingHudFramebuffer(true);
            return blurEffectRenderer.getOverlayRenderTargetHolder().initAndGet();
        }
        return original.call(instance);
    }

    @Inject(
        method = "draw", at = @At("RETURN")
    )
    private void afterRenderBlurOverlay(CallbackInfo ci) {
        GuiCircleLutAtlas.INSTANCE.resetForNextDraw();
        BlurEffectRenderer.INSTANCE.blitBlurOverlay();
    }

}
