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
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleDroneControl;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.ccbluex.liquidbounce.features.module.modules.render.cameraclip.ModuleCameraClip;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow
    private boolean detached;
    @Shadow
    private float yRot;
    @Shadow
    private float xRot;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract float getMaxZoom(float f);

    @Shadow
    protected abstract void move(float f, float g, float h);

    @Shadow
    public abstract void setPosition(Vec3 pos);

    @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER))
    private void hookFreeCamModifiedPosition(Level area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
        float tickProgress, CallbackInfo ci) {
        ModuleFreeCam.INSTANCE.applyCameraPosition(focusedEntity, tickProgress);
    }

    @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER), cancellable = true)
    private void modifyCameraOrientation(Level area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
        float tickProgress, CallbackInfo ci) {
        var freeLook = ModuleFreeLook.INSTANCE.getRunning();
        var freeLockInvertedView = ModuleFreeLook.INSTANCE.isInvertedView();
        var qps = ModuleQuickPerspectiveSwap.INSTANCE.getRunning();
        var rearView = qps && ModuleQuickPerspectiveSwap.INSTANCE.getRearView() && !freeLook && !thirdPerson;

        if (freeLook || qps) {
            if (!rearView) this.detached = true;

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
                setRotation(yRot + 180.0f, freeLook && !freeLockInvertedView ? xRot : -xRot);
            }

            float scale = focusedEntity instanceof LivingEntity livingEntity ? livingEntity.getScale() : 1.0F;
            float desiredCameraDistance = ModuleCameraClip.INSTANCE.getRunning() ? ModuleCameraClip.INSTANCE.getDistance() : 4f;

            if (!rearView) {
                move(-getMaxZoom(desiredCameraDistance * scale), 0.0f, 0.0f);
            }

            ci.cancel();
            return;
        }
        var screen = ModuleDroneControl.INSTANCE.getScreen();

        if (screen != null) {
            this.setPosition(screen.getCameraPos());
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
            Mth.lerp(tickProgress, previousRotation.yRot(), currentRotation.yRot()),
            Mth.lerp(tickProgress, previousRotation.xRot(), currentRotation.xRot())
        );
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void applyFreeCamPlayerSelfRendering(Level area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        if (ModuleFreeCam.INSTANCE.getRunning()) {
            this.detached = true;
        }
    }

    @ModifyConstant(method = "getMaxZoom", constant = @Constant(intValue = 8))
    private int hookCameraClip(int constant) {
        return ModuleCameraClip.INSTANCE.getRunning() ? 0 : constant;
    }

    @ModifyExpressionValue(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
    private float modifyDesiredCameraDistance(float original) {
        return ModuleCameraClip.INSTANCE.getRunning() ? getMaxZoom(ModuleCameraClip.INSTANCE.getDistance()) : original;
    }

    @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 modifyPositionVehicle(Vec3 instance, Vec3 vec) {
        if (ModuleFreeLook.INSTANCE.getRunning()) {
            return vec;
        }

        return ModuleSmoothCamera.shouldApplyChanges() ? vec.add(0, 1, 0) : vec;
    }

    @ModifyArgs(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void modifyPosition(Args args) {
        if (ModuleFreeLook.INSTANCE.getRunning()) {
            return;
        }

        Vec3 original = new Vec3(args.get(0), args.get(1), args.get(2));
        ModuleSmoothCamera.cameraUpdate(original);
        if (ModuleSmoothCamera.shouldApplyChanges()) {
            Vec3 smoothPos = ModuleSmoothCamera.INSTANCE.getSmoothPos();
            args.set(0, smoothPos.x);
            args.set(1, smoothPos.y);
            args.set(2, smoothPos.z);
        }
    }
}

