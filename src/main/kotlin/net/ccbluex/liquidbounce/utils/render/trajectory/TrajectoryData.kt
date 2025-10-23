/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
 *
 */

package net.ccbluex.liquidbounce.utils.render.trajectory

import net.ccbluex.liquidbounce.features.module.modules.render.trajectories.ModuleTrajectories.ProjectileType
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.*
import net.minecraft.entity.projectile.thrown.*
import net.minecraft.item.*
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

object TrajectoryData {
    @JvmStatic
    fun getRenderedTrajectoryInfo(player: PlayerEntity, item: Item, alwaysShowBow: Boolean): TrajectoryInfo? {
        return when (item) {
            is BowItem -> {
                val useTime = if (alwaysShowBow && player.itemUseTime < 1) 40 else player.itemUseTime
                TrajectoryInfo.bowWithUsageDuration(useTime)?.copy(
                    projectileType = ProjectileType.Arrow
                )
            }

            is CrossbowItem -> TrajectoryInfo.BOW_FULL_PULL.copy(
                projectileType = ProjectileType.Arrow
            )

            is FishingRodItem -> TrajectoryInfo.FISHING_ROD.copy(
                projectileType = ProjectileType.FishingBobber
            )

            is ThrowablePotionItem -> TrajectoryInfo.POTION.copy(
                projectileType = ProjectileType.Potion
            )

            is TridentItem -> TrajectoryInfo.TRIDENT.copy(
                projectileType = ProjectileType.Trident
            )

            is SnowballItem -> TrajectoryInfo.GENERIC.copy(
                projectileType = ProjectileType.Snowball
            )

            is EnderPearlItem -> TrajectoryInfo.GENERIC.copy(
                projectileType = ProjectileType.EnderPearl
            )

            is EggItem -> TrajectoryInfo.GENERIC.copy(
                projectileType = ProjectileType.Egg
            )

            is ExperienceBottleItem -> TrajectoryInfo.EXP_BOTTLE.copy(
                projectileType = ProjectileType.ExpBottle
            )

            is FireChargeItem -> TrajectoryInfo.FIREBALL.copy(
                projectileType = ProjectileType.Fireball
            )

            is WindChargeItem -> TrajectoryInfo.WIND_CHARGE.copy(
                projectileType = ProjectileType.WindCharge
            )

            else -> null
        }
    }


    @JvmStatic
    fun getRenderTrajectoryInfoForOtherEntity(entity: Entity): TrajectoryInfo? {
        return when (entity) {
            is ArrowEntity -> if (!entity.isInGround()) {
                TrajectoryInfo(
                    0.05, 0.3,
                    projectileType = ProjectileType.Arrow)
            } else {
                null
            }

            is PotionEntity -> TrajectoryInfo.POTION.copy(
                projectileType = ProjectileType.Potion
            )

            is TridentEntity -> {
                if (!entity.isInGround()) {
                    TrajectoryInfo.TRIDENT.copy(
                        projectileType = ProjectileType.Trident
                    )
                } else {
                    null
                }
            }

            is EnderPearlEntity -> TrajectoryInfo.GENERIC.copy(
                projectileType = ProjectileType.EnderPearl
            )

            is SnowballEntity -> TrajectoryInfo.GENERIC.copy(
                projectileType = ProjectileType.Snowball
            )

            is EggEntity -> TrajectoryInfo.GENERIC.copy(
                projectileType = ProjectileType.Egg
            )

            is ExperienceBottleEntity -> TrajectoryInfo.EXP_BOTTLE.copy(
                projectileType = ProjectileType.ExpBottle
            )

            is AbstractFireballEntity -> TrajectoryInfo.FIREBALL.copy(
                projectileType = ProjectileType.Fireball
            )

            is FishingBobberEntity -> TrajectoryInfo.FISHING_ROD.copy(
                projectileType = ProjectileType.FishingBobber
            )

            is WindChargeEntity -> TrajectoryInfo.WIND_CHARGE.copy(
                projectileType = ProjectileType.WindCharge
            )

            else -> null
        }
    }
}

fun TrajectoryInfo.categorize(): ProjectileType? = this.projectileType

// Determine if first-tick position skip is needed
fun TrajectoryInfo.requiresInitialTickCorrection(): Boolean =
    projectileType in setOf(
        ProjectileType.EnderPearl,
        ProjectileType.Snowball,
        ProjectileType.Egg,
        ProjectileType.Potion,
        ProjectileType.ExpBottle,
        ProjectileType.FishingBobber,
    )

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
    val projectileType: ProjectileType? = null
) {
    @JvmOverloads
    fun hitbox(center: Vec3d = Vec3d.ZERO): Box = Box(
        center.x - hitboxRadius,
        center.y - hitboxRadius,
        center.z - hitboxRadius,
        center.x + hitboxRadius,
        center.y + hitboxRadius,
        center.z + hitboxRadius,
    )

    companion object {
        @JvmField
        val GENERIC = TrajectoryInfo(0.03, 0.25)

        @JvmField
        val PERSISTENT = TrajectoryInfo(0.05, 0.5)

        @JvmField
        val POTION = GENERIC.copy(gravity = 0.05, initialVelocity = 0.5, roll = -20.0F)

        @JvmField
        val EXP_BOTTLE = POTION.copy(gravity = 0.07, initialVelocity = 0.7)

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
        fun bowWithUsageDuration(usageDurationTicks: Int = player.itemUseTime): TrajectoryInfo? {
            // Calculate the power of bow
            var power = usageDurationTicks / 20f
            power = (power * power + power * 2F) / 3F

            if (power < 0.1F) {
                return null
            }

            val v0 = power.coerceAtMost(1.0F) * BOW_FULL_PULL.initialVelocity

            return BOW_FULL_PULL.copy(initialVelocity = v0)
        }

    }
}
