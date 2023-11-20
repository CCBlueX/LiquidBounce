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

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCameraClip;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleQuickPerspectiveSwap;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleRotations;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
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
    protected abstract void moveBy(double x, double y, double z);

    @Shadow
    protected abstract double clipToSpace(double desiredCameraDistance);

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift = At.Shift.AFTER))
    private void injectQuickPerspectiveSwap(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (ModuleQuickPerspectiveSwap.INSTANCE.getEnabled()) {
            this.thirdPerson = true;

            this.setRotation(this.yaw + 180.0f, -this.pitch);

            this.moveBy(-this.clipToSpace(4.0), 0.0, 0.0);
            return;
        }

        RotationsConfigurable configurable = RotationManager.INSTANCE.getActiveConfigurable();

        var previousRotation = RotationManager.INSTANCE.getPreviousRotation();
        var currentRotation = RotationManager.INSTANCE.getCurrentRotation();

        boolean shouldModifyRotation = ModuleRotations.INSTANCE.getEnabled() && ModuleRotations.INSTANCE.getPov() || configurable != null && !configurable.getSilent();

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
        ModuleFreeCam.INSTANCE.applyPosition(focusedEntity, tickDelta);
    }

    @ModifyConstant(method = "clipToSpace", constant = @Constant(intValue = 8))
    private int hookCameraClip(int constant) {
        return ModuleCameraClip.INSTANCE.getEnabled() ? 0 : constant;
    }
}
