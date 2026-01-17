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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.additions.InventoryAddition;
import net.ccbluex.liquidbounce.utils.client.SilentHotbar;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Inventory.class)
public abstract class MixinInventory implements InventoryAddition {

    @Shadow
    private int selected;
    @Shadow
    @Final
    public Player player;

    /**
     * Override the original slot based on the server-side slot information.
     */
    @ModifyExpressionValue(
            method = {"removeFromSelected", "tick", "getSelectedItem"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Inventory;selected:I", opcode = Opcodes.GETFIELD)
    )
    private int hookOverrideOriginalSlot(int original) {
        return ((Inventory) (Object) this).player == Minecraft.getInstance().player ? SilentHotbar.INSTANCE.getServersideSlot() : original;
    }

    @Unique
    @Override
    public int liquid_bounce$getRealSelectedSlot() {
        return this.selected;
    }
}
