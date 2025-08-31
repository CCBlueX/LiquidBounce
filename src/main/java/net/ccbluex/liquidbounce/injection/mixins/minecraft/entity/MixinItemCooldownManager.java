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

import net.ccbluex.liquidbounce.interfaces.ItemCooldownManagerAdditions;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(ItemCooldownManager.class)
public abstract class MixinItemCooldownManager implements ItemCooldownManagerAdditions {

    @Shadow
    @Final
    private Map<Identifier, ItemCooldownManager.Entry> entries;

    @Shadow
    private int tick;

    @Shadow
    public abstract Identifier getGroup(ItemStack stack);

    @Override
    public @Nullable Entry liquidBounce$getCooldown(@NotNull ItemStack stack) {
        var entry = this.entries.get(this.getGroup(stack));
        if (entry != null) {
            return new Entry(this.tick, entry.startTick(), entry.endTick());
        } else {
            return null;
        }
    }
}
