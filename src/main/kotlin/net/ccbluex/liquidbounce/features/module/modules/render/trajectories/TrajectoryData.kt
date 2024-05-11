package net.ccbluex.liquidbounce.features.module.modules.render.trajectories

import net.ccbluex.liquidbounce.render.engine.Color4b
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.entity.projectile.TridentEntity
import net.minecraft.entity.projectile.thrown.EggEntity
import net.minecraft.entity.projectile.thrown.EnderPearlEntity
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity
import net.minecraft.entity.projectile.thrown.PotionEntity
import net.minecraft.entity.projectile.thrown.SnowballEntity
import net.minecraft.item.*
import org.joml.Matrix4f

object TrajectoryData {
    fun getRenderedTrajectoryInfo(player: PlayerEntity, item: Item, alwaysShowBow: Boolean): TrajectoryInfo? {
        return when (item) {
            is BowItem -> {
                // Calculate power of bow
                var power = player.itemUseTime / 20f
                power = (power * power + power * 2F) / 3F
                power = if (alwaysShowBow && power == 0.0F) 1.0F else power

                if (power < 0.1F) {
                    return null
                }

                val v0 = power.coerceAtMost(1.0F) * TrajectoryInfo.BOW_FULL_PULL.initialVelocity

                TrajectoryInfo.BOW_FULL_PULL.copy(initialVelocity = v0)
            }

            is CrossbowItem -> TrajectoryInfo.BOW_FULL_PULL
            is FishingRodItem -> TrajectoryInfo.FISHING_ROD
            is ThrowablePotionItem -> TrajectoryInfo.POTION
            is TridentItem -> TrajectoryInfo.TRIDENT
            is SnowballItem -> TrajectoryInfo.GENERIC
            is EnderPearlItem -> TrajectoryInfo.GENERIC
            is EggItem -> TrajectoryInfo.GENERIC
            is ExperienceBottleItem -> TrajectoryInfo.EXP_BOTTLE
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
        if (activeArrows && entity is ArrowEntity && !entity.inGround) {
            return TrajectoryInfo(0.05, 0.3)
        }
        if (!activeOthers) {
            return null
        }

        return when (entity) {
            is PotionEntity -> TrajectoryInfo.POTION
            is TridentEntity -> {
                if (!entity.inGround) {
                    TrajectoryInfo.TRIDENT
                } else {
                    null
                }
            }
            is EnderPearlEntity -> TrajectoryInfo.GENERIC
            is SnowballEntity -> TrajectoryInfo.GENERIC
            is ExperienceBottleEntity -> TrajectoryInfo.EXP_BOTTLE
            is EggEntity -> TrajectoryInfo.GENERIC
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
    val roll: Float = 0.0F
) {
    companion object {
        val GENERIC = TrajectoryInfo(0.03, 0.25)
        private val PERSISTENT = TrajectoryInfo(0.05, 0.5)
        val POTION = GENERIC.copy(gravity = 0.05, initialVelocity = 0.5, roll = -20.0F)
        val EXP_BOTTLE = POTION.copy(initialVelocity = 0.7)
        val FISHING_ROD = GENERIC.copy(gravity = 0.04, drag = 0.92)
        val TRIDENT = PERSISTENT.copy(initialVelocity = 2.5, gravity = 0.05, dragInWater = 0.99)
        val BOW_FULL_PULL = PERSISTENT.copy(initialVelocity = 3.0)
    }
}
