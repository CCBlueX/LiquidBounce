package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.projectileaimbot.raytraceFromVirtualEye
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.trajectories.TrajectoryData
import net.ccbluex.liquidbounce.features.module.modules.render.trajectories.TrajectoryInfo
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.prevPos
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round

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

            val lookVec = calculateLookVec(target, trajectory)

            lookVec?.let(::createRotatationForLookVec)
        }.firstOrNull() ?: return@repeatable

        RotationManager.aimAt(
            x,
            considerInventory = false,
            rotations,
            Priority.IMPORTANT_FOR_USAGE_1,
            ModuleProjectileAimbot
        )
    }

    /**
     * Calculates the look vector for the player to hit the target.
     */
    private fun calculateLookVec(
        target: LivingEntity,
        trajectory: TrajectoryInfo
    ): Vec3d? {
        val positionFunction = getPositionFunctionForEntity(target)

        return predictArrowDirection(trajectory, player.eyePos, target.dimensions, positionFunction) ?: return null
    }

    private fun getPositionFunctionForEntity(target: LivingEntity): (Double) -> Vec3d {
        if (target is PlayerEntity) {
            val playerSimulation = PlayerSimulationCache.getSimulationForOtherPlayers(target)

            return { ticks: Double ->
                playerSimulation.getSnapshotAt(round(ticks.coerceAtMost(30.0)).toInt()).pos
            }
        } else {
            val targetBoxCenter = target.pos
            val targetVelocity = target.pos.subtract(target.prevPos)

            return { ticks: Double -> targetBoxCenter.add(targetVelocity.multiply(ticks)) }
        }
    }

    private fun createRotatationForLookVec(direction: Vec3d): Rotation? {
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
        time: Double
    ): Vec3d {
        val vA = trajectoryInfo.initialVelocity
        val resistanceFactor = trajectoryInfo.drag
        val g = trajectoryInfo.gravity

        return Vec3d(
            (enemyPosition.x - playerHeadPosition.x) * (resistanceFactor - 1)
                / (vA * (resistanceFactor.pow(time) - 1)),

            (enemyPosition.y - playerHeadPosition.y) * (resistanceFactor - 1)
                / (vA * (resistanceFactor.pow(time) - 1)) + g * (resistanceFactor.pow(time)
                - resistanceFactor * time + time - 1)
                / (vA * (resistanceFactor - 1) * (resistanceFactor.pow(time) - 1)),

            (enemyPosition.z - playerHeadPosition.z) * (resistanceFactor - 1)
                / (vA * (resistanceFactor.pow(time) - 1))
        )
    }

    private fun getVelocityOnImpact(trajectoryInfo: TrajectoryInfo, ticksPassed: Double, initialDir: Vec3d): Vec3d {
        val d_x = initialDir.x
        val d_y = initialDir.y
        val d_z = initialDir.z

        val r_proj = trajectoryInfo.drag
        val v_proj = trajectoryInfo.initialVelocity
        val g = trajectoryInfo.gravity
        val t = ticksPassed

        val fResistance = r_proj - 1

        return Vec3d(
            (d_x * r_proj.pow(t) * ln(r_proj) * v_proj) / fResistance,
            (d_y * fResistance * r_proj.pow(t) * ln(r_proj) * v_proj - g * (r_proj.pow(t) * ln(r_proj) - r_proj + 1))
                / fResistance.pow(2),
            (d_z * r_proj.pow(t) * ln(r_proj) * v_proj) / fResistance
        )
    }

    private fun predictArrowDirection(
        trajectoryInfo: TrajectoryInfo,
        playerHeadPosition: Vec3d,
        targetDimensions: EntityDimensions,
        entityPositionFunction: (Double) -> Vec3d,
    ): Vec3d? {
        val defaultBoxOffset = Vec3d(targetDimensions.width * 0.5, targetDimensions.height * 0.5, targetDimensions.width * 0.5)
        val distance = entityPositionFunction(0.0).subtract(playerHeadPosition).length()
        val minTravelTime = distance / trajectoryInfo.initialVelocity

        val (ticks, delta) = bisectFindMininum(0.0, minTravelTime * 1.5, { ticks ->
            val newLimit =
                getDirectionByTime(trajectoryInfo, entityPositionFunction(ticks).add(defaultBoxOffset), playerHeadPosition, ticks)

            abs(newLimit.length() - 1)
        })

        if (delta > 1E-1) {
            return null
        }

        val entityPositionOnImpact = entityPositionFunction(ticks)

        val ticksBeforeImpact = ticks

        val finalDirection =
            getDirectionByTime(trajectoryInfo, entityPositionOnImpact.add(defaultBoxOffset), playerHeadPosition, ticksBeforeImpact)

        val directionOnImpact = getVelocityOnImpact(trajectoryInfo, ticksBeforeImpact, finalDirection).normalize()

        ModuleDebug.debugGeometry(ModuleProjectileAimbot, "inboundDirection", ModuleDebug.DebuggedLineSegment(
            entityPositionOnImpact,
            entityPositionOnImpact.add(directionOnImpact.normalize().multiply(2.0)),
            Color4b.BLUE
        ))

        val virtualEyes = playerHeadPosition.add(0.0, directionOnImpact.y * -(playerHeadPosition.distanceTo(entityPositionOnImpact)), 0.0)

        val currTime = System.nanoTime()

        val bestPos = raytraceFromVirtualEye(virtualEyes, EntityDimensions.fixed(0.6f, 1.8f).getBoxAt(entityPositionOnImpact), 5.0) ?: return null

        val rayTraceTime = System.nanoTime() - currTime

        ModuleDebug.debugParameter(ModuleProjectileAimbot, "raytraceTime", String.format("%.2f us", rayTraceTime / 1E3))

        return getDirectionByTime(trajectoryInfo, bestPos, playerHeadPosition, round(ticks))
    }

    /**
     * Finds the minimum between min and max.
     */
    private inline fun bisectFindMininum(
        min: Double,
        max: Double,
        function: (Double) -> Double,
        minDelta: Double = 1E-4
    ): Pair<Double, Double> {
        var lowerBound = min
        var upperBound = max

        var t = 0

        while (upperBound - lowerBound > minDelta) {
            val mid = (lowerBound + upperBound) / 2

            val leftValue = function((lowerBound + mid) / 2)
            val rightValue = function((mid + upperBound) / 2)

            if (leftValue < rightValue) {
                upperBound = mid
            } else {
                lowerBound = mid
            }

            t++
        }

        val x = (lowerBound + upperBound) / 2
        val y = function(x)

        return x to y
    }

}
