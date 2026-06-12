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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleNoPitchLimit;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleAntiBounce;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoPose;
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.slime.NoSlowSlime;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow
    public boolean noPhysics;

    @Shadow
    public abstract boolean onGround();

    @Shadow
    public abstract boolean isPassenger();

    @Shadow
    public abstract Level level();

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract double getZ();

    @Shadow public abstract float getYRot();

    @Unique
    protected boolean liquid_bounce$isClientPlayer() {
        return (Object) this == Minecraft.getInstance().player;
    }

    @ModifyExpressionValue(method = "isSuppressingBounce", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isShiftKeyDown()Z"))
    private boolean hookAntiBounce(boolean original) {
        return ModuleAntiBounce.INSTANCE.getRunning() || original;
    }

    @ModifyExpressionValue(method = "restituteMovementAfterCollisions", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(DD)D", remap = false))
    private double hookSlimeBounce(double original, @Local(argsOnly = true, name = "effectState") BlockState effectState) {
        // TODO(26.2): Re-check the old exact Y-velocity conditions after vanilla moved slime bounce into Entity restitution.
        if (NoSlowSlime.INSTANCE.getRunning() && effectState.getBlock() instanceof SlimeBlock) {
            return 0.0;
        }

        return original;
    }

    /**
     * Hook entity margin modification event
     */
    @Inject(method = "getPickRadius", at = @At("RETURN"), cancellable = true)
    private void hookMargin(CallbackInfoReturnable<Float> callback) {
        EntityMarginEvent marginEvent = new EntityMarginEvent((Entity) (Object) this, callback.getReturnValue());
        EventManager.INSTANCE.callEvent(marginEvent);
        callback.setReturnValue(marginEvent.getMargin());
    }

    /**
     * Hook no pitch limit exploit
     */
    @WrapOperation(method = {"turn", "absSnapRotationTo"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F"))
    public float hookNoPitchLimit1(float value, float min, float max, Operation<Float> original) {
        boolean noLimit = ModuleNoPitchLimit.INSTANCE.getRunning();
        return noLimit ? value : original.call(value, min, max);
    }

    @WrapOperation(method = "setXRot", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;xRot:F", opcode = Opcodes.PUTFIELD))
    public void hookNoPitchLimit2(Entity instance, float clamped, Operation<Void> original, @Local(argsOnly = true, name = "xRot") float xRot) {
        boolean noLimit = ModuleNoPitchLimit.INSTANCE.getRunning();
        original.call(instance, noLimit ? xRot : clamped);
    }

    @ModifyExpressionValue(method = "moveRelative", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"))
    public Vec3 hookVelocity(Vec3 original, @Local(argsOnly = true, name = "input") Vec3 input, @Local(argsOnly = true, name = "speed") float speed) {
        if (!liquid_bounce$isClientPlayer()) {
            return original;
        }

        var event = new PlayerVelocityStrafe(input, speed, this.getYRot(), original);
        EventManager.INSTANCE.callEvent(event);
        return event.getVelocity();
    }

    @ModifyExpressionValue(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;maxUpStep()F"))
    private float hookStepHeight(float original) {
        if (!liquid_bounce$isClientPlayer()) {
            return original;
        }

        var stepEvent = new PlayerStepEvent(original);
        EventManager.INSTANCE.callEvent(stepEvent);
        return stepEvent.getHeight();
    }

    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At(value = "RETURN", ordinal = 0), cancellable = true)
    private void hookStepHeight(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if (liquid_bounce$isClientPlayer()) {
            PlayerStepSuccessEvent movementCollisionsEvent = new PlayerStepSuccessEvent(movement, cir.getReturnValue());
            EventManager.INSTANCE.callEvent(movementCollisionsEvent);
            cir.setReturnValue(movementCollisionsEvent.getAdjustedVec());
        }
    }

    @ModifyReturnValue(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"))
    private Vec3 hookFreeCamModifiedRaycast(Vec3 original) {
        return ModuleFreeCam.INSTANCE.modifyRaycast(original, (Entity) (Object) this, 1.0F);
    }

    @ModifyReturnValue(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"))
    private Vec3 hookFreeCamModifiedRaycast(Vec3 original, float tickDelta) {
        return ModuleFreeCam.INSTANCE.modifyRaycast(original, (Entity) (Object) this, tickDelta);
    }

    /**
     * When modules that modify player's velocity are enabled while on a vehicle, the game essentially gets screwed up, making the player unable to move.
     * <p>
     * With this injection, the issue is solved.
     */
    @Inject(method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
    private void hookVelocityDuringRidingPrevention(Vec3 velocity, CallbackInfo ci) {
        if (!liquid_bounce$isClientPlayer()) {
            return;
        }

        if (isPassenger()) {
            ci.cancel();
        }
    }

    @Inject(method = "isEyeInFluid", at = @At("HEAD"), cancellable = true)
    private void hookIsSubmergedIn(TagKey<Fluid> fluidTag, CallbackInfoReturnable<Boolean> cir) {
        if (liquid_bounce$isClientPlayer()) {
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
    @ModifyExpressionValue(method = "isLocalInstanceAuthoritative", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isClientAuthoritative()Z"))
    private boolean fixFallDistanceCalculation(boolean original) {
        if (liquid_bounce$isClientPlayer()) {
            return false;
        }

        return original;
    }

    @Inject(method = "setPose", at = @At("HEAD"), cancellable = true)
    private void setPose(Pose pose, CallbackInfo ci) {
        /* Cancel pose if needed */
        if (liquid_bounce$isClientPlayer() && ModuleNoPose.INSTANCE.shouldCancelPose(pose))
            ci.cancel();
    }
}
