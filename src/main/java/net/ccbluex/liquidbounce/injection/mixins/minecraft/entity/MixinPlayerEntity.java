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
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent;
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent;
import net.ccbluex.liquidbounce.event.events.PlayerStrideEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleCriticals;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleKeepSprint;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleAntiReducedDebugInfo;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoClip;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReach;
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall;
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallNoGround;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleRotations;
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleNoSlowBreak;
import net.ccbluex.liquidbounce.utils.aiming.AimPlan;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends MixinLivingEntity {

    @Shadow
    public abstract void tick();

    /**
     * Hook player stride event
     */
    @ModifyVariable(method = "tickMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;strideDistance:F", shift = At.Shift.BEFORE, ordinal = 0), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setMovementSpeed(F)V"), to = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isSpectator()Z")), index = 1, ordinal = 0, require = 1, allow = 1)
    private float hookStrideForce(float strideForce) {
        final PlayerStrideEvent event = new PlayerStrideEvent(strideForce);
        EventManager.INSTANCE.callEvent(event);
        return event.getStrideForce();
    }

    /**
     * Hook safe walk event
     */
    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    private void hookSafeWalk(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        final PlayerSafeWalkEvent event = EventManager.INSTANCE.callEvent(new PlayerSafeWalkEvent());

        if (event.isSafeWalk()) {
            callbackInfoReturnable.setReturnValue(true);
        }
    }

    /**
     * Hook velocity rotation modification
     * <p>
     * There are a few velocity changes when attacking an entity, which could be easily detected by anti-cheats when a different server-side rotation is applied.
     */
    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"))
    private float hookFixRotation(PlayerEntity entity) {
        RotationManager rotationManager = RotationManager.INSTANCE;
        Rotation rotation = rotationManager.getCurrentRotation();
        AimPlan configurable = rotationManager.getStoredAimPlan();

        if (configurable == null || !configurable.getApplyVelocityFix() || rotation == null) {
            return entity.getYaw();
        }

        return rotation.getYaw();
    }

    @Inject(method = "hasReducedDebugInfo", at = @At("HEAD"), cancellable = true)
    private void injectReducedDebugInfo(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (ModuleAntiReducedDebugInfo.INSTANCE.getEnabled()) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;isSpectator()Z",
            ordinal = 1,
            shift = At.Shift.BEFORE))
    private void hookNoClip(CallbackInfo ci) {
        if (!this.noClip && ModuleNoClip.INSTANCE.getEnabled()) {
            this.noClip = true;
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

    @ModifyExpressionValue(method = "getBlockBreakingSpeed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"))
    private boolean injectFatigueNoSlow(boolean original) {
        ModuleNoSlowBreak module = ModuleNoSlowBreak.INSTANCE;
        if ((Object) this == MinecraftClient.getInstance().player && module.getEnabled() && module.getMiningFatigue()) {
            return false;
        }

        return original;
    }


    @ModifyExpressionValue(method = "getBlockBreakingSpeed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;isSubmergedIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
    private boolean injectWaterNoSlow(boolean original) {
        ModuleNoSlowBreak module = ModuleNoSlowBreak.INSTANCE;
        if ((Object) this == MinecraftClient.getInstance().player && module.getEnabled() && module.getWater()) {
            return false;
        }

        return original;
    }

    @Redirect(method = "getBlockBreakingSpeed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;isOnGround()Z"))
    private boolean injectOnAirNoSlow(PlayerEntity instance) {
        if ((Object) this == MinecraftClient.getInstance().player) {

            if (ModuleNoSlowBreak.INSTANCE.getEnabled() && ModuleNoSlowBreak.INSTANCE.getOnAir()){
                return true;
            }

            if (ModuleNoFall.INSTANCE.getEnabled() && NoFallNoGround.INSTANCE.isActive()) {
                return false;
            }

            if (ModuleCriticals.INSTANCE.getEnabled() && ModuleCriticals.NoGroundCrit.INSTANCE.isActive()) {
                return false;
            }
        }


        return instance.isOnGround();
    }

    /**
     * Head rotations injection hook
     */
    @Redirect(method = "tickNewAi", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"))
    private float hookHeadRotations(PlayerEntity instance) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return instance.getYaw();
        }

        Pair<Float, Float> pitch = ModuleRotations.INSTANCE.getRotationPitch();
        ModuleRotations rotations = ModuleRotations.INSTANCE;
        Rotation rotation = rotations.displayRotations();

        // Update pitch here
        rotations.setRotationPitch(new Pair<>(pitch.getRight(), rotation.getPitch()));

        return rotations.shouldDisplayRotations() ? rotation.getYaw() : instance.getYaw();
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V", ordinal = 0))
    private void hookSlowVelocity(PlayerEntity instance, Vec3d velocity) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            if (ModuleKeepSprint.INSTANCE.getEnabled()) {
                return;
            }
        }



        instance.setVelocity(velocity);
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V", ordinal = 0))
    private void hookSlowVelocity(PlayerEntity instance, boolean b) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            if (ModuleKeepSprint.INSTANCE.getEnabled() && !b) {
                return;
            }
        }

        instance.setSprinting(b);
    }

    @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void hookEntityInteractionRange(CallbackInfoReturnable<Float> cir) {
        if ((Object) this == MinecraftClient.getInstance().player && ModuleReach.INSTANCE.getEnabled()) {
            cir.setReturnValue(ModuleReach.INSTANCE.getCombatReach());
            cir.cancel();
        }
    }

    @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void hookBlockInteractionRange(CallbackInfoReturnable<Float> cir) {
        if ((Object) this == MinecraftClient.getInstance().player && ModuleReach.INSTANCE.getEnabled()) {
            cir.setReturnValue(ModuleReach.INSTANCE.getBlockReach());
            cir.cancel();
        }
    }

}
