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
package net.ccbluex.liquidbounce.interfaces;

/**
 * Additions to {@link net.minecraft.entity.LivingEntity}.
 */
public interface LivingEntityAddition {

    /**
     * Allows the entity to pop infinite totems and
     * bypass {@link net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY}
     * damage sources.
     */
    @SuppressWarnings("JavadocReference")
    boolean liquid_bounce$hasInfiniteTotems();

    /**
     * Allows client side entities to get attacked.
     */
    boolean liquid_bounce$isClientsideDamagable();

}
