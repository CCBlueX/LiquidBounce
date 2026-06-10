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
package net.ccbluex.liquidbounce.injection.mixins.neoforge;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes.CriticalsNoGround;
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallNoGround;
import net.ccbluex.liquidbounce.features.module.modules.render.hitfx.ModuleHitFX;
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleNoSlowBreak;
import net.ccbluex.liquidbounce.interfaces.PlayerAddition;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Loader-specific companion of {@code minecraft.entity.MixinPlayer}.
 * <p>
 * NeoForge patches the bodies of {@code doSweepAttack} and
 * {@code getDestroySpeed} into added overloads with trailing parameters, so
 * each loader hooks its own shape of these methods.
 */
@Mixin(Player.class)
public abstract class MixinPlayer {

    @Inject(
        method = "doSweepAttack(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;FLnet/minecraft/world/phys/AABB;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;playServerSideSound(Lnet/minecraft/sounds/SoundEvent;)V",
            ordinal = 0
        )
    )
    private void hookPlaySound4(Entity target, float damage, DamageSource damageSource, float cooldownProgress,
                                AABB sweepBox, CallbackInfo ci) {
        if (!ModuleHitFX.INSTANCE.getRunning()) {
            ((PlayerAddition) this).liquid_bounce$playSoundIfFakePlayer(target, SoundEvents.PLAYER_ATTACK_SWEEP);
        }
    }

    @ModifyExpressionValue(
        method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)F",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;hasEffect(Lnet/minecraft/core/Holder;)Z")
    )
    private boolean injectFatigueNoSlow(boolean original) {
        if ((Object) this == Minecraft.getInstance().player && ModuleNoSlowBreak.getMiningFatigue()) {
            return false;
        }

        return original;
    }

    @ModifyExpressionValue(
        method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)F",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z")
    )
    private boolean injectWaterNoSlow(boolean original) {
        if ((Object) this == Minecraft.getInstance().player && ModuleNoSlowBreak.getWater()) {
            return false;
        }

        return original;
    }

    @ModifyExpressionValue(
        method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)F",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;onGround()Z")
    )
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

}
