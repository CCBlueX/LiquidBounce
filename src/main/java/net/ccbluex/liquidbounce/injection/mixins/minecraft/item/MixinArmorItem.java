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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.item;

import net.ccbluex.liquidbounce.interfaces.ArmorItemAdditions;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorItem.class)
public abstract class MixinArmorItem implements ArmorItemAdditions {

    @Unique
    private ArmorMaterial liquid_bounce$armorMaterial;

    @Unique
    private EquipmentType liquid_bounce$type;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void hookCatchArgs(ArmorMaterial material, EquipmentType type, Item.Settings settings, CallbackInfo ci) {
        this.liquid_bounce$armorMaterial = material;
        this.liquid_bounce$type = type;
    }

    @Override
    public ArmorMaterial liquid_bounce$getMaterial() {
        return liquid_bounce$armorMaterial;
    }

    @Override
    public EquipmentType liquid_bounce$getType() {
        return liquid_bounce$type;
    }

}
