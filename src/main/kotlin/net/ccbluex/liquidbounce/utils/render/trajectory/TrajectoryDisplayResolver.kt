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

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.entity.projectile.arrow.Arrow
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl
import net.minecraft.world.item.ItemStack

object TrajectoryDisplayResolver {
    @JvmStatic
    fun resolveEntityColor(entity: Entity): Color4b {
        return when (entity) {
            is Arrow -> Color4b(255, 0, 0, 200)
            is ThrownEnderpearl -> Color4b(128, 0, 128, 200)
            is FireworkRocketEntity -> Color4b(255, 165, 0, 220)
            else -> Color4b(200, 200, 200, 200)
        }
    }

    @JvmStatic
    fun resolveEntityIcon(
        entity: Entity,
        activeArrows: Boolean,
        activeOthers: Boolean,
    ): ItemStack {
        if (activeArrows && entity is Arrow && !entity.isInGround) {
            return entity.pickupItemStackOrigin
        }

        if (!activeOthers) {
            return ItemStack.EMPTY
        }

        return when (entity) {
            is ThrowableItemProjectile -> entity.item
            is FireworkRocketEntity -> entity.item
            is Fireball -> entity.item
            is AbstractArrow -> if (!entity.isInGround) {
                entity.pickupItemStackOrigin
            } else {
                ItemStack.EMPTY
            }

            else -> ItemStack.EMPTY
        }
    }
}
