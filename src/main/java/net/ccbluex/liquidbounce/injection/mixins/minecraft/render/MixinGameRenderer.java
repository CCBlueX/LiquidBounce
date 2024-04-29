/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import com.mojang.blaze3d.systems.RenderSystem;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.GameRenderEvent;
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent;
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent;
import net.ccbluex.liquidbounce.features.module.modules.fun.ModuleDankBobbing;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReach;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoBob;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoHurtCam;
import net.ccbluex.liquidbounce.interfaces.PostEffectPassTextureAddition;
import net.ccbluex.liquidbounce.render.engine.UIRenderer;
import net.ccbluex.liquidbounce.utils.aiming.RaytracingExtensionsKt;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract MinecraftClient getClient();

    @Shadow
    @Final
    private ResourceManager resourceManager;
    /**
     * UI Blur Post Effect Processor
     *
     * @author superblaubeere27
     */
    @Unique
    private PostEffectProcessor blurPostEffectProcessor;
    @Shadow
    @Final
    private Camera camera;

    /**
     * Hook game render event
     */
    @Inject(method = "render", at = @At("HEAD"))
    public void hookGameRender(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new GameRenderEvent());
    }

    /**
     * We change crossHairTarget according to server side rotations
     */
    @Redirect(method = "findCrosshairTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;raycast(DFZ)Lnet/minecraft/util/hit/HitResult;"))
    private HitResult hookRaycast(Entity instance, double maxDistance, float tickDelta, boolean includeFluids) {
        if (instance != client.player) {
            return instance.raycast(maxDistance, tickDelta, includeFluids);
        }

        var rotation = (RotationManager.INSTANCE.getCurrentRotation() != null) ?
                RotationManager.INSTANCE.getCurrentRotation() :
                ModuleFreeCam.INSTANCE.getEnabled() ?
                        RotationManager.INSTANCE.getServerRotation() :
                        new Rotation(instance.getYaw(tickDelta), instance.getPitch(tickDelta));

        return RaytracingExtensionsKt.raycast(maxDistance, rotation, includeFluids, tickDelta);
    }

    @Redirect(method = "findCrosshairTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getRotationVec(F)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookRotationVector(Entity instance, float tickDelta) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();

        return rotation != null ? rotation.getRotationVec() : instance.getRotationVec(tickDelta);
    }

    /**
     * Hook world render event
     */
    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD)
    public void hookWorldRender(float tickDelta, long limitTime, CallbackInfo ci, boolean bl, Camera camera, Entity entity, double d, Matrix4f matrix4f, MatrixStack matrixStack, float f, float g, Matrix4f matrix4f2) {
        // TODO: Improve this
        var newMatStack = new MatrixStack();

        newMatStack.multiplyPositionMatrix(matrix4f2);

        EventManager.INSTANCE.callEvent(new WorldRenderEvent(newMatStack, this.camera, tickDelta));
    }

    /**
     * Hook screen render event
     */
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            shift = At.Shift.AFTER))
    public void hookScreenRender(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ScreenRenderEvent());
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void injectHurtCam(MatrixStack matrixStack, float f, CallbackInfo callbackInfo) {
        if (ModuleNoHurtCam.INSTANCE.getEnabled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void injectBobView(MatrixStack matrixStack, float f, CallbackInfo callbackInfo) {
        if (ModuleNoBob.INSTANCE.getEnabled()) {
            callbackInfo.cancel();
            return;
        }

        if (!ModuleDankBobbing.INSTANCE.getEnabled()) {
            return;
        }

        if (!(client.getCameraEntity() instanceof PlayerEntity playerEntity)) {
            return;
        }

        float additionalBobbing = ModuleDankBobbing.INSTANCE.getMotion();

        float g = playerEntity.horizontalSpeed - playerEntity.prevHorizontalSpeed;
        float h = -(playerEntity.horizontalSpeed + g * f);
        float i = MathHelper.lerp(f, playerEntity.prevStrideDistance, playerEntity.strideDistance);
        matrixStack.translate((MathHelper.sin(h * MathHelper.PI) * i * 0.5F), -Math.abs(MathHelper.cos(h * MathHelper.PI) * i), 0.0D);
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(h * MathHelper.PI) * i * (3.0F + additionalBobbing)));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Math.abs(MathHelper.cos(h * MathHelper.PI - (0.2F + additionalBobbing)) * i) * 5.0F));

        callbackInfo.cancel();
    }

    @Inject(method = "onResized", at = @At("HEAD"))
    private void injectResizeUIBlurShader(int width, int height, CallbackInfo ci) {
        if (this.blurPostEffectProcessor != null) {
            this.blurPostEffectProcessor.setupDimensions(width, height);
        }

        UIRenderer.INSTANCE.setupDimensions(width, height);
    }

    @Inject(method = "loadPrograms", at = @At("TAIL"))
    private void hookUIBlurLoad(final CallbackInfo ci) {
        if (this.blurPostEffectProcessor == null) {
            try {
                var identifier = new Identifier("liquidbounce", "shaders/post/ui_blur.json");

                this.blurPostEffectProcessor = new PostEffectProcessor(this.client.getTextureManager(), this.resourceManager,
                        this.client.getFramebuffer(), identifier);
                this.blurPostEffectProcessor.setupDimensions(this.client.getWindow().getFramebufferWidth(),
                        this.client.getWindow().getFramebufferHeight());
            } catch (final Exception e) {
                LiquidBounce.INSTANCE.getLogger().error("Failed to load UI blur shader", e);
            }
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;drawEntityOutlinesFramebuffer()V", shift = At.Shift.AFTER))
    private void injectUIBlurRender(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        if (!ModuleHud.INSTANCE.isBlurable() || this.blurPostEffectProcessor == null) {
            return;
        }

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();

        var overlayFramebuffer = UIRenderer.INSTANCE.getOverlayFramebuffer();
        var overlayTexture = overlayFramebuffer.getColorAttachment();

        overlayFramebuffer.beginRead();

        RenderSystem.setShaderTexture(0, overlayTexture);
        ((PostEffectPassTextureAddition) this.blurPostEffectProcessor.passes.get(0)).liquid_bounce$setTextureSampler("Overlay", overlayTexture);
        this.blurPostEffectProcessor.passes.get(0).getProgram().getUniformByName("Radius").set(UIRenderer.INSTANCE.getBlurRadius());

        this.blurPostEffectProcessor.render(tickDelta);
    }

    @Inject(method = "render", at = @At(value = "RETURN"))
    private void hookRenderEventStop(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        UIRenderer.INSTANCE.endUIOverlayDrawing();
    }

}
