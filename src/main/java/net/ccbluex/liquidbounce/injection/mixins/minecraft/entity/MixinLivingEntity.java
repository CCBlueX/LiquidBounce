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

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.PlayerJumpEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleAirJump;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleAntiLevitation;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoJumpDelay;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoPush;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity {

    @Shadow
    private int jumpingCooldown;

    @Shadow
    public abstract float getJumpVelocity();

    @Shadow
    protected boolean jumping;

    @Shadow
    protected abstract void jump();

    /**
     * Hook anti levitation module
     */
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z"))
    public boolean hookTravelStatusEffect(LivingEntity livingEntity, StatusEffect effect) {
        if ((effect == StatusEffects.LEVITATION || effect == StatusEffects.SLOW_FALLING) && ModuleAntiLevitation.INSTANCE.getEnabled()) {
            livingEntity.fallDistance = 0f;
            return false;
        }

        return livingEntity.hasStatusEffect(effect);
    }

    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
    private void hookAntiNausea(StatusEffect effect, CallbackInfoReturnable<Boolean> cir) {
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

    /**
     * Hook velocity rotation modification
     * <p>
     * Jump according to modified rotation. Prevents detection by movement sensitive anticheats such as AAC, Hawk, Intave, etc.
     */
    @Redirect(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookFixRotation(Vec3d instance, double x, double y, double z) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return instance.add(x, y, z);
        }

        if (RotationManager.INSTANCE.getActiveConfigurable() == null || !RotationManager.INSTANCE.getActiveConfigurable().getFixVelocity()) {
            return instance.add(x, y, z);
        }

        Rotation currentRotation = RotationManager.INSTANCE.getCurrentRotation();
        if (currentRotation == null) {
            return instance.add(x, y, z);
        }

        currentRotation = currentRotation.fixedSensitivity();
        if (currentRotation == null) {
            return instance.add(x, y, z);
        }

        float yaw = currentRotation.getYaw() * 0.017453292F;

        return instance.add(-MathHelper.sin(yaw) * 0.2F, 0.0, MathHelper.cos(yaw) * 0.2F);
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void hookNoPush(CallbackInfo callbackInfo) {
        if (ModuleNoPush.INSTANCE.getEnabled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void hookTickMovement(CallbackInfo callbackInfo) {
        if (ModuleNoJumpDelay.INSTANCE.getEnabled() && !ModuleAirJump.INSTANCE.getEnabled()) {
            jumpingCooldown = 0;
        }
    }

    @Inject(method = "tickMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;jumping:Z"))
    private void hookAirJump(CallbackInfo callbackInfo) {
        if (ModuleAirJump.INSTANCE.getEnabled() && jumping && jumpingCooldown == 0) {
            this.jump();
            jumpingCooldown = 10;
        }
    }
}
