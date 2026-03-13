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

package net.ccbluex.liquidbounce.utils.render.trajectory

import net.ccbluex.liquidbounce.config.types.list.Tagged

/**
 * @see net.minecraft.world.entity.projectile.ThrowableProjectile.tick
 * @see net.minecraft.world.entity.projectile.arrow.AbstractArrow.tick
 * @see net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile.tick
 * @see net.minecraft.world.entity.projectile.FireworkRocketEntity.tick
 * @see net.minecraft.world.entity.projectile.FishingHook.tick
 */
enum class TrajectoryType(
    override val tag: String,
    /**
     * Determines if first-tick position skip is needed
     *
     * This flag mirrors whether vanilla applies movement-affecting physics before first movement
     * in the corresponding projectile tick loop.
     */
    val requiresInitialTickCorrection: Boolean,
) : Tagged {
    Arrow("Arrow", false),
    Potion("Potion", true),
    EnderPearl("EnderPearl", true),
    FishingBobber("FishingBobber", true),
    Trident("Trident", false),
    Snowball("Snowball", true),
    Egg("Egg", true),
    ExpBottle("ExpBottle", true),
    FireworkRocket("FireworkRocket", false),
    Fireball("Fireball", true),
    WindCharge("WindCharge", true),
}
