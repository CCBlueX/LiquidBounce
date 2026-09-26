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
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.GameRenderEvent;
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent;
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent;
import net.ccbluex.liquidbounce.features.module.modules.fun.ModuleDankBobbing;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.ccbluex.liquidbounce.features.module.modules.render.customambience.ModuleCustomAmbience;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.collection.Pools;
import net.ccbluex.liquidbounce.utils.render.WorldToScreen;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private Camera mainCamera;

    @Shadow
    public abstract void tick();

    @Shadow
    @Final
    private RenderTarget mainRenderTarget;

    @Shadow
    @Final
    private Lightmap lightmap;

    /**
     * Hook game render event
     */
    @Inject(method = "render", at = @At("HEAD"))
    public void hookGameRender(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(GameRenderEvent.INSTANCE);
    }

    /**
     * Apply change-look rotations before vanilla updates and extracts the camera state.
     */
    @Inject(method = "update", at = @At("HEAD"))
    private void applyChangeLookRotation(DeltaTracker deltaTracker, CallbackInfo ci) {
        RotationManager.INSTANCE.applyChangeLookRotation(deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    @Inject(method = "extractCamera", at = @At("TAIL"))
    private void hookWorldToScreenMatricesInExtract(
        DeltaTracker deltaTracker, float worldPartialTicks, CallbackInfo ci, @Local(name = "cameraState") CameraRenderState cameraState
    ) {
        WorldToScreen.setMatrices(cameraState.projectionMatrix, cameraState.viewRotationMatrix, cameraState.pos);
    }

    /**
     * Hook world render event after the level has been rendered and before the 3D HUD is drawn.
     */
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZZ)V",
            shift = At.Shift.AFTER
        )
    )
    private void hookWorldRender(
        CallbackInfo ci, @Local(name = "cameraState") CameraRenderState cameraState,
        @Local(name = "worldPartialTicks") float worldPartialTicks
    ) {
        var newMatStack = Pools.MatStack.borrow();
        try {
            newMatStack.mulPose(cameraState.viewRotationMatrix);
            try (var event = new WorldRenderEvent(
                newMatStack,
                this.mainCamera,
                worldPartialTicks,
                this.mainRenderTarget
            )) {
                EventManager.INSTANCE.callEvent(event);
            }
        } finally {
            Pools.MatStack.recycle(newMatStack);
        }
    }

    @ModifyArg(
        method = "renderLevel",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;getBuffer(Lnet/minecraft/client/renderer/fog/FogRenderer$FogMode;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;")
    )
    private FogRenderer.FogMode disableFog(FogRenderer.FogMode fogMode) {
        var fogValueGroup = ModuleCustomAmbience.FogValueGroup.INSTANCE;
        if (fogValueGroup.getRunning() && ModuleCustomAmbience.FogValueGroup.INSTANCE.getDisableWorldFog()) {
            return FogRenderer.FogMode.NONE;
        }
        return fogMode;
    }

    @WrapOperation(
        method = "renderItemInHand",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;prepareFrame(Lnet/minecraft/client/renderer/SubmitNodeStorage;)Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;"
        )
    )
    private FeatureRenderDispatcher.PreparedFrame drawItemCharmsOnHandPrepareFrame(
        FeatureRenderDispatcher instance, SubmitNodeStorage submitNodeStorage,
        Operation<FeatureRenderDispatcher.PreparedFrame> original
    ) {
        return ModuleItemChams.Lightmap.doOverride(() -> original.call(instance, submitNodeStorage));
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Lightmap;render(Lnet/minecraft/client/renderer/state/LightmapRenderState;)V",
            shift = At.Shift.AFTER
        )
    )
    private void hookItemChamsLightmapRefresh(CallbackInfo ci) {
        ModuleItemChams.Lightmap.refresh(this.lightmap.getTextureView());
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void injectHurtCam(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (ModuleNoHurtCam.INSTANCE.getRunning()) {
            ci.cancel();
        }
    }

    /**
     * Keeps the vanilla 26.1 walk interpolation inputs while applying the custom bobbing strength.
     *
     * {@code GameRenderer#bobView(CameraRenderState, PoseStack)} is private, so it cannot be referenced via {@code @see}.
     *
     * @see net.minecraft.client.Camera#extractRenderState(net.minecraft.client.renderer.state.level.CameraRenderState, net.minecraft.client.DeltaTracker)
     * @see net.minecraft.client.renderer.state.level.CameraEntityRenderState#backwardsInterpolatedWalkDistance
     * @see net.minecraft.client.renderer.state.level.CameraEntityRenderState#bob
     */
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void injectBobView(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (ModuleNoBob.INSTANCE.getRunning() ||
            ModuleTracers.INSTANCE.getRunning() ||
            (ModuleItemESP.INSTANCE.getRunning() && ModuleItemESP.INSTANCE.getShowTracers()) ||
            ModuleStorageESP.INSTANCE.showTracers()) {

            ci.cancel();
            return;
        }

        if (!ModuleDankBobbing.INSTANCE.getRunning()) {
            return;
        }

        final var entityRenderState = cameraState.entityRenderState;
        if (!entityRenderState.isPlayer) {
            return;
        }

        float additionalBobbing = ModuleDankBobbing.INSTANCE.getMotion();
        float g = entityRenderState.backwardsInterpolatedWalkDistance;
        float h = entityRenderState.bob;
        poseStack.translate(Mth.sin(g * Mth.PI) * h * 0.5f, -Math.abs(Mth.cos(g * Mth.PI) * h), 0.0f);
        poseStack.rotate(Axis.ZP.rotationDegrees(Mth.sin(h * Mth.PI) * h * (3.0F + additionalBobbing)));
        poseStack.rotate(Axis.XP.rotationDegrees(Math.abs(Mth.cos(h * Mth.PI - (0.2F + additionalBobbing)) * h) * 5.0F));

        ci.cancel();
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", ordinal = 0, remap = false))
    private float hookAntiNausea(float original) {
        if (!ModuleAntiBlind.canRender(DoRender.NAUSEA)) {
            return 0f;
        }

        return original;
    }

    @ModifyExpressionValue(method = "extractOptions",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;"
            )
    )
    private CameraType hookPerspectiveEventOnCamera(CameraType original) {
        return PerspectiveEvent.INSTANCE.getPerspective();
    }

}
