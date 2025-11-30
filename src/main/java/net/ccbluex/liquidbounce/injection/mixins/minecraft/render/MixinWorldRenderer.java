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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.ccbluex.liquidbounce.common.OutlineFlag;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.DrawOutlinesEvent;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.ccbluex.liquidbounce.render.engine.OutlineFramebufferHolder;
import net.ccbluex.liquidbounce.utils.collection.Pools;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Nullable
    public abstract Framebuffer getEntityOutlinesFramebuffer();

    @Shadow
    protected abstract boolean canDrawEntityOutlines();

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        var matrixStack = Pools.MatStack.borrow();
        // Apply camera transformation to fix outline positioning
        matrixStack.peek().getPositionMatrix().mul(positionMatrix);

        var event = new DrawOutlinesEvent(OutlineFramebufferHolder.prepare(), matrixStack, camera, tickCounter.getTickProgress(false), DrawOutlinesEvent.OutlineType.INBUILT_OUTLINE);
        EventManager.INSTANCE.callEvent(event);

        if (event.getDirtyFlag()) {
            OutlineFramebufferHolder.setDirty(true);
        }
        Pools.MatStack.recycle(matrixStack);
    }

    @ModifyExpressionValue(method = "method_62218", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/ColorHelper;fromFloats(FFFF)I"))
    private int customFogClearColor(int original) {
        return ModuleCustomAmbience.FogConfigurable.INSTANCE.modifyClearColor(original);
    }

    // this method is a lambda
    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V"))
    private void onDrawOutlines(GpuBufferSlice gpuBufferSlice, WorldRenderState worldRenderState, Profiler profiler, Matrix4f matrix4f, Handle handle, Handle handle2, boolean bl, Frustum frustum, Handle handle3, Handle handle4, CallbackInfo ci) {
        OutlineFramebufferHolder.drawIfDirty(this.client.getFramebuffer());
    }

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V", shift = At.Shift.BEFORE))
    private void onRenderGlow(GpuBufferSlice gpuBufferSlice, WorldRenderState worldRenderState, Profiler profiler, Matrix4f matrix4f, Handle handle, Handle handle2, boolean bl, Frustum frustum, Handle handle3, Handle handle4, CallbackInfo ci) {
        var entityOutlineFb = getEntityOutlinesFramebuffer();
        if (!this.canDrawEntityOutlines() || entityOutlineFb == null) {
            return;
        }

        var matrixStack = Pools.MatStack.borrow();
        entityOutlineFb.blitToScreen();
        final var mc = MinecraftClient.getInstance();
        final var camera = mc.gameRenderer.getCamera();
        var event = new DrawOutlinesEvent(
            entityOutlineFb, matrixStack,
            camera, mc.getRenderTickCounter().getTickProgress(false),
            DrawOutlinesEvent.OutlineType.MINECRAFT_GLOW
        );
        EventManager.INSTANCE.callEvent(event);
        OutlineFlag.drawOutline |= event.getDirtyFlag();
        Pools.MatStack.recycle(matrixStack);
    }


    // TODO(1.21.10-port): fix this too lol
//    @ModifyVariable(method = "render", at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/client/gl/ShaderLoader;loadPostEffect(Lnet/minecraft/util/Identifier;Ljava/util/Set;)Lnet/minecraft/client/gl/PostEffectProcessor;"
//    ), name = "bl3", ordinal = 3) // boolean bl3 = this.capturedFrustum != null
//    private boolean modifyDrawOutline(boolean original) {
//        var flag = OutlineFlag.drawOutline;
//        if (flag) {
//            OutlineFlag.drawOutline = false;
//        }
//        return original || flag;
//    }

    // TODO(1.21.10-port): fix renderSetupTerrainModifyArg
//    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"), index = 3)
//    private boolean renderSetupTerrainModifyArg(boolean spectator) {
//        return ModuleFreeCam.INSTANCE.getRunning() || spectator;
//    }

    @Inject(method = "renderTargetBlockOutline", at = @At("HEAD"), cancellable = true)
    private void cancelBlockOutline(VertexConsumerProvider.Immediate immediate, MatrixStack matrices, boolean renderBlockOutline, WorldRenderState renderStates, CallbackInfo ci) {
        if (ModuleBlockOutline.INSTANCE.getRunning()) {
            ci.cancel();
        }
    }

}
