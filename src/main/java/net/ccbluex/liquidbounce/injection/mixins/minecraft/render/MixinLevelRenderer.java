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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.ccbluex.liquidbounce.common.OutlineFlag;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.DrawOutlinesEvent;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBlockOutline;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCustomAmbience;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.render.engine.OutlineShaderRenderer;
import net.ccbluex.liquidbounce.utils.collection.Pools;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Nullable
    public abstract RenderTarget entityOutlineTarget();

    @Shadow
    protected abstract boolean shouldShowEntityOutlines();

    // After ModelViewMatrix setup
    @Inject(method = "renderLevel", at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;"))
    private void onRender(GraphicsResourceAllocator allocator, DeltaTracker tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        OutlineShaderRenderer renderer = OutlineShaderRenderer.INSTANCE;
        if (!renderer.shouldRender()) {
            return;
        }

        var matrixStack = Pools.MatStack.borrow();
        var event = new DrawOutlinesEvent(
            renderer.prepareRenderTarget(),
            matrixStack,
            camera,
            tickCounter.getGameTimeDeltaPartialTick(false),
            DrawOutlinesEvent.OutlineType.INBUILT_OUTLINE
        );
        EventManager.INSTANCE.callEvent(event);
        Pools.MatStack.recycle(matrixStack);

        if (event.getDirtyFlag()) {
            renderer.setDirty(true);
        }
    }

    @ModifyExpressionValue(method = "method_62218", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;colorFromFloat(FFFF)I"))
    private int customFogClearColor(int original) {
        return ModuleCustomAmbience.FogValueGroup.INSTANCE.modifyClearColor(original);
    }

    // this method is a lambda
    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"))
    private void onDrawOutlines(GpuBufferSlice gpuBufferSlice, LevelRenderState worldRenderState, ProfilerFiller profiler,
        Matrix4f matrix4f, ResourceHandle handle, ResourceHandle handle2, boolean bl, ResourceHandle handle3, ResourceHandle handle4, CallbackInfo ci) {
        OutlineShaderRenderer.INSTANCE.drawBlitIfDirty(this.minecraft.getMainRenderTarget());
    }

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V", shift = At.Shift.BEFORE))
    private void onRenderGlow(GpuBufferSlice gpuBufferSlice, LevelRenderState worldRenderState, ProfilerFiller profiler,
        Matrix4f matrix4f, ResourceHandle handle, ResourceHandle handle2, boolean bl, ResourceHandle handle3, ResourceHandle handle4, CallbackInfo ci) {
        var entityOutlineFb = entityOutlineTarget();
        if (!this.shouldShowEntityOutlines() || entityOutlineFb == null) {
            return;
        }

        var matrixStack = Pools.MatStack.borrow();
        entityOutlineFb.blitToScreen();
        final var camera = this.minecraft.gameRenderer.getMainCamera();
        var event = new DrawOutlinesEvent(
            entityOutlineFb, matrixStack,
            camera, this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false),
            DrawOutlinesEvent.OutlineType.MINECRAFT_GLOW
        );
        EventManager.INSTANCE.callEvent(event);
        Pools.MatStack.recycle(matrixStack);
        OutlineFlag.drawOutline |= event.getDirtyFlag();
    }

    @WrapOperation(method = "renderLevel", at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/client/renderer/state/LevelRenderState;haveGlowingEntities:Z"
    ))
    private boolean modifyDrawOutline(LevelRenderState instance, Operation<Boolean> original) {
        var flag = OutlineFlag.drawOutline;
        if (flag) {
            OutlineFlag.drawOutline = false;
            return true;
        }
        return original.call(instance);
    }

    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;cullTerrain(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Z)V"), index = 2)
    private boolean renderSetupTerrainModifyArg(boolean spectator) {
        return ModuleFreeCam.INSTANCE.getRunning() || spectator;
    }

    @Inject(method = "renderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void cancelBlockOutline(MultiBufferSource.BufferSource immediate, PoseStack matrices, boolean renderBlockOutline, LevelRenderState renderStates, CallbackInfo ci) {
        if (ModuleBlockOutline.INSTANCE.getRunning()) {
            ci.cancel();
        }
    }

}
