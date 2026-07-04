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

package net.ccbluex.liquidbounce.injection.mixins.blaze3d.vk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vulkan.VulkanRenderPass;
import com.mojang.blaze3d.vulkan.VulkanRenderPipeline;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Objects;

@Mixin(VulkanRenderPass.class)
public abstract class MixinVulkanRenderPass {

    @Shadow
    @Nullable
    protected VulkanRenderPipeline pipeline;

    @Unique
    private @Nullable RenderPipeline liquid_bounce$previousPipeline;

    @Shadow
    @Final
    protected HashMap<String, GpuBufferSlice> uniforms;

    @Inject(method = "setPipeline", at = @At("HEAD"), cancellable = true)
    private void skipBindPipeline(RenderPipeline pipeline, CallbackInfo ci) {
        if (pipeline == liquid_bounce$previousPipeline) {
            ci.cancel();
        }

        this.liquid_bounce$previousPipeline = pipeline;
    }

    @Inject(
        method = "setUniform(Ljava/lang/String;Lcom/mojang/blaze3d/buffers/GpuBuffer;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void skipSetUniform(String name, GpuBuffer value, CallbackInfo ci) {
        if (Objects.equals(this.uniforms.get(name), value.slice())) {
            ci.cancel();
        }
    }

    @Inject(
        method = "setUniform(Ljava/lang/String;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void skipSetUniformSlice(String name, GpuBufferSlice value, CallbackInfo ci) {
        if (Objects.equals(this.uniforms.get(name), value)) {
            ci.cancel();
        }
    }

}
