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
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.entity.projectile.arrow.Arrow
import net.minecraft.world.entity.projectile.arrow.ThrownTrident
import net.minecraft.world.entity.projectile.FireworkRocketEntity
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
import net.minecraft.world.item.component.ChargedProjectiles
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object TrajectoryData {
    @JvmRecord
    data class TrajectoryShotSpec(
        val trajectoryInfo: TrajectoryInfo,
        val trajectoryType: TrajectoryType,
        val yawOffsetDegrees: Float = 0f,
        val icon: ItemStack = ItemStack.EMPTY,
    )

    @JvmStatic
    /**
     * Resolves one or more rendered trajectory shots for held items.
     *
     * Crossbow behavior mirrors vanilla projectile loading/spread semantics:
     * @see net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES
     * @see net.minecraft.world.item.component.ChargedProjectiles.items
     * @see net.minecraft.world.item.ProjectileWeaponItem.draw
     * @see net.minecraft.world.item.ProjectileWeaponItem.shoot
     * @see net.minecraft.world.item.enchantment.EnchantmentHelper.processProjectileCount
     * @see net.minecraft.world.item.enchantment.EnchantmentHelper.processProjectileSpread
     * @see net.minecraft.world.item.CrossbowItem.createProjectile
     */
    fun getRenderedTrajectoryShotSpecs(
        player: Player,
        stack: ItemStack,
        alwaysShowBow: Boolean,
    ): List<TrajectoryShotSpec>? {
        return when (stack.item) {
            is BowItem -> {
                val useTime = if (alwaysShowBow && player.ticksUsingItem < 1) {
                    40
                } else {
                    player.ticksUsingItem
                }

                val trajectoryInfo = TrajectoryInfo.bowWithUsageDuration(useTime) ?: return null
                listOf(TrajectoryShotSpec(trajectoryInfo, TrajectoryType.Arrow, icon = stack))
            }
            is CrossbowItem -> {
                val chargedProjectiles = stack[DataComponents.CHARGED_PROJECTILES]
                val chargedProjectileCount = chargedProjectiles?.items?.size ?: 0
                val isMultiShot = stack.getEnchantment(Enchantments.MULTISHOT) > 0
                val shotCount = when {
                    chargedProjectileCount > 0 -> chargedProjectileCount
                    isMultiShot -> 3
                    else -> 1
                }.coerceAtLeast(1)

                val trajectoryInfoTyped = if (isCrossbowFirework(chargedProjectiles)) {
                    TrajectoryInfo.FIREWORK_ROCKET.typed(TrajectoryType.FireworkRocket)
                } else {
                    TrajectoryInfo.BOW_FULL_PULL.typed(TrajectoryType.Arrow)
                }

                getShotYawOffsets(shotCount).map { yawOffsetDegrees ->
                    TrajectoryShotSpec(
                        trajectoryInfo = trajectoryInfoTyped.info,
                        trajectoryType = trajectoryInfoTyped.type,
                        yawOffsetDegrees = yawOffsetDegrees,
                        icon = stack
                    )
                }
            }
            is FishingRodItem -> listOf(TrajectoryShotSpec(TrajectoryInfo.FISHING_ROD, TrajectoryType.FishingBobber, icon = stack))
            is ThrowablePotionItem -> listOf(TrajectoryShotSpec(TrajectoryInfo.POTION, TrajectoryType.Potion, icon = stack))
            is TridentItem -> listOf(TrajectoryShotSpec(TrajectoryInfo.TRIDENT, TrajectoryType.Trident, icon = stack))
            is SnowballItem -> listOf(TrajectoryShotSpec(TrajectoryInfo.GENERIC, TrajectoryType.Snowball, icon = stack))
            is EnderpearlItem -> listOf(TrajectoryShotSpec(TrajectoryInfo.GENERIC, TrajectoryType.EnderPearl, icon = stack))
            is EggItem -> listOf(TrajectoryShotSpec(TrajectoryInfo.GENERIC, TrajectoryType.Egg, icon = stack))
            is ExperienceBottleItem -> listOf(TrajectoryShotSpec(TrajectoryInfo.EXP_BOTTLE, TrajectoryType.ExpBottle, icon = stack))
            is FireChargeItem -> listOf(TrajectoryShotSpec(TrajectoryInfo.FIREBALL, TrajectoryType.Fireball, icon = stack))
            is WindChargeItem -> listOf(TrajectoryShotSpec(TrajectoryInfo.WIND_CHARGE, TrajectoryType.WindCharge, icon = stack))
            else -> null
        }
    }

    /**
     * Fallback compatibility API for single-shot callers.
     *
     * @see getRenderedTrajectoryShotSpecs
     */
    @JvmStatic
    fun getRenderedTrajectoryInfo(
        player: Player,
        stack: ItemStack,
        alwaysShowBow: Boolean,
    ): TrajectoryInfo.Typed? {
        return getRenderedTrajectoryShotSpecs(player, stack, alwaysShowBow)
            ?.firstOrNull()
            ?.let {
                it.trajectoryInfo.typed(it.trajectoryType)
            }
    }

    private fun isCrossbowFirework(chargedProjectiles: ChargedProjectiles?): Boolean {
        return chargedProjectiles != null && chargedProjectiles.contains(Items.FIREWORK_ROCKET)
    }

    /**
     * Yaw offset model for multi-shot trajectory preview.
     *
     * The `[-10, 0, +10]` branch mirrors vanilla triple-shot spread behavior.
     * @see net.minecraft.world.item.ProjectileWeaponItem.shoot
     */
    private fun getShotYawOffsets(shotCount: Int): FloatArray {
        return when (shotCount) {
            1 -> floatArrayOf(0f)
            3 -> floatArrayOf(-10f, 0f, 10f)
            else -> {
                val spread = 20f
                val step = spread / (shotCount - 1).toFloat()
                FloatArray(shotCount) { index ->
                    -spread * 0.5f + step * index.toFloat()
                }
            }
        }
    }

    @JvmStatic
    fun getColorForEntity(it: Entity): Color4b {
        return when (it) {
            is Arrow -> Color4b(255, 0, 0, 200)
            is ThrownEnderpearl -> Color4b(128, 0, 128, 200)
            is FireworkRocketEntity -> Color4b(255, 165, 0, 220)
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
            is FireworkRocketEntity -> TrajectoryInfo.FIREWORK_ROCKET.typed(TrajectoryType.FireworkRocket)
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
        val FIREWORK_ROCKET = TrajectoryInfo(
            gravity = 0.0,
            hitboxRadius = 0.25,
            initialVelocity = 1.6,
            drag = 1.0,
            dragInWater = 1.0,
            copiesPlayerVelocity = false
        )
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
