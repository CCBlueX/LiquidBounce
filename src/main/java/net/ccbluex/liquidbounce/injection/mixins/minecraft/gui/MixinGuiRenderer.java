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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.ccbluex.liquidbounce.render.engine.BlurEffectRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@NullMarked
@Mixin(GuiRenderer.class)
public abstract class MixinGuiRenderer {

    /*
     * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *     http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     *
     * https://github.com/FabricMC/fabric/blob/320674d1c713640a2e71834c1e3eb379e80a49fb/fabric-rendering-v1/src/client/java/net/fabricmc/fabric/mixin/client/rendering/GuiRendererMixin.java#L111-L128
     */
    @WrapOperation(
        method = "executeDraw(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setIndexBuffer(Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V")
    )
    private void fixIndexBufferType(
        RenderPass instance,
        GpuBuffer gpuBuffer,
        VertexFormat.IndexType indexType,
        Operation<Void> original,
        @Local(argsOnly = true) GuiRenderer.Draw draw
    ) {
        var pipeline = draw.pipeline();
        if (pipeline.getVertexFormatMode() != VertexFormat.Mode.QUADS) {
            var shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            gpuBuffer = shapeIndexBuffer.getBuffer(draw.indexCount());
            indexType = shapeIndexBuffer.type();
        }

        original.call(instance, gpuBuffer, indexType);
    }

    @WrapOperation(
        method = "draw",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget injectBlurRenderTarget(Minecraft instance, Operation<RenderTarget> original) {
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
    private void afterRenderBlurOverlay(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        BlurEffectRenderer.INSTANCE.blitBlurOverlay();
    }

}
