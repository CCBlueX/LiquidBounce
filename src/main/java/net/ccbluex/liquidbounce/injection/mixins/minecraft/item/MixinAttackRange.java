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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleHitbox;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReach;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.AttackRange;
import org.jspecify.annotations.NullMarked;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@NullMarked
@Mixin(AttackRange.class)
public abstract class MixinAttackRange {

    @ModifyExpressionValue(
        method = "defaultFor",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D")
    )
    private static double applyReachDefault(double original, @Local(argsOnly = true) LivingEntity entity) {
        if (entity == Minecraft.getInstance().player && ModuleReach.INSTANCE.getRunning()) {
            return ModuleReach.INSTANCE.getEntityInteractionReach() + original;
        }
        return original;
    }

    @ModifyExpressionValue(
        method = "isInRange(Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/ToDoubleFunction;D)Z",
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/component/AttackRange;hitboxMargin:F", opcode = Opcodes.GETFIELD)
    )
    private static float applyHitboxMargin(float original, @Local(argsOnly = true) LivingEntity entity) {
        if (entity == Minecraft.getInstance().player && ModuleHitbox.INSTANCE.getRunning() && ModuleHitbox.INSTANCE.getApplyToComponent()) {
            return ModuleHitbox.INSTANCE.getSize() + original;
        }
        return original;
    }

    @ModifyReturnValue(
        method = "effectiveMinRange",
        at = @At("RETURN")
    )
    private float applyReachMinRange(float original, @Local(argsOnly = true) Entity entity) {
        if (entity == Minecraft.getInstance().player && ModuleReach.INSTANCE.getRunning()) {
            return Math.max(0F, original - ModuleReach.INSTANCE.getComponentMinRangeReach());
        }
        return original;
    }

    @ModifyReturnValue(
        method = "effectiveMaxRange",
        at = @At("RETURN")
    )
    private float applyReachMaxRange(float original, @Local(argsOnly = true) Entity entity) {
        if (entity == Minecraft.getInstance().player && ModuleReach.INSTANCE.getRunning()) {
            return original + ModuleReach.INSTANCE.getComponentMaxRangeReach();
        }
        return original;
    }

}
