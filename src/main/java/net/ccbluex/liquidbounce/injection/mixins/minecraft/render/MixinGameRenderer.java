/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.GameRenderEvent;
import net.ccbluex.liquidbounce.event.ScreenRenderEvent;
import net.ccbluex.liquidbounce.features.module.modules.fun.ModuleDankBobbing;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoBob;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoHurtCam;
import net.ccbluex.liquidbounce.interfaces.IMixinGameRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements IMixinGameRenderer {

    @Shadow
    @Final
    private Camera camera;
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    private int ticks;

    @Shadow
    protected abstract void bobViewWhenHurt(MatrixStack matrixStack, float f);

    @Shadow
    protected abstract void bobView(MatrixStack matrixStack, float f);

    @Shadow
    public abstract Matrix4f getBasicProjectionMatrix(double d);

    @Shadow
    protected abstract double getFov(Camera camera, float tickDelta, boolean changingFov);

    /**
     * Hook game render event
     */
    @Inject(method = "render", at = @At("HEAD"))
    public void hookGameRender(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new GameRenderEvent());
    }

    /**
     * Hook screen render event
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/util/math/MatrixStack;IIF)V"))
    public void hookScreenRender(Screen screen, MatrixStack matrices, int mouseX, int mouseY, float delta) {
        screen.render(matrices, mouseX, mouseY, delta);
        EventManager.INSTANCE.callEvent(new ScreenRenderEvent(screen, matrices, mouseX, mouseY, delta));
    }

    @Override
    public Matrix4f getCameraMVPMatrix(float tickDelta, boolean bobbing) {
        MatrixStack matrixStack = new MatrixStack();

        matrixStack.peek().getPositionMatrix().mul(this.getBasicProjectionMatrix(this.getFov(camera, tickDelta, true)));

        if (bobbing) {
            this.bobViewWhenHurt(matrixStack, tickDelta);

            if (this.client.options.getBobView().getValue()) {
                this.bobView(matrixStack, tickDelta);
            }

            float f = MathHelper.lerp(tickDelta, this.client.player.lastNauseaStrength, this.client.player.nextNauseaStrength) * this.client.options.getDistortionEffectScale().getValue().floatValue() * this.client.options.getDistortionEffectScale().getValue().floatValue();
            if (f > 0.0F) {
                int i = this.client.player.hasStatusEffect(StatusEffects.NAUSEA) ? 7 : 20;
                float g = 5.0F / (f * f + 5.0F) - f * 0.04F;
                g *= g;

                RotationAxis vec3f = RotationAxis.of(new Vector3f(0.0F, MathHelper.SQUARE_ROOT_OF_TWO / 2.0F, MathHelper.SQUARE_ROOT_OF_TWO / 2.0F));
                matrixStack.multiply(vec3f.rotationDegrees(((float) this.ticks + tickDelta) * (float) i));
                matrixStack.scale(1.0F / g, 1.0F, 1.0F);
                float h = -((float) this.ticks + tickDelta) * (float) i;
                matrixStack.multiply(vec3f.rotationDegrees(h));
            }
        }

        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

        Vec3d pos = this.camera.getPos();

        Matrix4f model = matrixStack.peek().getPositionMatrix();

        // todo: what
        // model.mul(Matrix4f.translate(-(float) pos.x, -(float) pos.y, -(float) pos.z));

        return model;
    }

    @Inject(method = "bobViewWhenHurt", at = @At("HEAD"), cancellable = true)
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

        if (!(this.client.getCameraEntity() instanceof PlayerEntity playerEntity)) {
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

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void hookFreeCamDisableHandRender(MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        if (ModuleFreeCam.INSTANCE.shouldDisableHandRender()) {
            ci.cancel();
        }
    }
}
