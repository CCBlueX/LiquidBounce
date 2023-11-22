package net.ccbluex.liquidbounce.features.module.modules.combat.autobow

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.client.QuickAccess.player
import net.ccbluex.liquidbounce.utils.client.QuickAccess.world
import net.ccbluex.liquidbounce.features.module.modules.combat.autobow.aimbot.AimPlanner
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.combat.PriorityEnum
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.item.BowItem
import net.minecraft.item.TridentItem
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Bow aimbot automatically aims at enemy targets
 */
object ModuleAutoBowAimbot : ToggleableConfigurable(ModuleAutoBow, "BowAimbot", true) {

    // Target
    val targetTracker = TargetTracker(PriorityEnum.DISTANCE)

    // Rotation
    val rotationConfigurable = RotationsConfigurable()

    val minExpectedPullOption by int("MinExpectedPull", 5, 0..20)

    private val finalMinExpectedPull: Float
        get() {
            if (ModuleAutoBowFastCharge.enabled)
                return 1.0F

            return minExpectedPullOption / 20.0F
        }

    init {
        tree(targetTracker)
        tree(rotationConfigurable)
    }

    val tickRepeatable = repeatable {
        targetTracker.cleanup()

        // Should check if player is using bow
        val activeItem = player.activeItem?.item

        if (activeItem !is BowItem && activeItem !is TridentItem) {
            return@repeatable
        }

        val eyePos = player.eyes

        var rotation: Rotation? = null

        for (enemy in targetTracker.enemies()) {
            val rot = AimPlanner.planShot(
                enemy,
                eyePos,
                finalMinExpectedPull
            ) ?: continue

            targetTracker.lock(enemy)
            rotation = rot
            break
        }

        if (rotation == null) {
            return@repeatable
        }

        RotationManager.aimAt(rotation, configurable = rotationConfigurable)
    }


//    private fun getRotationToTarget(
//        targetPos: Vec3d,
//        eyePos: Vec3d,
//        target: Entity,
//    ): Rotation? {
//        var deltaPos = targetPos.subtract(eyePos)
//        val basePrediction = predictBow(deltaPos, finalMinExpectedPull)
//
//        // Since the target will have moved between the time the arrow starts and hits the target, we need to
//        // account for that
//        val realTravelTime =
//            getTravelTime(
//                basePrediction.travelledOnX,
//                cos(basePrediction.rotation.pitch.toRadians()) * basePrediction.pullProgress * 3.0 * 0.7,
//            )
//
//        if (!realTravelTime.isNaN()) {
//            deltaPos =
//                deltaPos.add(
//                    (target.x - target.prevX) * realTravelTime,
//                    (target.y - target.prevY) * realTravelTime,
//                    (target.z - target.prevZ) * realTravelTime,
//                )
//        }
//
//        val finalPrediction = predictBow(deltaPos, finalMinExpectedPull)
//        val rotation = finalPrediction.rotation
//
//        if (rotation.yaw.isNaN() || rotation.pitch.isNaN()) {
//            return null
//        }
//
//        val pullProgress = finalPrediction.pullProgress
//
//        // If the planned trajectory would end in a block, don't shoot
//        if (doesTrajectoryHitBlock(deltaPos, rotation, pullProgress, eyePos, targetPos)) {
//            return null
//        }
//
//        return rotation
//    }

    /**
     * Returns the highest point of the trajectory (the tip of the parabola)
     */
    fun getHighestPointOfTrajectory(
        deltaPos: Vec3d,
        rotation: Rotation,
        pullProgress: Float,
    ): Vec3d? {
        val v0 = pullProgress * 3.0F * 0.7f

        val v_x = cos(Math.toRadians(rotation.pitch.toDouble())) * v0
        val v_y = sin(Math.toRadians(rotation.pitch.toDouble())) * v0

        val maxX = -(v_x * v_y) / (2.0 * ModuleAutoBow.ACCELERATION) // -(v_x*v_y)/a

        val maxY = -(v_y * v_y) / (4.0 * ModuleAutoBow.ACCELERATION) // -v_y^2/(2*a)

        val xPitch = cos(Math.toRadians(rotation.yaw.toDouble() - 90.0F))
        val zPitch = sin(Math.toRadians(rotation.yaw.toDouble() - 90.0F))

        val vertex =
            if (maxX < 0 && maxX * maxX < deltaPos.x * deltaPos.x + deltaPos.z * deltaPos.z) {
                Vec3d(
                    xPitch * maxX,
                    maxY,
                    zPitch * maxX,
                )
            } else {
                null
            }
        return vertex
    }

    fun getTravelTime(
        dist: Double,
        v0: Double,
    ): Float {
//        return (dist / v0).toFloat()
        return log((v0 / (ln(0.99)) + dist) / (v0 / (ln(0.99))), 0.99).toFloat()
    }

    class BowPredictionResult(val rotation: Rotation, val pullProgress: Float, val travelledOnX: Double)


}
