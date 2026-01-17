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


package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.projectile;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.additions.FireworkRocketEntityAddition;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleExtendedFirework;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FireworkRocketEntity.class)
public abstract class MixinFireworkRocketEntity implements FireworkRocketEntityAddition {
    @Shadow
    private LivingEntity attachedToEntity;

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 getRotationVector(Vec3 original) {
        if (attachedToEntity != Minecraft.getInstance().player) {
            return original;
        }

        var rotation = RotationManager.INSTANCE.getCurrentRotation();
        var rotationTarget = RotationManager.INSTANCE.getActiveRotationTarget();

        if (rotation == null || rotationTarget == null || rotationTarget.getMovementCorrection() == MovementCorrection.OFF) {
            return original;
        }

        return rotation.directionVector();
    }

    @ModifyArgs(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
    private void hookExtendedFirework(Args args, @Local(ordinal = 0) Vec3 rotation, @Local(ordinal = 1) Vec3 velocity) {
        if (attachedToEntity != Minecraft.getInstance().player
                || !ModuleExtendedFirework.INSTANCE.getRunning()
        ) return;

        var multiplier = ModuleExtendedFirework.getVelocityMultiplier();
        args.set(0, rotation.x * multiplier.x + (rotation.x * multiplier.y - velocity.x) * multiplier.z);
        args.set(1, rotation.y * multiplier.x + (rotation.y * multiplier.y - velocity.y) * multiplier.z);
        args.set(2, rotation.z * multiplier.x + (rotation.z * multiplier.y - velocity.z) * multiplier.z);
    }

    @Override
    public @Nullable LivingEntity liquidbounce$getShooter() {
        return attachedToEntity;
    }
}
