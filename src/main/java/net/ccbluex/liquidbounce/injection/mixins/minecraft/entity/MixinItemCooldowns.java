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

import net.ccbluex.liquidbounce.interfaces.ItemCooldownsAddition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(ItemCooldowns.class)
public abstract class MixinItemCooldowns implements ItemCooldownsAddition {

    @Shadow
    @Final
    private Map<Identifier, ItemCooldowns.CooldownInstance> cooldowns;

    @Shadow
    private int tickCount;

    @Shadow
    public abstract Identifier getCooldownGroup(ItemStack stack);

    @Override
    public @Nullable Entry liquidBounce$getCooldown(@NotNull ItemStack stack) {
        var entry = this.cooldowns.get(this.getCooldownGroup(stack));
        if (entry != null) {
            return new Entry(this.tickCount, entry.startTime(), entry.endTime());
        } else {
            return null;
        }
    }
}
