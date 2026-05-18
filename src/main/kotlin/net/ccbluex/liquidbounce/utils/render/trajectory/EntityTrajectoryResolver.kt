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

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.entity.projectile.FishingHook
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.entity.projectile.arrow.ThrownTrident
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle

object EntityTrajectoryResolver {
    @JvmStatic
    fun resolveEntityTrajectory(
        entity: Entity,
        activeArrows: Boolean,
        activeOthers: Boolean,
    ): TrajectoryDescriptor? {
        if (activeArrows && entity is AbstractArrow && entity !is ThrownTrident && !entity.isInGround) {
            return TrajectoryDescriptor.ENTITY_ARROW
        }
        if (!activeOthers) {
            return null
        }

        return when (entity) {
            is AbstractThrownPotion -> TrajectoryDescriptor.POTION
            is ThrownTrident -> {
                if (!entity.isInGround) {
                    TrajectoryDescriptor.TRIDENT
                } else {
                    null
                }
            }
            is ThrownEnderpearl -> TrajectoryDescriptor.ENDER_PEARL
            is Snowball -> TrajectoryDescriptor.SNOWBALL
            is ThrownExperienceBottle -> TrajectoryDescriptor.EXP_BOTTLE
            is ThrownEgg -> TrajectoryDescriptor.EGG
            is FishingHook -> TrajectoryDescriptor.FISHING_BOBBER
            is FireworkRocketEntity -> TrajectoryDescriptor.FIREWORK_ROCKET
            is Fireball -> TrajectoryDescriptor.FIREBALL
            is WindCharge -> TrajectoryDescriptor.WIND_CHARGE
            else -> null
        }
    }
}
