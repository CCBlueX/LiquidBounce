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
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.collection.Pools;
import net.ccbluex.liquidbounce.utils.render.WorldToScreen;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.objectweb.asm.Opcodes;
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
    private Lightmap lightmap;

    @Shadow
    @Final
    private RenderTarget mainRenderTarget;

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
        DeltaTracker deltaTracker,
        float worldPartialTicks,
        float cameraEntityPartialTicks,
        CallbackInfo ci,
        @Local(name = "cameraState") CameraRenderState cameraState
    ) {
        WorldToScreen.setMatrices(cameraState.projectionMatrix, cameraState.viewRotationMatrix, cameraState.pos);
    }

    /**
     * Hook world render event
     */
    @Inject(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/CameraEntityRenderState;isSleeping:Z", opcode = Opcodes.GETFIELD))
    public void hookWorldRender(
        DeltaTracker deltaTracker,
        CallbackInfo ci,
        @Local(name = "projectionMatrix") Matrix4f projectionMatrix,
        @Local(name = "modelViewMatrix") Matrix4fc modelViewMatrix
    ) {
        var newMatStack = Pools.MatStack.borrow();
        try {
            newMatStack.mulPose(modelViewMatrix);
            try (var event = new WorldRenderEvent(
                newMatStack,
                this.mainCamera,
                deltaTracker.getGameTimeDeltaPartialTick(false),
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
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;getBuffer(Lnet/minecraft/client/renderer/fog/FogRenderer$FogMode;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;")
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
            target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lnet/minecraft/client/renderer/SubmitNodeStorage;)V"
        )
    )
    private void drawItemCharmsOnHandFeatureExecution(
        net.minecraft.client.renderer.feature.FeatureRenderDispatcher instance,
        net.minecraft.client.renderer.SubmitNodeStorage submitNodeStorage,
        Operation<Void> original
    ) {
        ModuleItemChams.Lightmap.INSTANCE.applyToTexture(this.lightmap.getTextureView());
        try {
            original.call(instance, submitNodeStorage);
        } finally {
            ModuleItemChams.Lightmap.INSTANCE.resetTexture(this.lightmap.getTextureView());
        }
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
     * @see net.minecraft.client.renderer.GameRenderer#bobView(net.minecraft.client.renderer.state.level.CameraRenderState, com.mojang.blaze3d.vertex.PoseStack)
     * @see net.minecraft.client.Camera#extractRenderState(net.minecraft.client.renderer.state.level.CameraRenderState, float)
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
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(h * Mth.PI) * h * (3.0F + additionalBobbing)));
        poseStack.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(h * Mth.PI - (0.2F + additionalBobbing)) * h) * 5.0F));

        ci.cancel();
    }

    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void hookShowFloatingItem(ItemStack floatingItem, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.FLOATING_ITEMS)) {
            ci.cancel();
        }
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
