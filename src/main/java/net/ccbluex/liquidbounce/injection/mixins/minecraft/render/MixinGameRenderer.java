/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.event.events.GameRenderEvent;
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent;
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent;
import net.ccbluex.liquidbounce.features.module.modules.fun.ModuleDankBobbing;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReach;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoBob;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoHurtCam;
import net.ccbluex.liquidbounce.interfaces.IMixinGameRenderer;
import net.ccbluex.liquidbounce.utils.aiming.RaytracingExtensionsKt;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
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
    protected abstract void bobView(MatrixStack matrixStack, float f);

    @Shadow
    public abstract Matrix4f getBasicProjectionMatrix(double d);

    @Shadow
    protected abstract double getFov(Camera camera, float tickDelta, boolean changingFov);

    @Shadow
    protected abstract void tiltViewWhenHurt(MatrixStack matrices, float tickDelta);

    @Shadow
    public abstract MinecraftClient getClient();

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
    @Redirect(method = "updateTargetedEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;raycast(DFZ)Lnet/minecraft/util/hit/HitResult;"))
    private HitResult hookRaycast(Entity instance, double maxDistance, float tickDelta, boolean includeFluids) {
        if (instance != client.player) return instance.raycast(maxDistance, tickDelta, includeFluids);

        Rotation rotation = (RotationManager.INSTANCE.getCurrentRotation() != null) ?
                RotationManager.INSTANCE.getCurrentRotation() :
                ModuleFreeCam.INSTANCE.getEnabled() ?
                        RotationManager.INSTANCE.getServerRotation() :
                        new Rotation(instance.getYaw(tickDelta), instance.getPitch(tickDelta));

        return RaytracingExtensionsKt.raycast(maxDistance, rotation, includeFluids);
    }

    @Redirect(method = "updateTargetedEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getRotationVec(F)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookRotationVector(Entity instance, float tickDelta) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();

        return rotation != null ? rotation.getRotationVec() : instance.getRotationVec(tickDelta);
    }

    /**
     * Hook world render event
     */
    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    public void hookWorldRender(float partialTicks, long finishTimeNano, MatrixStack matrixStack, CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new WorldRenderEvent(matrixStack, partialTicks));
    }

    /**
     * Hook screen render event
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    public void hookScreenRender(Screen screen, DrawContext context, int mouseX, int mouseY, float delta) {
        screen.render(context, mouseX, mouseY, delta);
        EventManager.INSTANCE.callEvent(new ScreenRenderEvent(screen, context, mouseX, mouseY, delta));
    }

    @Override
    public Matrix4f getCameraMVPMatrix(float tickDelta, boolean bobbing) {
        MatrixStack matrices = new MatrixStack();

        double fov = getFov(camera, tickDelta, true);
        matrices.multiplyPositionMatrix(getBasicProjectionMatrix(fov));

        if (bobbing) {
            tiltViewWhenHurt(matrices, tickDelta);

            if (client.options.getBobView().getValue()) {
                bobView(matrices, tickDelta);
            }

            float f = MathHelper.lerp(tickDelta, client.player.prevNauseaIntensity, client.player.nauseaIntensity) * client.options.getDistortionEffectScale().getValue().floatValue() * client.options.getDistortionEffectScale().getValue().floatValue();
            if (f > 0.0F) {
                int i = client.player.hasStatusEffect(StatusEffects.NAUSEA) ? 7 : 20;
                float g = 5.0F / (f * f + 5.0F) - f * 0.04F;
                g *= g;

                RotationAxis vec3f = RotationAxis.of(new Vector3f(0.0F, MathHelper.SQUARE_ROOT_OF_TWO / 2.0F, MathHelper.SQUARE_ROOT_OF_TWO / 2.0F));
                matrices.multiply(vec3f.rotationDegrees((ticks + tickDelta) * i));
                matrices.scale(1.0F / g, 1.0F, 1.0F);
                float h = -(ticks + tickDelta) * i;
                matrices.multiply(vec3f.rotationDegrees(h));
            }
        }

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));

        Vec3d cameraPosition = camera.getPos();

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        matrix4f.mul(new Matrix4f().translate((float) -cameraPosition.x, (float) -cameraPosition.y, (float) -cameraPosition.z));
        return matrix4f;
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

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void hookFreeCamDisableHandRender(MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        if (ModuleFreeCam.INSTANCE.shouldDisableHandRender()) {
            ci.cancel();
        }
    }

    @ModifyConstant(method = "updateTargetedEntity", constant = @Constant(doubleValue = 9.0))
    private double hookReachModifyCombatReach(double constant) {
        return ModuleReach.INSTANCE.getEnabled() ? (ModuleReach.INSTANCE.getCombatReach() * ModuleReach.INSTANCE.getCombatReach()) : constant;
    }

    @Inject(method = "updateTargetedEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getRotationVec(F)Lnet/minecraft/util/math/Vec3d;"))
    private void hookReachModifyBlockReach(float tickDelta, CallbackInfo ci) {
        if (ModuleReach.INSTANCE.getEnabled()) {
            client.crosshairTarget = client.player.raycast(ModuleReach.INSTANCE.getBlockReach(), tickDelta, false);
        }
    }
}
