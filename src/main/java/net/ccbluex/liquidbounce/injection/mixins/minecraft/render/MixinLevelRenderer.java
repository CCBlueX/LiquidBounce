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
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.DrawOutlinesEvent;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.ccbluex.liquidbounce.utils.collection.Pools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.jspecify.annotations.Nullable;
import org.joml.Vector4fc;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {

    @Shadow
    @Nullable
    public abstract RenderTarget entityOutlineTarget();

    @Unique
    private boolean liquid_bounce$hasCustomOutlineMesh = false;

    // TODO: removed because of vanilla changes
//    // After ModelViewMatrix setup
//    @Inject(method = "render", at = @At(value = "NEW", target = "Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;"))
//    private void onRender(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
//        OutlineShaderRenderer renderer = OutlineShaderRenderer.INSTANCE;
//        if (!renderer.shouldRender()) {
//            return;
//        }
//
//        var matrixStack = Pools.MatStack.borrow();
//        var event = new DrawOutlinesEvent(
//            renderer.prepareRenderTarget(),
//            matrixStack,
//            cameraState,
//            deltaTracker.getGameTimeDeltaPartialTick(false),
//            DrawOutlinesEvent.OutlineType.INBUILT_OUTLINE
//        );
//        EventManager.INSTANCE.callEvent(event);
//        Pools.MatStack.recycle(matrixStack);
//
//        if (event.getDirtyFlag()) {
//            renderer.setDirty(true);
//        }
//    }

    @ModifyArg(
        method = "lambda$render$0",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;Lorg/joml/Vector4fc;Lcom/mojang/blaze3d/textures/GpuTexture;D)V"),
        index = 1
    )
    private Vector4fc customFogClearColor(Vector4fc original) {
        return ModuleCustomAmbience.FogValueGroup.INSTANCE.modifyClearColor(original);
    }

//    @Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeOutline()V", shift = At.Shift.AFTER))
//    private void onDrawOutlines(CallbackInfo ci) {
//        OutlineShaderRenderer.INSTANCE.drawBlitIfDirty(Minecraft.getInstance().gameRenderer.mainRenderTarget());
//    }

    @Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeOutline()V", shift = At.Shift.BEFORE))
    private void onRenderGlow(CallbackInfo ci) {
        var minecraft = Minecraft.getInstance();
        var entityOutlineFb = entityOutlineTarget();
        if (entityOutlineFb == null
            || !minecraft.gameRenderer.gameRenderState().levelRenderState.shouldShowEntityOutlines) {
            return;
        }

        liquid_bounce$hasCustomOutlineMesh = false;
        var matrixStack = Pools.MatStack.borrow();
        var event = new DrawOutlinesEvent(
            entityOutlineFb, matrixStack,
            minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false)
        );
        EventManager.INSTANCE.callEvent(event);
        liquid_bounce$hasCustomOutlineMesh = event.getDirtyFlag();
        Pools.MatStack.recycle(matrixStack);
    }

    @Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeSolid()V", shift = At.Shift.BEFORE))
    private void prepareChamsRenderTarget(CallbackInfo ci) {
        ModuleChams.INSTANCE.beginFrameIfNeeded();
    }

    @Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeTranslucentAfterTerrain()V", shift = At.Shift.AFTER))
    private void blitChams(CallbackInfo ci) {
        ModuleChams.INSTANCE.compositeIfNeeded(Minecraft.getInstance().gameRenderer.mainRenderTarget());
    }

    @ModifyExpressionValue(
        method = {"submitFeatures", "lambda$addMainPass$0"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/LevelRenderState;shouldShowEntityOutlines:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean includeCustomOutlines(boolean original) {
        return original || liquid_bounce$hasCustomOutlineMesh;
    }

    @ModifyExpressionValue(
        method = {"render", "addMainPass"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;hasAnyOutline()Z")
    )
    private boolean includeCustomOutlineTargetInMainPass(boolean original) {
        return original || liquid_bounce$hasCustomOutlineMesh;
    }

    @Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true)
    private void cancelBlockOutline(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (ModuleBlockOutline.INSTANCE.getRunning()) {
            ci.cancel();
        }
    }

    @WrapWithCondition(method = "submitBlockDestroyAnimation", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitBreakingBlockModel(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;I)V"))
    private boolean cancelRenderBreakingTexture(SubmitNodeCollector instance, PoseStack poseStack, List<?> list, int i) {
        return ModuleAntiBlind.canRender(DoRender.BLOCK_BREAK_OVERLAY);
    }

}
