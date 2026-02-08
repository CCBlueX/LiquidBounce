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
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.GameRenderEvent;
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent;
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent;
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleDroneControl;
import net.ccbluex.liquidbounce.features.module.modules.fun.ModuleDankBobbing;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.ccbluex.liquidbounce.utils.collection.Pools;
import net.ccbluex.liquidbounce.utils.render.WorldToScreen;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public abstract Minecraft getMinecraft();

    @Shadow
    @Final
    private Camera mainCamera;

    @Shadow
    public abstract void tick();

    @Shadow
    @Final
    private LightTexture lightTexture;

    /**
     * Hook game render event
     */
    @Inject(method = "render", at = @At("HEAD"))
    public void hookGameRender(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(GameRenderEvent.INSTANCE);
    }

    /**
     * Hook world render event
     */
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"))
    public void hookWorldRender(
        DeltaTracker deltaTracker,
        CallbackInfo ci,
        @Local(ordinal = 0) Matrix4f projectionMatrix,
        @Local(ordinal = 1) Matrix4f modelViewMatrix
    ) {
        WorldToScreen.setMatrices(projectionMatrix, modelViewMatrix);

        var newMatStack = Pools.MatStack.borrow();
        newMatStack.mulPose(modelViewMatrix);
        EventManager.INSTANCE.callEvent(new WorldRenderEvent(newMatStack, this.mainCamera, deltaTracker.getGameTimeDeltaPartialTick(false)));
        Pools.MatStack.recycle(newMatStack);
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

    @WrapOperation(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V"))
    public void drawItemCharms(ItemInHandRenderer instance, float tickProgress, PoseStack matrices,
        SubmitNodeCollector orderedRenderCommandQueue, LocalPlayer player, int light,
        Operation<Void> original) {
        ModuleItemChams.INSTANCE.applyToTexture(this.lightTexture.getTextureView());
        original.call(instance, tickProgress, matrices, orderedRenderCommandQueue, player, light);
        ModuleItemChams.INSTANCE.resetTexture(this.lightTexture.getTextureView());
    }

    /**
     * Hook screen render event
     */
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            shift = At.Shift.AFTER))
    public void hookScreenRender(DeltaTracker tickCounter, boolean tick, CallbackInfo ci, @Local GuiGraphics drawContext) {
        EventManager.INSTANCE.callEvent(new ScreenRenderEvent(drawContext, tickCounter.getGameTimeDeltaPartialTick(false)));
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void injectHurtCam(PoseStack matrixStack, float f, CallbackInfo callbackInfo) {
        if (ModuleNoHurtCam.INSTANCE.getRunning()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void injectBobView(PoseStack matrixStack, float tickProgress, CallbackInfo callbackInfo) {
        if (ModuleNoBob.INSTANCE.getRunning() ||
            ModuleTracers.INSTANCE.getRunning() ||
            (ModuleItemESP.INSTANCE.getRunning() && ModuleItemESP.INSTANCE.getShowTracers())) {

            callbackInfo.cancel();
            return;
        }

        if (!ModuleDankBobbing.INSTANCE.getRunning()) {
            return;
        }

        if (!(minecraft.getCameraEntity() instanceof AbstractClientPlayer playerEntity)) {
            return;
        }

        float additionalBobbing = ModuleDankBobbing.INSTANCE.getMotion();

        final var state = playerEntity.avatarState();

        float g = state.getBackwardsInterpolatedWalkDistance(tickProgress);
        float h = state.getInterpolatedBob(tickProgress);
        matrixStack.translate(Mth.sin(g * Mth.PI) * h * 0.5f, -Math.abs(Mth.cos(g * Mth.PI) * h), 0.0f);
        matrixStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(h * Mth.PI) * h * (3.0F + additionalBobbing)));
        matrixStack.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(h * Mth.PI - (0.2F + additionalBobbing)) * h) * 5.0F));

        callbackInfo.cancel();
    }

    @Inject(method = "resize", at = @At("HEAD"))
    private void hookBlurEffectResize(int width, int height, CallbackInfo ci) {
    }

    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void hookShowFloatingItem(ItemStack floatingItem, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.FLOATING_ITEMS)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "getFov", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", remap = false))
    private int hookGetFov(int original) {
        int result;

        if (ModuleZoom.INSTANCE.getRunning()) {
            return ModuleZoom.INSTANCE.getFov(true, 0);
        } else {
            result = ModuleZoom.INSTANCE.getFov(false, original);
        }

        if (ModuleNoFov.INSTANCE.getRunning() && result == original) {
            return ModuleNoFov.INSTANCE.getFov(result);
        }

        return result;
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", ordinal = 0, remap = false))
    private float hookAntiNausea(float original) {
        if (!ModuleAntiBlind.canRender(DoRender.NAUSEA)) {
            return 0f;
        }

        return original;
    }

    @ModifyExpressionValue(method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;"
            )
    )
    private CameraType hookPerspectiveEventOnCamera(CameraType original) {
        return EventManager.INSTANCE.callEvent(new PerspectiveEvent(original)).getPerspective();
    }

    @ModifyExpressionValue(method = "renderItemInHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;"
            )
    )
    private CameraType hookPerspectiveEventOnHand(CameraType original) {
        return EventManager.INSTANCE.callEvent(new PerspectiveEvent(original)).getPerspective();
    }

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float injectShit(float original) {
        var screen = ModuleDroneControl.INSTANCE.getScreen();

        if (screen != null) {
            return Math.min(120f, original / screen.getZoomFactor());
        }

        return original;
    }

    @ModifyArgs(method = "getProjectionMatrix", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;", remap = false))
    private void hookBasicProjectionMatrix(Args args) {
        if (ModuleAspect.INSTANCE.getRunning()) {
            args.set(1, (float) args.get(1) / ModuleAspect.getRatioMultiplier());
        }
    }

}
