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
import net.minecraft.world.entity.projectile.arrow.Arrow
import net.minecraft.world.entity.projectile.arrow.ThrownTrident
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball
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
        if (activeArrows && entity is Arrow && !entity.isInGround) {
            return TrajectoryDescriptor(TrajectoryInfo(0.05, 0.3), TrajectoryType.Arrow)
        }
        if (!activeOthers) {
            return null
        }

        return when (entity) {
            is AbstractThrownPotion -> TrajectoryDescriptor(TrajectoryInfo.POTION, TrajectoryType.Potion)
            is ThrownTrident -> {
                if (!entity.isInGround) {
                    TrajectoryDescriptor(TrajectoryInfo.TRIDENT, TrajectoryType.Trident)
                } else {
                    null
                }
            }
            is ThrownEnderpearl -> TrajectoryDescriptor(TrajectoryInfo.GENERIC, TrajectoryType.EnderPearl)
            is Snowball -> TrajectoryDescriptor(TrajectoryInfo.GENERIC, TrajectoryType.Snowball)
            is ThrownExperienceBottle -> TrajectoryDescriptor(TrajectoryInfo.EXP_BOTTLE, TrajectoryType.ExpBottle)
            is ThrownEgg -> TrajectoryDescriptor(TrajectoryInfo.GENERIC, TrajectoryType.Egg)
            is FireworkRocketEntity -> TrajectoryDescriptor(TrajectoryInfo.FIREWORK_ROCKET, TrajectoryType.FireworkRocket)
            is Fireball -> TrajectoryDescriptor(TrajectoryInfo.FIREBALL, TrajectoryType.Fireball)
            else -> null
        }
    }
}
