package net.ccbluex.liquidbounce.utils.render.trajectory

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.AbstractFireballEntity
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.entity.projectile.TridentEntity
import net.minecraft.entity.projectile.thrown.*
import net.minecraft.item.*

object TrajectoryData {
    fun getRenderedTrajectoryInfo(player: PlayerEntity, item: Item, alwaysShowBow: Boolean): TrajectoryInfo? {
        return when (item) {
            is BowItem -> {
                val useTime = if (alwaysShowBow && player.itemUseTime < 1) {
                    40
                } else {
                    player.itemUseTime
                }

                TrajectoryInfo.bowWithUsageDuration(useTime)
            }
            is CrossbowItem -> TrajectoryInfo.BOW_FULL_PULL
            is FishingRodItem -> TrajectoryInfo.FISHING_ROD
            is ThrowablePotionItem -> TrajectoryInfo.POTION
            is TridentItem -> TrajectoryInfo.TRIDENT
            is SnowballItem -> TrajectoryInfo.GENERIC
            is EnderPearlItem -> TrajectoryInfo.GENERIC
            is EggItem -> TrajectoryInfo.GENERIC
            is ExperienceBottleItem -> TrajectoryInfo.EXP_BOTTLE
            is FireChargeItem -> TrajectoryInfo.FIREBALL
            is WindChargeItem -> TrajectoryInfo.WIND_CHARGE
            else -> null
        }
    }

    fun getColorForEntity(it: Entity): Color4b {
        return when (it) {
            is ArrowEntity -> Color4b(255, 0, 0, 200)
            is EnderPearlEntity -> Color4b(128, 0, 128, 200)
            else -> Color4b(200, 200, 200, 200)
        }
    }

    fun getRenderTrajectoryInfoForOtherEntity(
        entity: Entity,
        activeArrows: Boolean,
        activeOthers: Boolean,
    ): TrajectoryInfo? {
        if (activeArrows && entity is ArrowEntity && !entity.isInGround()) {
            return TrajectoryInfo(0.05, 0.3)
        }
        if (!activeOthers) {
            return null
        }

        return when (entity) {
            is PotionEntity -> TrajectoryInfo.POTION
            is TridentEntity -> {
                if (!entity.isInGround()) {
                    TrajectoryInfo.TRIDENT
                } else {
                    null
                }
            }
            is EnderPearlEntity -> TrajectoryInfo.GENERIC
            is SnowballEntity -> TrajectoryInfo.GENERIC
            is ExperienceBottleEntity -> TrajectoryInfo.EXP_BOTTLE
            is EggEntity -> TrajectoryInfo.GENERIC
            is AbstractFireballEntity -> TrajectoryInfo.FIREBALL
            else -> null
        }
    }
}


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
        fun bowWithUsageDuration(usageDuration: Int = player.itemUseTime): TrajectoryInfo? {
            // Calculate the power of bow
            var power = usageDuration / 20f
            power = (power * power + power * 2F) / 3F

            if (power < 0.1F) {
                return null
            }

            val v0 = power.coerceAtMost(1.0F) * BOW_FULL_PULL.initialVelocity

            return BOW_FULL_PULL.copy(initialVelocity = v0)
        }
    }
}
