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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.EntityMarginEvent;
import net.ccbluex.liquidbounce.event.events.PlayerStepEvent;
import net.ccbluex.liquidbounce.event.events.PlayerStepSuccessEvent;
import net.ccbluex.liquidbounce.event.events.PlayerVelocityStrafe;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleNoPitchLimit;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleAntiBounce;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow
    public boolean noClip;

    @Shadow
    public static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        return null;
    }

    @Shadow
    public abstract boolean isOnGround();

    @Shadow
    public abstract boolean hasVehicle();

    @Shadow
    public abstract boolean isPlayer();

    @Shadow
    public abstract World getWorld();

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract double getZ();

    @ModifyExpressionValue(method = "bypassesLandingEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSneaking()Z"))
    private boolean hookAntiBounce(boolean original) {
        return ModuleAntiBounce.INSTANCE.getEnabled() || original;
    }

    /**
     * Hook entity margin modification event
     */
    @Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true)
    private void hookMargin(CallbackInfoReturnable<Float> callback) {
        EntityMarginEvent marginEvent = new EntityMarginEvent((Entity) (Object) this, callback.getReturnValue());
        EventManager.INSTANCE.callEvent(marginEvent);
        callback.setReturnValue(marginEvent.getMargin());
    }

    /**
     * Hook no pitch limit exploit
     */
    @Redirect(method = "changeLookDirection", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(FFF)F"))
    public float hookNoPitchLimit(float value, float min, float max) {
        boolean noLimit = ModuleNoPitchLimit.INSTANCE.getEnabled();

        if (noLimit) return value;
        return MathHelper.clamp(value, min, max);
    }

    @Redirect(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d hookVelocity(Vec3d movementInput, float speed, float yaw) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            PlayerVelocityStrafe event = new PlayerVelocityStrafe(movementInput, speed, yaw, MixinEntity.movementInputToVelocity(movementInput, speed, yaw));
            EventManager.INSTANCE.callEvent(event);
            return event.getVelocity();
        }

        return MixinEntity.movementInputToVelocity(movementInput, speed, yaw);
    }

    @Redirect(method = "adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getStepHeight()F"))
    private float hookStepHeight(Entity instance) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            PlayerStepEvent stepEvent = new PlayerStepEvent(instance.getStepHeight());
            EventManager.INSTANCE.callEvent(stepEvent);
            return stepEvent.getHeight();
        }

        return instance.getStepHeight();
    }

    @Inject(method = "adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",
            at = @At(value = "RETURN", ordinal = 0), cancellable = true)
    private void hookStepHeight(Vec3d movement, CallbackInfoReturnable<Vec3d> cir) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            PlayerStepSuccessEvent movementCollisionsEvent = new PlayerStepSuccessEvent(movement, cir.getReturnValue());
            EventManager.INSTANCE.callEvent(movementCollisionsEvent);
            cir.setReturnValue(movementCollisionsEvent.getAdjustedVec());
        }
    }

    @Inject(method = "getCameraPosVec", at = @At("RETURN"), cancellable = true)
    private void hookFreeCamModifiedRaycast(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        cir.setReturnValue(ModuleFreeCam.INSTANCE.modifyRaycast(cir.getReturnValue(), (Entity) (Object) this, tickDelta));
    }

    /**
     * When modules that modify player's velocity are enabled while on a vehicle, the game essentially gets screwed up, making the player unable to move.
     * <p>
     * With this injection, the issue is solved.
     */
    @Inject(method = "setVelocity(Lnet/minecraft/util/math/Vec3d;)V", at = @At("HEAD"), cancellable = true)
    private void hookVelocityDuringRidingPrevention(Vec3d velocity, CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return;
        }

        if (hasVehicle()) {
            ci.cancel();
        }
    }
}
