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

import net.ccbluex.liquidbounce.config.NoneChoice;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent;
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleAirJump;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleAntiLevitation;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoJumpDelay;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoPush;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleRotations;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.utils.aiming.AimPlan;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity {

    @Shadow
    public boolean jumping;

    @Shadow
    public int jumpingCooldown;

    @Shadow
    public abstract float getJumpVelocity();

    @Shadow
    protected abstract void jump();

    @Shadow
    public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> effect);

    @Shadow
    public abstract void tick();

    @Shadow public abstract void swingHand(Hand hand, boolean fromServerPlayer);

    /**
     * Hook anti levitation module
     */
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"))
    public boolean hookTravelStatusEffect(LivingEntity instance, RegistryEntry<StatusEffect> effect) {
        if ((effect == StatusEffects.LEVITATION || effect == StatusEffects.SLOW_FALLING) && ModuleAntiLevitation.INSTANCE.getEnabled()) {
            if (instance.hasStatusEffect(effect)) {
                instance.fallDistance = 0f;
            }

            return false;
        }

        return instance.hasStatusEffect(effect);
    }

    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
    private void hookAntiNausea(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect == StatusEffects.NAUSEA && ModuleAntiBlind.INSTANCE.getEnabled() && ModuleAntiBlind.INSTANCE.getAntiNausea()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Redirect(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getJumpVelocity()F"))
    private float hookJumpEvent(LivingEntity instance) {
        if (instance != MinecraftClient.getInstance().player) {
            return instance.getJumpVelocity();
        }

        final PlayerJumpEvent jumpEvent = new PlayerJumpEvent(getJumpVelocity());
        EventManager.INSTANCE.callEvent(jumpEvent);
        return jumpEvent.getMotion();
    }

    @Inject(method = "jump", at = @At("RETURN"))
    private void hookAfterJumpEvent(CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return;
        }

        EventManager.INSTANCE.callEvent(new PlayerAfterJumpEvent());
    }

    /**
     * Hook velocity rotation modification
     * <p>
     * Jump according to modified rotation. Prevents detection by movement sensitive anticheats.
     */
    @Redirect(method = "jump", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookFixRotation(double x, double y, double z) {
        RotationManager rotationManager = RotationManager.INSTANCE;
        Rotation rotation = rotationManager.getCurrentRotation();
        AimPlan configurable = rotationManager.getStoredAimPlan();

        if ((Object) this != MinecraftClient.getInstance().player) {
            return new Vec3d(x, y, z);
        }

        if (configurable == null || !configurable.getApplyVelocityFix() || rotation == null) {
            return new Vec3d(x, y, z);
        }

        float yaw = rotation.getYaw() * 0.017453292F;

        return new Vec3d(-MathHelper.sin(yaw) * 0.2F, 0.0, MathHelper.cos(yaw) * 0.2F);
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void hookNoPush(CallbackInfo callbackInfo) {
        if (ModuleNoPush.INSTANCE.getEnabled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void hookTickMovement(CallbackInfo callbackInfo) {
        // We don't want NoJumpDelay to interfere with AirJump which would lead to a Jetpack-like behavior
        var noJumpDelay = ModuleNoJumpDelay.INSTANCE.getEnabled() && !ModuleAirJump.INSTANCE.getAllowJump();

        // The jumping cooldown would lead to very slow tower building
        var towerActive = ModuleScaffold.INSTANCE.getEnabled() && !(ModuleScaffold.INSTANCE.getTowerMode()
                .getActiveChoice() instanceof NoneChoice) && ModuleScaffold.INSTANCE.getTowerMode()
                .getActiveChoice().isActive();

        if (noJumpDelay || towerActive) {
            jumpingCooldown = 0;
        }
    }

    @Inject(method = "tickMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;jumping:Z"))
    private void hookAirJump(CallbackInfo callbackInfo) {
        if (ModuleAirJump.INSTANCE.getAllowJump() && jumping && jumpingCooldown == 0) {
            this.jump();
            jumpingCooldown = 10;
        }
    }

    /**
     * Body rotation yaw injection hook
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"), slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F", ordinal = 1)))
    private float hookBodyRotationsA(LivingEntity instance) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return instance.getYaw();
        }

        ModuleRotations rotations = ModuleRotations.INSTANCE;
        Rotation rotation = rotations.displayRotations();

        return rotations.shouldDisplayRotations() ? rotation.getYaw() : instance.getYaw();
    }

    /**
     * Body rotation yaw injection hook
     */
    @Redirect(method = "turnHead", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float hookBodyRotationsB(LivingEntity instance) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return instance.getYaw();
        }

        ModuleRotations rotations = ModuleRotations.INSTANCE;
        Rotation rotation = rotations.displayRotations();

        return rotations.shouldDisplayRotations() ? rotation.getYaw() : instance.getYaw();
    }

    /**
     * Fall flying using modified-rotation
     */
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"))
    private float hookModifyFallFlyingPitch(LivingEntity instance) {
        RotationManager rotationManager = RotationManager.INSTANCE;
        Rotation rotation = rotationManager.getCurrentRotation();
        AimPlan configurable = rotationManager.getStoredAimPlan();

        if (instance != MinecraftClient.getInstance().player || rotation == null || configurable == null || !configurable.getApplyVelocityFix() || configurable.getChangeLook()) {
            return instance.getPitch();
        }

        return rotation.getPitch();
    }

    /**
     * Fall flying using modified-rotation
     */
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookModifyFallFlyingRotationVector(LivingEntity instance) {
        RotationManager rotationManager = RotationManager.INSTANCE;
        Rotation rotation = rotationManager.getCurrentRotation();
        AimPlan configurable = rotationManager.getStoredAimPlan();

        if (instance != MinecraftClient.getInstance().player || rotation == null || configurable == null || !configurable.getApplyVelocityFix() || configurable.getChangeLook()) {
            return instance.getRotationVector();
        }

        return rotation.getRotationVec();
    }

}
