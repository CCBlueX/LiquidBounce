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

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCameraClip;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleQuickPerspectiveSwap;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleRotations;
import net.ccbluex.liquidbounce.utils.aiming.AimPlan;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
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
    protected abstract void moveBy(double x, double y, double z);

    @Shadow
    protected abstract double clipToSpace(double desiredCameraDistance);

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift = At.Shift.AFTER))
    private void injectQuickPerspectiveSwap(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (ModuleQuickPerspectiveSwap.INSTANCE.getEnabled()) {
            this.thirdPerson = true;

            this.setRotation(this.yaw + 180.0f, -this.pitch);

            var desiredCameraDistance = ModuleCameraClip.INSTANCE.getEnabled() ? ModuleCameraClip.INSTANCE.getDistance() : 4.0;
            this.moveBy(-this.clipToSpace(desiredCameraDistance), 0.0, 0.0);
            return;
        }

        AimPlan aimPlan = RotationManager.INSTANCE.getStoredAimPlan();

        var previousRotation = RotationManager.INSTANCE.getPreviousRotation();
        var currentRotation = RotationManager.INSTANCE.getCurrentRotation();

        boolean shouldModifyRotation = ModuleRotations.INSTANCE.getEnabled() && ModuleRotations.INSTANCE.getPov()
                || aimPlan != null && aimPlan.getChangeLook();

        if (currentRotation == null || previousRotation == null || !shouldModifyRotation) {
            return;
        }

        this.setRotation(
                MathHelper.lerp(tickDelta, previousRotation.getYaw(), currentRotation.getYaw()),
                MathHelper.lerp(tickDelta, previousRotation.getPitch(), currentRotation.getPitch())
        );
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift = At.Shift.AFTER))
    private void hookFreeCamModifiedPosition(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        ModuleFreeCam.INSTANCE.applyCameraPosition(focusedEntity, tickDelta);
    }

    @ModifyConstant(method = "clipToSpace", constant = @Constant(intValue = 8))
    private int hookCameraClip(int constant) {
        return ModuleCameraClip.INSTANCE.getEnabled() ? 0 : constant;
    }


    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(D)D"))
    private double modifyDesiredCameraDistance(Camera instance, double desiredCameraDistance) {
        var param = ModuleCameraClip.INSTANCE.getEnabled() ? ModuleCameraClip.INSTANCE.getDistance() : desiredCameraDistance;

        return this.clipToSpace(param);
    }
}
