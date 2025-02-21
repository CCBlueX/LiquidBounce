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
import net.ccbluex.liquidbounce.config.types.NoneChoice;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent;
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.*;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
    public abstract void jump();

    @Shadow
    public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> effect);

    @Shadow
    public abstract void tick();

    @Shadow public abstract void swingHand(Hand hand, boolean fromServerPlayer);

    @Shadow
    public abstract void setHealth(float health);


    @Shadow
    public abstract boolean isGliding();

    /**
     * Disable [StatusEffects.LEVITATION] effect when [ModuleAntiLevitation] is enabled
     */
    @ModifyExpressionValue(
            method = "travelMidAir",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;getStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Lnet/minecraft/entity/effect/StatusEffectInstance;",
                    ordinal = 0
            ),
            require = 1,
            allow = 1
    )
    public StatusEffectInstance hookTravelStatusEffect(StatusEffectInstance original) {
        // If we get anyting other than levitation, the injection went wrong
        assert original != StatusEffects.LEVITATION;

        if (ModuleAntiLevitation.INSTANCE.getRunning()) {
            return null;
        }

        return original;
    }

    /**
     * Disable [StatusEffects.SLOW_FALLING] effect when [ModuleAntiLevitation] is enabled
     */
    @ModifyExpressionValue(
            method = "getEffectiveGravity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z",
                    ordinal = 0
            ),
            require = 1,
            allow = 1
    )
    public boolean hookTravelStatusEffect(boolean original) {
        if (ModuleAntiLevitation.INSTANCE.getRunning()) {
            return false;
        }

        return original;
    }

    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
    private void hookAntiNausea(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect == StatusEffects.NAUSEA && ModuleAntiBlind.INSTANCE.getRunning() && ModuleAntiBlind.INSTANCE.getAntiNausea()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void hookJumpEvent(CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return;
        }

        final PlayerJumpEvent jumpEvent = new PlayerJumpEvent(getJumpVelocity());
        EventManager.INSTANCE.callEvent(jumpEvent);
        if (jumpEvent.isCancelled()) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getJumpVelocity()F"))
    private float hookJumpEvent(float original) {
        if (((Object) this) != MinecraftClient.getInstance().player) {
            return original;
        }

        final var jumpEvent = EventManager.INSTANCE.callEvent(new PlayerJumpEvent(original));
        return jumpEvent.getMotion();
    }

    @Inject(method = "jump", at = @At("RETURN"))
    private void hookAfterJumpEvent(CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return;
        }

        EventManager.INSTANCE.callEvent(PlayerAfterJumpEvent.INSTANCE);
    }

    /**
     * Hook velocity rotation modification
     * <p>
     * Jump according to modified rotation. Prevents detection by movement sensitive anticheats.
     */
    @ModifyExpressionValue(method = "jump", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookFixRotation(Vec3d original) {
        var rotationManager = RotationManager.INSTANCE;
        var rotation = rotationManager.getCurrentRotation();
        var configurable = rotationManager.getActiveRotationTarget();

        if ((Object) this != MinecraftClient.getInstance().player) {
            return original;
        }

        if (configurable == null || configurable.getMovementCorrection() == MovementCorrection.OFF || rotation == null) {
            return original;
        }

        float yaw = rotation.getYaw() * 0.017453292F;

        return new Vec3d(-MathHelper.sin(yaw) * 0.2F, 0.0, MathHelper.cos(yaw) * 0.2F);
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void hookNoPush(CallbackInfo callbackInfo) {
        if (ModuleNoPush.INSTANCE.isEntities()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void hookTickMovement(CallbackInfo callbackInfo) {
        // We don't want NoJumpDelay to interfere with AirJump which would lead to a Jetpack-like behavior
        var noJumpDelay = ModuleNoJumpDelay.INSTANCE.getRunning() && !ModuleAirJump.INSTANCE.getAllowJump();

        // The jumping cooldown would lead to very slow tower building
        var towerActive = ModuleScaffold.INSTANCE.getRunning() &&
        !(ModuleScaffold.INSTANCE.getTowerMode().getActiveChoice() instanceof NoneChoice) &&
        ModuleScaffold.INSTANCE.getTowerMode().getActiveChoice().getRunning();

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

    @Unique
    private boolean previousElytra = false;

    @Inject(method = "tickGliding", at = @At("TAIL"))
    public void recastIfLanded(CallbackInfo callbackInfo) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return;
        }

        var elytra = isGliding();
        if (ModuleElytraRecast.INSTANCE.getRunning() && previousElytra && !elytra) {
            MinecraftClient.getInstance().getSoundManager().stopSounds(SoundEvents.ITEM_ELYTRA_FLYING.id(),
                    SoundCategory.PLAYERS);
            ModuleElytraRecast.INSTANCE.recastElytra();
            jumpingCooldown = 0;
        }

        previousElytra = elytra;
    }

    /**
     * Gliding using modified-rotation
     */
    @ModifyExpressionValue(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"))
    private float hookModifyFallFlyingPitch(float original) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return original;
        }

        var rotationManager = RotationManager.INSTANCE;
        var rotation = rotationManager.getCurrentRotation();
        var configurable = rotationManager.getActiveRotationTarget();

        if (rotation == null || configurable == null || configurable.getMovementCorrection() == MovementCorrection.OFF) {
            return original;
        }

        return rotation.getPitch();
    }

    /**
     * Gliding using modified-rotation
     */
    @ModifyExpressionValue(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookModifyFallFlyingRotationVector(Vec3d original) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return original;
        }

        var rotationManager = RotationManager.INSTANCE;
        var rotation = rotationManager.getCurrentRotation();
        var configurable = rotationManager.getActiveRotationTarget();

        if (rotation == null || configurable == null || configurable.getMovementCorrection() == MovementCorrection.OFF) {
            return original;
        }

        return rotation.getDirectionVector();
    }

}
