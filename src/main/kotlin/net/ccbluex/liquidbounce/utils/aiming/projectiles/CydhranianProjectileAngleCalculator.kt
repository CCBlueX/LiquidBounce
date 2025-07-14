package net.ccbluex.liquidbounce.utils.aiming.projectiles

import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleProjectileAimbot
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.kotlin.component1
import net.ccbluex.liquidbounce.utils.kotlin.component2
import net.ccbluex.liquidbounce.utils.math.findFunctionMinimumByBisect
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.entity.EntityDimensions
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round

/**
 * Implements the angle calculator described by Cydhra
 * ([see here](https://gitlab.com/Cydhra/Vibrant/-/blob/master/doc/main.pdf)).
 *
 * This is currently used as the flagship implementation. When the distance between the source and target pos is low,
 * this implementation often malfunctions. Use a backup calculator for low distances instead.
 */
object CydhranianProjectileAngleCalculator: ProjectileAngleCalculator() {
    /**
     * @param sourcePos the position the projectile originates from (usually the player's eyePos)
     */
    override fun calculateAngleFor(
        projectileInfo: TrajectoryInfo,
        sourcePos: Vec3d,
        targetPosFunction: PositionExtrapolation,
        targetShape: EntityDimensions
    ): Rotation? {
        val calculatedLookVec = predictArrowDirection(projectileInfo, sourcePos, targetShape, targetPosFunction)

        return calculatedLookVec?.let(Rotation::fromRotationVec)
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

    /**
     * Calculates how long the arrow would fly when hitting the target (see paper)
     */
    private fun calculatePossibleTravelTimeToTarget(
        trajectoryInfo: TrajectoryInfo,
        playerHeadPosition: Vec3d,
        positionFunction: PositionExtrapolation,
        defaultBoxOffset: Vec3d,
    ): Double? {
        val distance = positionFunction.getPositionInTicks(0.0).subtract(playerHeadPosition).length()

        // The max travel time which we expect the arrow to fly. The bisect function expects only
        // one minimum (= one solution). In most cases, there are actually two solutions. We don't want the second
        // solution as involves longer flight times than the first solution. This maximum guarantees that the found
        // solution may only take 75% longer than the straight line to the target.
        val maxTravelTime = distance / trajectoryInfo.initialVelocity * 1.75

        // Calculate how long the arrow would need to travel to hit the entity with the given position function.
        val (ticks, delta) = findFunctionMinimumByBisect(0.0, maxTravelTime) { ticks ->
            val newLimit = getDirectionByTime(
                trajectoryInfo,
                enemyPosition = positionFunction.getPositionInTicks(ticks).add(defaultBoxOffset),
                playerHeadPosition = playerHeadPosition,
                time = ticks
            )

            abs(newLimit.length() - 1)
        }

        // If the length returned rotation is not close to 1.0, return (-> see document)
        if (delta > 1E-1) {
            return null
        }

        return ticks
    }

    private fun predictArrowDirection(
        trajectoryInfo: TrajectoryInfo,
        playerHeadPosition: Vec3d,
        targetDimensions: EntityDimensions,
        positionFunction: PositionExtrapolation,
    ): Vec3d? {
        val defaultBoxOffset = Vec3d(
            targetDimensions.width * 0.5,
            targetDimensions.height * 0.5,
            targetDimensions.width * 0.5
        )

        val ticksUntilImpact = calculatePossibleTravelTimeToTarget(
            trajectoryInfo,
            playerHeadPosition,
            positionFunction,
            defaultBoxOffset
        ) ?: return null

        val entityPositionOnImpact = positionFunction.getPositionInTicks(ticksUntilImpact)

        val finalDirection = getDirectionByTime(
            trajectoryInfo,
            enemyPosition = entityPositionOnImpact.add(defaultBoxOffset),
            playerHeadPosition = playerHeadPosition,
            time = ticksUntilImpact
        )

        val directionOnImpact = getVelocityOnImpact(trajectoryInfo, ticksUntilImpact, finalDirection).normalize()

        ModuleDebug.debugGeometry(
            ModuleProjectileAimbot, "inboundDirection", ModuleDebug.DebuggedLineSegment(
                entityPositionOnImpact,
                entityPositionOnImpact.add(directionOnImpact.normalize().multiply(2.0)),
                Color4b.BLUE
            )
        )

        val finalTargetPos = ProjectileTargetPointFinder.findHittablePosition(
            playerHeadPosition,
            directionOnImpact,
            entityPositionOnImpact,
            targetEntityBox = targetDimensions.getBoxAt(entityPositionOnImpact).expand(trajectoryInfo.hitboxRadius)
        ) ?: return null

        return getDirectionByTime(trajectoryInfo, finalTargetPos, playerHeadPosition, round(ticksUntilImpact))
    }


}
