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
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.ccbluex.liquidbounce.render.engine.BlurEffectRenderer;
import net.ccbluex.liquidbounce.render.gui.GuiCircleLutAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

@NullMarked
@Mixin(GuiRenderer.class)
public abstract class MixinGuiRenderer {

    @Shadow
    protected abstract void executeDraw(GuiRenderer.Draw draw, RenderPass renderPass);

    @Unique
    private final List<GuiRenderer.Draw> invertingDraws = new ArrayList<>();

    @Unique
    private @Nullable GpuBufferSlice invertingDrawTransforms;

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

    /**
     * The crosshair inverts the pixels below it. On the blur overlay these are transparent,
     * so it would always come out white; draw it onto the main target after the overlay instead.
     */
    @WrapOperation(
        method = "executeDrawRange",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDraw(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/renderpearl/api/commands/RenderPass;)V")
    )
    private void deferInvertingDraws(GuiRenderer instance, GuiRenderer.Draw draw, RenderPass renderPass,
                                     Operation<Void> original,
                                     @Local(argsOnly = true) GpuBufferSlice dynamicTransforms) {
        if (BlurEffectRenderer.INSTANCE.isDrawingHudFramebuffer() && draw.pipeline() == RenderPipelines.CROSSHAIR) {
            invertingDraws.add(draw);
            invertingDrawTransforms = dynamicTransforms;
            return;
        }
        original.call(instance, draw, renderPass);
    }

    @Inject(
        method = "draw", at = @At("RETURN")
    )
    private void afterRenderBlurOverlay(CallbackInfo ci) {
        GuiCircleLutAtlas.INSTANCE.resetForNextDraw();
        BlurEffectRenderer.INSTANCE.blitBlurOverlay();
        executeInvertingDraws();
    }

    @Unique
    private void executeInvertingDraws() {
        if (invertingDraws.isEmpty() || invertingDrawTransforms == null) {
            return;
        }

        RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            () -> "GUI inverting draws",
            target.getColorTextureView(),
            Optional.empty(),
            target.hasDepth() ? target.getDepthTextureView() : null,
            OptionalDouble.empty()
        )) {
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", invertingDrawTransforms);
            for (GuiRenderer.Draw draw : invertingDraws) {
                executeDraw(draw, renderPass);
            }
        }

        invertingDraws.clear();
        invertingDrawTransforms = null;
    }

}
