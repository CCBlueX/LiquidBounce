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

import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.world.item.BowItem
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

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

    companion object {
        /**
         * @see net.minecraft.world.entity.projectile.ThrowableProjectile.getDefaultGravity
         * @see net.minecraft.world.entity.projectile.ThrowableProjectile.tick
         */
        @JvmField
        val GENERIC = TrajectoryInfo(
            gravity = 0.03,
            hitboxRadius = 0.25,
            dragInWater = 0.8,
        )
        @JvmField
        val PERSISTENT = TrajectoryInfo(0.05, 0.5)
        @JvmField
        val POTION = GENERIC.copy(gravity = 0.05, initialVelocity = 0.5, roll = -20.0F)
        /**
         * @see net.minecraft.world.item.ExperienceBottleItem.use
         * @see net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle.getDefaultGravity
         */
        @JvmField
        val EXP_BOTTLE = POTION.copy(gravity = 0.07, initialVelocity = 0.7)
        /**
         * @see net.minecraft.world.entity.projectile.FishingHook.tick
         */
        @JvmField
        val FISHING_ROD = GENERIC.copy(gravity = 0.04, drag = 0.92, dragInWater = 0.92)
        @JvmField
        val TRIDENT = PERSISTENT.copy(initialVelocity = 2.5, gravity = 0.05, dragInWater = 0.99)
        @JvmField
        val BOW_FULL_PULL = PERSISTENT.copy(initialVelocity = 3.0)
        /**
         * Note: Player only
         *
         * @see net.minecraft.world.item.CrossbowItem.performShooting
         * @see net.minecraft.world.item.CrossbowItem.ARROW_POWER
         */
        @JvmField
        val CROSSBOW_ARROW = PERSISTENT.copy(
            initialVelocity = 3.15,
            copiesPlayerVelocity = false
        )
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
        /**
         * @see net.minecraft.world.item.WindChargeItem.use
         * @see net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge.getInertia
         */
        @JvmField
        val WIND_CHARGE = TrajectoryInfo(
            gravity = 0.0,
            hitboxRadius = 1.0,
            drag = 1.0,
            dragInWater = 1.0,
            copiesPlayerVelocity = true
        )

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
