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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import net.ccbluex.liquidbounce.event.EntityMarginEvent;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.PlayerStepEvent;
import net.ccbluex.liquidbounce.event.PlayerVelocityStrafe;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleNoPitchLimit;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.tag.TagKey;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow
    public static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        return null;
    }

    @Shadow
    public abstract Vec3d getVelocity();

    @Shadow
    public abstract void setVelocity(Vec3d velocity);

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract double getZ();

    @Shadow
    public abstract float getYaw();

    @Shadow
    public boolean noClip;

    @Shadow
    public abstract boolean isOnGround();

    @Shadow
    public abstract boolean isSubmergedIn(TagKey<Fluid> fluidTag);

    /**
     * Hook entity margin modification event
     */
    @Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true)
    private void hookMargin(CallbackInfoReturnable<Float> callback) {
        final EntityMarginEvent marginEvent = new EntityMarginEvent((Entity) (Object) this, callback.getReturnValue());
        EventManager.INSTANCE.callEvent(marginEvent);
        callback.setReturnValue(marginEvent.getMargin());
    }

    /**
     * Hook no pitch limit exploit
     */
    @Redirect(method = "changeLookDirection", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(FFF)F"))
    public float hookNoPitchLimit(float value, float min, float max) {
        final boolean noLimit = ModuleNoPitchLimit.INSTANCE.getEnabled();

        if (noLimit) return value;
        return MathHelper.clamp(value, min, max);
    }

    @Redirect(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d hookVelocity(Vec3d movementInput, float speed, float yaw) {
        //noinspection ConstantConditions
        if ((Object) this == MinecraftClient.getInstance().player) {
            final PlayerVelocityStrafe event = new PlayerVelocityStrafe(movementInput, speed, yaw, movementInputToVelocity(movementInput, speed, yaw));
            EventManager.INSTANCE.callEvent(event);
            return event.getVelocity();
        }

        return movementInputToVelocity(movementInput, speed, yaw);
    }

    @Redirect(method = "adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;stepHeight:F"))
    private float hookStepHeight(Entity instance) {
        final PlayerStepEvent stepEvent = new PlayerStepEvent(instance.stepHeight);
        EventManager.INSTANCE.callEvent(stepEvent);
        return stepEvent.getHeight();
    }

    @Inject(method = "getCameraPosVec", at = @At("RETURN"), cancellable = true)
    private void hookFreeCamModifiedRaycast(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        cir.setReturnValue(ModuleFreeCam.INSTANCE.modifyRaycast(cir.getReturnValue(), (Entity) (Object) this, tickDelta));
    }
}
