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
import net.minecraft.core.component.DataComponentGetter
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.entity.projectile.arrow.Arrow
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.alchemy.PotionContents

object TrajectoryDisplayResolver {
    /**
     * @see Arrow.NO_EFFECT_COLOR
     */
    private const val POTION_ARROW_COLOR_NONE = -1

    @JvmStatic
    fun resolveTrajectoryColor(
        trajectoryType: TrajectoryType,
        colorSource: DataComponentGetter = ItemStack.EMPTY,
        entity: Entity? = null,
    ): Color4b {
        return when (trajectoryType) {
            TrajectoryType.Arrow -> resolveArrowColor(colorSource, entity)
            TrajectoryType.Potion -> resolvePotionColor(colorSource)
            TrajectoryType.EnderPearl -> Color4b.PURPLE.alpha(200)
            TrajectoryType.FishingBobber -> Color4b.CYAN.alpha(200)
            TrajectoryType.Trident -> Color4b(180, 210, 255, 220)
            TrajectoryType.Snowball -> Color4b.LIGHT_GRAY.alpha(220)
            TrajectoryType.Egg -> Color4b(240, 234, 214, 220)
            TrajectoryType.ExpBottle -> Color4b(120, 230, 120, 220)
            TrajectoryType.FireworkRocket -> Color4b.ORANGE.alpha(220)
            TrajectoryType.Fireball -> Color4b.ORANGE.alpha(220)
            TrajectoryType.WindCharge -> Color4b(180, 235, 255, 220)
        }
    }

    private fun resolveArrowColor(
        colorSource: DataComponentGetter,
        entity: Entity?,
    ): Color4b {
        if (entity is Arrow) {
            val potionColor = entity.color
            if (potionColor != POTION_ARROW_COLOR_NONE) {
                return Color4b.fullAlpha(potionColor).alpha(220)
            }
        }

        val potionColor = colorSource[DataComponents.POTION_CONTENTS]?.color
        return if (potionColor != null) {
            Color4b.fullAlpha(potionColor).alpha(220)
        } else {
            Color4b.WHITE.alpha(220)
        }
    }

    private fun resolvePotionColor(colorSource: DataComponentGetter): Color4b {
        val potionColor = colorSource[DataComponents.POTION_CONTENTS]?.color
        return if (potionColor != null) {
            Color4b.fullAlpha(potionColor).alpha(220)
        } else {
            Color4b.fullAlpha(PotionContents.BASE_POTION_COLOR).alpha(220)
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
