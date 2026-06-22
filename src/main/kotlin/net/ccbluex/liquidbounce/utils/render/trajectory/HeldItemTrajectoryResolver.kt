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

import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.player.Player
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

object HeldItemTrajectoryResolver {
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
    fun resolveHeldItemShots(
        player: Player,
        stack: ItemStack,
        alwaysShowBow: Boolean,
        includeMultiShot: Boolean = true,
    ): List<TrajectoryShotDescriptor>? {
        return when (stack.item) {
            is BowItem -> {
                val useTime = if (alwaysShowBow && player.ticksUsingItem < 1) {
                    40
                } else {
                    player.ticksUsingItem
                }

                val trajectoryInfo = TrajectoryInfo.bowWithUsageDuration(useTime) ?: return null
                singleShot(stack, TrajectoryDescriptor(trajectoryInfo, TrajectoryType.Arrow))
            }
            is CrossbowItem -> {
                val chargedProjectiles = stack[DataComponents.CHARGED_PROJECTILES]
                val chargedProjectileCount = chargedProjectiles?.items?.size ?: 0
                val isMultiShot = stack.getEnchantment(Enchantments.MULTISHOT) > 0
                val shotCount = when {
                    !includeMultiShot -> 1
                    chargedProjectileCount > 0 -> chargedProjectileCount
                    isMultiShot -> 3
                    else -> 1
                }.coerceAtLeast(1)

                val trajectoryDescriptor = if (isCrossbowFirework(chargedProjectiles)) {
                    TrajectoryDescriptor.FIREWORK_ROCKET
                } else {
                    TrajectoryDescriptor.CROSSBOW_ARROW
                }

                getShotYawOffsets(shotCount).map { yawOffsetDegrees ->
                    trajectoryDescriptor.toShotDescriptor(
                        yawOffsetDegrees = yawOffsetDegrees,
                        icon = stack
                    )
                }
            }
            is FishingRodItem -> singleShot(stack, TrajectoryDescriptor.FISHING_BOBBER)
            is ThrowablePotionItem -> singleShot(stack, TrajectoryDescriptor.POTION)
            is TridentItem -> singleShot(stack, TrajectoryDescriptor.TRIDENT)
            is SnowballItem -> singleShot(stack, TrajectoryDescriptor.SNOWBALL)
            is EnderpearlItem -> singleShot(stack, TrajectoryDescriptor.ENDER_PEARL)
            is EggItem -> singleShot(stack, TrajectoryDescriptor.EGG)
            is ExperienceBottleItem -> singleShot(stack, TrajectoryDescriptor.EXP_BOTTLE)
            is FireChargeItem -> singleShot(stack, TrajectoryDescriptor.FIREBALL)
            is WindChargeItem -> singleShot(stack, TrajectoryDescriptor.WIND_CHARGE)
            else -> null
        }
    }

    @JvmStatic
    fun resolveHeldItemPrimaryShot(
        player: Player,
        stack: ItemStack,
        alwaysShowBow: Boolean,
    ): TrajectoryDescriptor? {
        return resolveHeldItemShots(player, stack, alwaysShowBow, includeMultiShot = false)
            ?.firstOrNull()
            ?.let { TrajectoryDescriptor(it.trajectoryInfo, it.trajectoryType) }
    }

    private fun isCrossbowFirework(chargedProjectiles: ChargedProjectiles?): Boolean {
        return chargedProjectiles != null && chargedProjectiles.contains(Items.FIREWORK_ROCKET)
    }

    private fun singleShot(
        stack: ItemStack,
        trajectoryDescriptor: TrajectoryDescriptor,
    ): List<TrajectoryShotDescriptor> {
        return listOf(trajectoryDescriptor.toShotDescriptor(icon = stack))
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
}
