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
    val initialTickBehavior: InitialTickBehavior,
) : Tagged {
    ARROW("Arrow", InitialTickBehavior.NONE),
    POTION("Potion", InitialTickBehavior.APPLY_VELOCITY_ONLY_BEFORE_FIRST_MOVE),
    ENDER_PEARL("EnderPearl", InitialTickBehavior.APPLY_VELOCITY_ONLY_BEFORE_FIRST_MOVE),
    FISHING_BOBBER("FishingBobber", InitialTickBehavior.APPLY_VELOCITY_ONLY_BEFORE_FIRST_MOVE),
    TRIDENT("Trident", InitialTickBehavior.NONE),
    SNOWBALL("Snowball", InitialTickBehavior.APPLY_VELOCITY_ONLY_BEFORE_FIRST_MOVE),
    EGG("Egg", InitialTickBehavior.APPLY_VELOCITY_ONLY_BEFORE_FIRST_MOVE),
    EXP_BOTTLE("ExpBottle", InitialTickBehavior.APPLY_VELOCITY_ONLY_BEFORE_FIRST_MOVE),
    FIREWORK_ROCKET("FireworkRocket", InitialTickBehavior.NONE),
    FIREBALL("Fireball", InitialTickBehavior.APPLY_VELOCITY_ONLY_BEFORE_FIRST_MOVE),
    WIND_CHARGE("WindCharge", InitialTickBehavior.APPLY_VELOCITY_ONLY_BEFORE_FIRST_MOVE),
    ;

    enum class InitialTickBehavior {
        NONE,
        APPLY_VELOCITY_ONLY_BEFORE_FIRST_MOVE,
    }

    val requiresInitialTickCorrection: Boolean
        get() = initialTickBehavior == InitialTickBehavior.APPLY_VELOCITY_ONLY_BEFORE_FIRST_MOVE
}
