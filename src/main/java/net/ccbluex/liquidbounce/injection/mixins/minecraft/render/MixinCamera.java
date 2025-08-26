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
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleDroneControl;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCameraClip;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeLook;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleQuickPerspectiveSwap;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow
    private boolean thirdPerson;
    @Shadow
    private float yaw;
    @Shadow
    private float pitch;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract float clipToSpace(float f);

    @Shadow
    protected abstract void moveBy(float f, float g, float h);

    @Shadow
    public abstract void setPos(Vec3d pos);

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift = At.Shift.AFTER))
    private void hookFreeCamModifiedPosition(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        ModuleFreeCam.INSTANCE.applyCameraPosition(focusedEntity, tickDelta);
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift = At.Shift.AFTER), cancellable = true)
    private void modifyCameraOrientation(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        var freeLook = ModuleFreeLook.INSTANCE.getRunning();
        var freeLockInvertedView = ModuleFreeLook.INSTANCE.isInvertedView();
        var qps = ModuleQuickPerspectiveSwap.INSTANCE.getRunning();
        var rearView = qps && ModuleQuickPerspectiveSwap.INSTANCE.getRearView() && !freeLook && !thirdPerson;

        if (freeLook || qps) {
            if (!rearView) this.thirdPerson = true;

            if (freeLook) {
                var cameraYaw = ModuleFreeLook.INSTANCE.getCameraYaw();
                var cameraPitch = ModuleFreeLook.INSTANCE.getCameraPitch();

                if (freeLockInvertedView) {
                    setRotation(cameraYaw + 180, -cameraPitch);
                } else {
                    setRotation(cameraYaw, cameraPitch);
                }
            }

            if (qps) {
                setRotation(yaw + 180.0f, freeLook && !freeLockInvertedView ? pitch : -pitch);
            }

            float scale = focusedEntity instanceof LivingEntity livingEntity ? livingEntity.getScale() : 1.0F;
            float desiredCameraDistance = ModuleCameraClip.INSTANCE.getRunning() ? ModuleCameraClip.INSTANCE.getDistance() : 4f;

            if (!rearView) {
                moveBy(-clipToSpace(desiredCameraDistance * scale), 0.0f, 0.0f);
            }

            ci.cancel();
            return;
        }
        var screen = ModuleDroneControl.INSTANCE.getScreen();

        if (screen != null) {
            this.setPos(screen.getCameraPos());
            this.setRotation(screen.getCameraRotation().x, screen.getCameraRotation().y);
        }

        var rotationTarget = RotationManager.INSTANCE.getActiveRotationTarget();
        var previousRotation = RotationManager.INSTANCE.getPreviousRotation();
        var currentRotation = RotationManager.INSTANCE.getCurrentRotation();

        var changeLook = rotationTarget != null &&
                rotationTarget.getMovementCorrection() == MovementCorrection.CHANGE_LOOK;
        if (currentRotation == null || previousRotation == null || !changeLook ||
                !RotationManager.INSTANCE.isRotatingAllowed(rotationTarget)) {
            return;
        }

        setRotation(
            MathHelper.lerp(tickDelta, previousRotation.getYaw(), currentRotation.getYaw()),
            MathHelper.lerp(tickDelta, previousRotation.getPitch(), currentRotation.getPitch())
        );
    }

    @ModifyConstant(method = "clipToSpace", constant = @Constant(intValue = 8))
    private int hookCameraClip(int constant) {
        return ModuleCameraClip.INSTANCE.getRunning() ? 0 : constant;
    }

    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"))
    private float modifyDesiredCameraDistance(float original) {
        return ModuleCameraClip.INSTANCE.getRunning() ? clipToSpace(ModuleCameraClip.INSTANCE.getDistance()) : original;
    }
}
