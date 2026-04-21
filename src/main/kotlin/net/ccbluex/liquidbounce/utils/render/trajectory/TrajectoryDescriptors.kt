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

import net.minecraft.world.item.ItemStack

@JvmRecord
data class TrajectoryDescriptor(
    val trajectoryInfo: TrajectoryInfo,
    val trajectoryType: TrajectoryType,
) {
    fun toShotDescriptor(
        yawOffsetDegrees: Float = 0f,
        icon: ItemStack = ItemStack.EMPTY,
    ): TrajectoryShotDescriptor {
        return TrajectoryShotDescriptor(
            trajectoryInfo = trajectoryInfo,
            trajectoryType = trajectoryType,
            yawOffsetDegrees = yawOffsetDegrees,
            icon = icon
        )
    }

    companion object {
        @JvmField
        val BOW_ARROW = TrajectoryDescriptor(TrajectoryInfo.BOW_FULL_PULL, TrajectoryType.Arrow)

        @JvmField
        val CROSSBOW_ARROW = TrajectoryDescriptor(TrajectoryInfo.CROSSBOW_ARROW, TrajectoryType.Arrow)

        @JvmField
        val ENTITY_ARROW = TrajectoryDescriptor(TrajectoryInfo(0.05, 0.3), TrajectoryType.Arrow)

        @JvmField
        val POTION = TrajectoryDescriptor(TrajectoryInfo.POTION, TrajectoryType.Potion)

        @JvmField
        val ENDER_PEARL = TrajectoryDescriptor(TrajectoryInfo.GENERIC, TrajectoryType.EnderPearl)

        @JvmField
        val FISHING_BOBBER = TrajectoryDescriptor(TrajectoryInfo.FISHING_ROD, TrajectoryType.FishingBobber)

        @JvmField
        val TRIDENT = TrajectoryDescriptor(TrajectoryInfo.TRIDENT, TrajectoryType.Trident)

        @JvmField
        val SNOWBALL = TrajectoryDescriptor(TrajectoryInfo.GENERIC, TrajectoryType.Snowball)

        @JvmField
        val EGG = TrajectoryDescriptor(TrajectoryInfo.GENERIC, TrajectoryType.Egg)

        @JvmField
        val EXP_BOTTLE = TrajectoryDescriptor(TrajectoryInfo.EXP_BOTTLE, TrajectoryType.ExpBottle)

        @JvmField
        val FIREWORK_ROCKET = TrajectoryDescriptor(TrajectoryInfo.FIREWORK_ROCKET, TrajectoryType.FireworkRocket)

        @JvmField
        val FIREBALL = TrajectoryDescriptor(TrajectoryInfo.FIREBALL, TrajectoryType.Fireball)

        @JvmField
        val WIND_CHARGE = TrajectoryDescriptor(TrajectoryInfo.WIND_CHARGE, TrajectoryType.WindCharge)
    }
}

@JvmRecord
data class TrajectoryShotDescriptor(
    val trajectoryInfo: TrajectoryInfo,
    val trajectoryType: TrajectoryType,
    val yawOffsetDegrees: Float = 0f,
    val icon: ItemStack = ItemStack.EMPTY,
)
