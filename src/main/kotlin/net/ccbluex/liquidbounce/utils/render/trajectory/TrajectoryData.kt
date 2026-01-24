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
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.entity.projectile.arrow.Arrow
import net.minecraft.world.entity.projectile.arrow.ThrownTrident
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.EggItem
import net.minecraft.world.item.EnderpearlItem
import net.minecraft.world.item.ExperienceBottleItem
import net.minecraft.world.item.FireChargeItem
import net.minecraft.world.item.FishingRodItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.SnowballItem
import net.minecraft.world.item.ThrowablePotionItem
import net.minecraft.world.item.TridentItem
import net.minecraft.world.item.WindChargeItem
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object TrajectoryData {
    @JvmStatic
    fun getRenderedTrajectoryInfo(
        player: Player,
        stack: ItemStack,
        alwaysShowBow: Boolean,
    ): TrajectoryInfo.Typed? {
        return when (stack.item) {
            is BowItem -> {
                val useTime = if (alwaysShowBow && player.ticksUsingItem < 1) {
                    40
                } else {
                    player.ticksUsingItem
                }

                TrajectoryInfo.bowWithUsageDuration(useTime)?.typed(TrajectoryType.Arrow)
            }
            is CrossbowItem -> {
                val chargedProjectiles = stack[DataComponents.CHARGED_PROJECTILES]
                if (chargedProjectiles != null && chargedProjectiles.contains(Items.FIREWORK_ROCKET)) {
                    TrajectoryInfo.FIREBALL.typed(TrajectoryType.Fireball)
                } else {
                    TrajectoryInfo.BOW_FULL_PULL.typed(TrajectoryType.Arrow)
                }
            }
            is FishingRodItem -> TrajectoryInfo.FISHING_ROD.typed(TrajectoryType.FishingBobber)
            is ThrowablePotionItem -> TrajectoryInfo.POTION.typed(TrajectoryType.Potion)
            is TridentItem -> TrajectoryInfo.TRIDENT.typed(TrajectoryType.Trident)
            is SnowballItem -> TrajectoryInfo.GENERIC.typed(TrajectoryType.Snowball)
            is EnderpearlItem -> TrajectoryInfo.GENERIC.typed(TrajectoryType.EnderPearl)
            is EggItem -> TrajectoryInfo.GENERIC.typed(TrajectoryType.Egg)
            is ExperienceBottleItem -> TrajectoryInfo.EXP_BOTTLE.typed(TrajectoryType.ExpBottle)
            is FireChargeItem -> TrajectoryInfo.FIREBALL.typed(TrajectoryType.Fireball)
            is WindChargeItem -> TrajectoryInfo.WIND_CHARGE.typed(TrajectoryType.WindCharge)
            else -> null
        }
    }

    @JvmStatic
    fun getColorForEntity(it: Entity): Color4b {
        return when (it) {
            is Arrow -> Color4b(255, 0, 0, 200)
            is ThrownEnderpearl -> Color4b(128, 0, 128, 200)
            else -> Color4b(200, 200, 200, 200)
        }
    }

    @JvmStatic
    fun getRenderTrajectoryInfoForOtherEntity(
        entity: Entity,
        activeArrows: Boolean,
        activeOthers: Boolean,
    ): TrajectoryInfo.Typed? {
        if (activeArrows && entity is Arrow && !entity.isInGround) {
            return TrajectoryInfo(0.05, 0.3).typed(TrajectoryType.Arrow)
        }
        if (!activeOthers) {
            return null
        }

        return when (entity) {
            is AbstractThrownPotion -> TrajectoryInfo.POTION.typed(TrajectoryType.Potion)
            is ThrownTrident -> {
                if (!entity.isInGround) {
                    TrajectoryInfo.TRIDENT.typed(TrajectoryType.Trident)
                } else {
                    null
                }
            }
            is ThrownEnderpearl -> TrajectoryInfo.GENERIC.typed(TrajectoryType.EnderPearl)
            is Snowball -> TrajectoryInfo.GENERIC.typed(TrajectoryType.Snowball)
            is ThrownExperienceBottle -> TrajectoryInfo.EXP_BOTTLE.typed(TrajectoryType.ExpBottle)
            is ThrownEgg -> TrajectoryInfo.GENERIC.typed(TrajectoryType.Egg)
            is Fireball -> TrajectoryInfo.FIREBALL.typed(TrajectoryType.Fireball)
            else -> null
        }
    }

    @JvmStatic
    fun getRenderIconForOtherEntity(
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

@JvmRecord
data class TrajectoryInfo(
    val gravity: Double,
    /**
     * Radius (!!) of the projectile
     */
    val hitboxRadius: Double,
    val initialVelocity: Double = 1.5,
    val drag: Double = 0.99,
    val dragInWater: Double = 0.6,
    val roll: Float = 0.0F,
    val copiesPlayerVelocity: Boolean = true,
) {
    @JvmOverloads
    fun hitbox(center: Vec3 = Vec3.ZERO): AABB = AABB(
        center.x - hitboxRadius,
        center.y - hitboxRadius,
        center.z - hitboxRadius,
        center.x + hitboxRadius,
        center.y + hitboxRadius,
        center.z + hitboxRadius,
    )

    fun typed(type: TrajectoryType) = Typed(this, type)

    @JvmRecord
    data class Typed(val info: TrajectoryInfo, val type: TrajectoryType)

    companion object {
        @JvmField
        val GENERIC = TrajectoryInfo(0.03, 0.25)
        @JvmField
        val PERSISTENT = TrajectoryInfo(0.05, 0.5)
        @JvmField
        val POTION = GENERIC.copy(gravity = 0.05, initialVelocity = 0.5, roll = -20.0F)
        @JvmField
        val EXP_BOTTLE = POTION.copy(initialVelocity = 0.7)
        @JvmField
        val FISHING_ROD = GENERIC.copy(gravity = 0.04, drag = 0.92)
        @JvmField
        val TRIDENT = PERSISTENT.copy(initialVelocity = 2.5, gravity = 0.05, dragInWater = 0.99)
        @JvmField
        val BOW_FULL_PULL = PERSISTENT.copy(initialVelocity = 3.0)
        @JvmField
        val FIREBALL = TrajectoryInfo(gravity = 0.0, hitboxRadius = 1.0)
        @JvmField
        val WIND_CHARGE = TrajectoryInfo(gravity = 0.0, hitboxRadius = 1.0, copiesPlayerVelocity = false)

        @JvmStatic
        @JvmOverloads
        fun bowWithUsageDuration(usageDurationTicks: Int = player.ticksUsingItem): TrajectoryInfo? {
            // Calculate the power of bow
            val power = BowItem.getPowerForTime(usageDurationTicks)

            if (power < 0.1F) {
                return null
            }

            val v0 = power * BOW_FULL_PULL.initialVelocity

            return BOW_FULL_PULL.copy(initialVelocity = v0)
        }
    }
}
