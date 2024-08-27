package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.trajectories.TrajectoryData
import net.ccbluex.liquidbounce.features.module.modules.render.trajectories.TrajectoryInfo
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.prevPos
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.pow

object ModuleProjectileAimbot : Module("ProjectileAimbot", Category.COMBAT) {
    private val targetTracker = TargetTracker()
    private val rotations = RotationsConfigurable(this)

    init {
        tree(targetTracker)
        tree(rotations)
    }

    private val rep = repeatable {
        val target = targetTracker.enemies().firstOrNull() ?: return@repeatable

        val x = player.handItems.map {
            if (it.item == null) {
                return@map null
            }

            val trajectory = TrajectoryData.getRenderedTrajectoryInfo(
                player,
                it.item,
                true
            ) ?: return@map null

            createRotatation(target, trajectory)
        }.firstOrNull() ?: return@repeatable

        RotationManager.aimAt(x, considerInventory = false, rotations, Priority.IMPORTANT_FOR_USAGE_1, ModuleProjectileAimbot)
    }

    private fun createRotatation(target: LivingEntity, trajectoryInfo: TrajectoryInfo): Rotation? {
        val direction = predictArrowDirection(
            trajectoryInfo,
            target.box.center,
            player.eyePos,
            target.pos.subtract(target.prevPos)
            ) ?: return null

        val hypotenuse = hypot(direction.x, direction.z)

        val yawAtan = atan2(direction.z, direction.x).toFloat()
        val pitchAtan = atan2(direction.y, hypotenuse).toFloat()
        val deg = (180 / Math.PI).toFloat()

        val predictedYaw = yawAtan * deg - 90f
        val predictedPitch = -(pitchAtan * deg)

        return Rotation(predictedYaw, predictedPitch)
    }

    private fun getDirectionByTime(
        trajectoryInfo: TrajectoryInfo,
        enemyPosition: Vec3d,
        playerHeadPosition: Vec3d,
        enemyVelocity: Vec3d,
        time: Double
    ): Vec3d {
        val vA = trajectoryInfo.initialVelocity
        val resistanceFactor = trajectoryInfo.drag
        val g = trajectoryInfo.gravity

        return Vec3d(
            (enemyPosition.x + enemyVelocity.x * time - playerHeadPosition.x) * (resistanceFactor - 1)
                / (vA * (resistanceFactor.pow(time) - 1)),

            (enemyPosition.y + enemyVelocity.y * time - playerHeadPosition.y) * (resistanceFactor - 1)
                / (vA * (resistanceFactor.pow(time) - 1)) + g * (resistanceFactor.pow(time)
                - resistanceFactor * time + time - 1)
                / (vA * (resistanceFactor - 1) * (resistanceFactor.pow(time) - 1)),

            (enemyPosition.z + enemyVelocity.z * time - playerHeadPosition.z) * (resistanceFactor - 1)
                / (vA * (resistanceFactor.pow(time) - 1))
        )
    }

    // fixme Newton and/or bisect
    private fun predictArrowDirection(
        trajectoryInfo: TrajectoryInfo,
        enemyPosition: Vec3d,
        playerHeadPosition: Vec3d,
        enemyVelocity: Vec3d,
    ): Vec3d? {
        for (i in 1 until 180) {
            val newLimit =
                getDirectionByTime(trajectoryInfo, enemyPosition, playerHeadPosition, enemyVelocity, i.toDouble())
            val newLimitLength = newLimit.length()

            if (abs(newLimitLength - 1) < 1.5E-1) {
                println("$i $newLimitLength")
                return newLimit
            }

            // early escape if the length is already out of scope
            if (newLimitLength > 20 && i > 20) {
                break
            }
        }

        return null
    }

}
