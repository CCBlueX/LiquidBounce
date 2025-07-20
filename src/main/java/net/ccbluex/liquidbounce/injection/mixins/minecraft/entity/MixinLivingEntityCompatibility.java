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

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAnimations;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = LivingEntity.class, priority = 999)
public abstract class MixinLivingEntityCompatibility {

    // this is here and not in the MixinLivingEntity class to apply a priority of 999
    // while keeping the higher priority for all other methods in MixinLivingEntity
    // require = 0 and priority = 999 avoid with Mixin conflicts with mods that target the same constant
    @ModifyConstant(method = "getHandSwingDuration", constant = @Constant(intValue = 6), require = 0)
    private int hookSwingSpeed(int constant) {
        var animations = ModuleAnimations.INSTANCE;
        return animations.getRunning() ? animations.getSwingDuration() : constant;
    }

}
