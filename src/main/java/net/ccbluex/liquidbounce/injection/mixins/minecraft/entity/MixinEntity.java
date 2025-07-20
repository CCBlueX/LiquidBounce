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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleNoPitchLimit;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleAntiBounce;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoPush;
import net.ccbluex.liquidbounce.features.module.modules.movement.NoPushBy;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
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

    @Shadow public abstract float getYaw();

    @ModifyExpressionValue(method = "bypassesLandingEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSneaking()Z"))
    private boolean hookAntiBounce(boolean original) {
        return ModuleAntiBounce.INSTANCE.getRunning() || original;
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

    @ModifyExpressionValue(method = "updateMovementInFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/FluidState;getVelocity(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookNoPushInLiquids(Vec3d original) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return original;
        }

        return ModuleNoPush.canPush(NoPushBy.LIQUIDS)
                ? original : Vec3d.ZERO;
    }

    /**
     * Hook no pitch limit exploit
     */
    @Redirect(method = "changeLookDirection", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(FFF)F"))
    public float hookNoPitchLimit(float value, float min, float max) {
        boolean noLimit = ModuleNoPitchLimit.INSTANCE.getRunning();

        if (noLimit) return value;
        return MathHelper.clamp(value, min, max);
    }

    @ModifyExpressionValue(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d hookVelocity(Vec3d original, @Local(argsOnly = true) Vec3d movementInput, @Local(argsOnly = true) float speed, @Local(argsOnly = true) float yaw) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return original;
        }

        var event = new PlayerVelocityStrafe(movementInput, speed, yaw, original);
        EventManager.INSTANCE.callEvent(event);
        return event.getVelocity();
    }

    @ModifyExpressionValue(method = "adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getStepHeight()F"))
    private float hookStepHeight(float original) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return original;
        }

        var stepEvent = new PlayerStepEvent(original);
        EventManager.INSTANCE.callEvent(stepEvent);
        return stepEvent.getHeight();
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

    @ModifyReturnValue(method = "getCameraPosVec", at = @At("RETURN"))
    private Vec3d hookFreeCamModifiedRaycast(Vec3d original, float tickDelta) {
        return ModuleFreeCam.INSTANCE.modifyRaycast(original, (Entity) (Object) this, tickDelta);
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

    @Inject(method = "updateMovementInFluid", at = @At("HEAD"), cancellable = true)
    private void hookFluidMovement(TagKey<Fluid> tag, double speed, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            var event = EventManager.INSTANCE.callEvent(new PlayerFluidCollisionCheckEvent(tag));

            if (event.isCancelled()) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "isSubmergedIn", at = @At("HEAD"), cancellable = true)
    private void hookIsSubmergedIn(TagKey<Fluid> fluidTag, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            var event = EventManager.INSTANCE.callEvent(new PlayerFluidCollisionCheckEvent(fluidTag));

            if (event.isCancelled()) {
                cir.setReturnValue(false);
            }
        }
    }

    /**
     * Restores client-side fall distance calculation that was disabled
     * after Minecraft 1.21.4 (or 1.21.3, I don't know)
     * <p>
     * The vanilla game stopped calculating fall distance on the client side due to
     * PlayerEntity always returning true for isControlledByPlayer(). This modification
     * enables fall distance calculation by returning false when the entity is
     * the client's player instance.
     * <p>
     * Because we don't know if this might also break something else, when we would overwrite
     * the function to always return false, we only return false on fall distance calculation.
     *
     * @return false if the entity is the client's player, otherwise returns the original value
     */
    @ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z"))
    private boolean fixFallDistanceCalculation(boolean original) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            return false;
        }

        return original;
    }

}
