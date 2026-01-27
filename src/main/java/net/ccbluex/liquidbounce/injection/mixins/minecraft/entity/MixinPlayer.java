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
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent;
import net.ccbluex.liquidbounce.features.command.commands.ingame.fakeplayer.FakePlayer;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleKeepSprint;
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes.CriticalsNoGround;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleAntiReducedDebugInfo;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoClip;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReach;
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallNoGround;
import net.ccbluex.liquidbounce.features.module.modules.render.hitfx.ModuleHitFX;
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleNoSlowBreak;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayer extends MixinLivingEntity {

    @Shadow
    public abstract void tick();

    @Shadow
    public abstract SoundSource getSoundSource();

    /**
     * Hook safe walk event
     */
    @ModifyReturnValue(method = "isStayingOnGroundSurface", at = @At("RETURN"))
    private boolean hookSafeWalk(boolean original) {
        final var event = EventManager.INSTANCE.callEvent(new PlayerSafeWalkEvent());
        return original || event.isSafeWalk();
    }

    /**
     * Hook velocity rotation modification
     * <p>
     * There are a few velocity changes when attacking an entity, which could be easily detected by anti-cheats when a different server-side rotation is applied.
     */
    @ModifyExpressionValue(method = {"causeExtraKnockback",
        "doSweepAttack"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    private float hookFixRotation(float original) {
        if ((Object) this != Minecraft.getInstance().player) {
            return original;
        }

        var rotationManager = RotationManager.INSTANCE;
        var rotation = rotationManager.getCurrentRotation();
        var rotationTarget = rotationManager.getActiveRotationTarget();

        if (rotationTarget == null || rotationTarget.getMovementCorrection() == MovementCorrection.OFF || rotation == null) {
            return original;
        }

        return rotation.yRot();
    }

    @Inject(method = "isReducedDebugInfo", at = @At("HEAD"), cancellable = true)
    private void injectReducedDebugInfo(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (ModuleAntiReducedDebugInfo.INSTANCE.getRunning()) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z",
            ordinal = 1,
            shift = At.Shift.BEFORE))
    private void hookNoClip(CallbackInfo ci) {
        var clip = ModuleNoClip.INSTANCE;
        if (!this.noPhysics && clip.getRunning() && !clip.paused()) {
            this.noPhysics = true;
        }
    }

    @ModifyExpressionValue(method = "getDestroySpeed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean injectFatigueNoSlow(boolean original) {
        if ((Object) this == Minecraft.getInstance().player && ModuleNoSlowBreak.getMiningFatigue()) {
            return false;
        }

        return original;
    }


    @ModifyExpressionValue(method = "getDestroySpeed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean injectWaterNoSlow(boolean original) {
        if ((Object) this == Minecraft.getInstance().player && ModuleNoSlowBreak.getWater()) {
            return false;
        }

        return original;
    }

    @ModifyExpressionValue(method = "getDestroySpeed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;onGround()Z"))
    private boolean injectOnAirNoSlow(boolean original) {
        if ((Object) this == Minecraft.getInstance().player) {
            if (ModuleNoSlowBreak.getOnAir()) {
                return true;
            }

            if (NoFallNoGround.INSTANCE.getRunning()) {
                return false;
            }

            if (CriticalsNoGround.INSTANCE.getRunning()) {
                return false;
            }
        }

        return original;
    }

    @SuppressWarnings("ConstantValue")
    @Redirect(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 hookSlowVelocity(Vec3 instance, double x, double y, double z) {
        if ((Object) this == Minecraft.getInstance().player && ModuleKeepSprint.INSTANCE.getRunning()) {
            x = z = ModuleKeepSprint.INSTANCE.getMotion();
        }

        return instance.multiply(x, y, z);
    }

    /**
     * for: attack, pierce
     */
    @WrapWithCondition(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V", ordinal = 0))
    private boolean hookSlowVelocity(Player instance, boolean b) {
        if ((Object) this == Minecraft.getInstance().player) {
            ModuleKeepSprint.INSTANCE.setSprinting(b);
            return !ModuleKeepSprint.INSTANCE.getRunning() || b;
        }

        return true;
    }

    @SuppressWarnings({"UnreachableCode", "ConstantValue"})
    @ModifyExpressionValue(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSprinting()Z"))
    private boolean hookSlowVelocity(boolean original) {
        if ((Object) this == Minecraft.getInstance().player && ModuleKeepSprint.INSTANCE.getRunning()) {
            return ModuleKeepSprint.INSTANCE.getSprinting();
        }

        return original;
    }

    @ModifyReturnValue(method = "entityInteractionRange", at = @At("RETURN"))
    private double hookEntityInteractionRange(double original) {
        if ((Object) this == Minecraft.getInstance().player && ModuleReach.INSTANCE.getRunning()) {
            return ModuleReach.INSTANCE.getEntity().getInteractionRange$liquidbounce();
        }

        return original;
    }

    @ModifyReturnValue(method = "blockInteractionRange", at = @At("RETURN"))
    private double hookBlockInteractionRange(double original) {
        if ((Object) this == Minecraft.getInstance().player && ModuleReach.INSTANCE.getRunning()) {
            return ModuleReach.INSTANCE.getBlockRangeIncrease() + original;
        }

        return original;
    }

    @ModifyExpressionValue(method = "getCurrentItemAttackStrengthDelay", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double hookAutoWeaponAttackSpeed(double original) {
        if ((Object) this == Minecraft.getInstance().player && ModuleReach.INSTANCE.getRunning()) {
            return original;
        }

        return ModuleAutoWeapon.INSTANCE.getAttackSpeed(original);
    }

    /*
     * Sadly, mixins don't allow capturing parameters when redirecting,
     * so there needs to be an extra injection for every sound.
     */
    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;playServerSideSound(Lnet/minecraft/sounds/SoundEvent;)V", ordinal = 0))
    private void hookPlaySound(Entity target, CallbackInfo ci) {
        if (!ModuleHitFX.INSTANCE.getRunning()) {
            liquid_bounce$playSoundIfFakePlayer(target, SoundEvents.PLAYER_ATTACK_KNOCKBACK);
        }
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;playServerSideSound(Lnet/minecraft/sounds/SoundEvent;)V", ordinal = 1))
    private void hookPlaySound1(Entity target, CallbackInfo ci) {
        if (!ModuleHitFX.INSTANCE.getRunning()) {
            liquid_bounce$playSoundIfFakePlayer(target, SoundEvents.PLAYER_ATTACK_NODAMAGE);
        }
    }

    @Inject(method = "attackVisualEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;playServerSideSound(Lnet/minecraft/sounds/SoundEvent;)V", ordinal = 0))
    private void hookPlaySound2(Entity target, boolean criticalHit, boolean sweeping, boolean cooldownPassed, boolean pierce, float enchantDamage, CallbackInfo ci) {
        if (!ModuleHitFX.INSTANCE.getRunning()) {
            liquid_bounce$playSoundIfFakePlayer(target, SoundEvents.PLAYER_ATTACK_CRIT);
        }

    }

    @Inject(method = "attackVisualEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;playServerSideSound(Lnet/minecraft/sounds/SoundEvent;)V", ordinal = 1))
    private void hookPlaySound3(Entity target, boolean criticalHit, boolean sweeping, boolean cooldownPassed, boolean pierce, float enchantDamage, CallbackInfo ci) {
        if(!ModuleHitFX.INSTANCE.getRunning()) {
            liquid_bounce$playSoundIfFakePlayer(target, cooldownPassed ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK);
        }
    }

    @Inject(method = "doSweepAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;playServerSideSound(Lnet/minecraft/sounds/SoundEvent;)V", ordinal = 0))
    private void hookPlaySound4(Entity target, float damage, DamageSource damageSource, float cooldownProgress, CallbackInfo ci) {
        if(!ModuleHitFX.INSTANCE.getRunning()) {
            liquid_bounce$playSoundIfFakePlayer(target, SoundEvents.PLAYER_ATTACK_SWEEP);
        }
    }

    /**
     * When the target is a fake player, this method will play a client side sound.
     */
    @Unique
    private void liquid_bounce$playSoundIfFakePlayer(Entity target, SoundEvent soundEvent) {
        if (target instanceof FakePlayer) {
            level().playSound(Player.class.cast(this), getX(), getY(), getZ(), soundEvent, getSoundSource(), 1F, 1F);
        }
    }
}
