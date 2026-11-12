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

import net.minecraft.core.component.DataComponentGetter
import net.minecraft.world.item.ItemStack

@JvmRecord
data class TrajectoryDescriptor(
    val trajectoryInfo: TrajectoryInfo,
    val trajectoryType: TrajectoryType,
) {
    fun toShotDescriptor(
        yawOffsetDegrees: Float = 0f,
        icon: ItemStack = ItemStack.EMPTY,
        colorSource: DataComponentGetter = icon,
    ): TrajectoryShotDescriptor {
        return TrajectoryShotDescriptor(
            trajectoryInfo = trajectoryInfo,
            trajectoryType = trajectoryType,
            yawOffsetDegrees = yawOffsetDegrees,
            icon = icon,
            colorSource = colorSource,
        )
    }

    companion object {
        @JvmField
        val BOW_ARROW = TrajectoryDescriptor(TrajectoryInfo.BOW_FULL_PULL, TrajectoryType.ARROW)

        @JvmField
        val CROSSBOW_ARROW = TrajectoryDescriptor(TrajectoryInfo.CROSSBOW_ARROW, TrajectoryType.ARROW)

        @JvmField
        val ENTITY_ARROW = TrajectoryDescriptor(TrajectoryInfo(0.05, 0.3), TrajectoryType.ARROW)

        @JvmField
        val POTION = TrajectoryDescriptor(TrajectoryInfo.POTION, TrajectoryType.POTION)

        @JvmField
        val ENDER_PEARL = TrajectoryDescriptor(TrajectoryInfo.GENERIC, TrajectoryType.ENDER_PEARL)

        @JvmField
        val FISHING_BOBBER = TrajectoryDescriptor(TrajectoryInfo.FISHING_ROD, TrajectoryType.FISHING_BOBBER)

        @JvmField
        val TRIDENT = TrajectoryDescriptor(TrajectoryInfo.TRIDENT, TrajectoryType.TRIDENT)

        @JvmField
        val SNOWBALL = TrajectoryDescriptor(TrajectoryInfo.GENERIC, TrajectoryType.SNOWBALL)

        @JvmField
        val EGG = TrajectoryDescriptor(TrajectoryInfo.GENERIC, TrajectoryType.EGG)

        @JvmField
        val EXP_BOTTLE = TrajectoryDescriptor(TrajectoryInfo.EXP_BOTTLE, TrajectoryType.EXP_BOTTLE)

        @JvmField
        val FIREWORK_ROCKET = TrajectoryDescriptor(TrajectoryInfo.FIREWORK_ROCKET, TrajectoryType.FIREWORK_ROCKET)

        @JvmField
        val FIREBALL = TrajectoryDescriptor(TrajectoryInfo.FIREBALL, TrajectoryType.FIREBALL)

        @JvmField
        val WIND_CHARGE = TrajectoryDescriptor(TrajectoryInfo.WIND_CHARGE, TrajectoryType.WIND_CHARGE)
    }
}

@JvmRecord
data class TrajectoryShotDescriptor(
    val trajectoryInfo: TrajectoryInfo,
    val trajectoryType: TrajectoryType,
    val yawOffsetDegrees: Float = 0f,
    val icon: ItemStack = ItemStack.EMPTY,
    val colorSource: DataComponentGetter = icon,
)
