@file:Suppress("unused","all")
package net.ccbluex.liquidbounce.utils.aiming.projectiles

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.entity.EntityDimensions
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import kotlin.math.cos
import kotlin.math.sin
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.entity.projectile.ProjectileUtil
import kotlin.math.max

object SituationalArrowAngleCalculator : ProjectileAngleCalculator() {

    override fun calculateAngleFor(
        projectileInfo: TrajectoryInfo,
        sourcePos: Vec3d,
        targetPosFunction: PositionExtrapolation,
        targetShape: EntityDimensions
    ): Rotation? {
        val basePos = targetPosFunction.getPositionInTicks(0.0)
        val candidate = if (basePos.distanceTo(sourcePos) < 5.0) {
            PolynomialProjectileAngleCalculator.calculateAngleFor(projectileInfo, sourcePos, targetPosFunction, targetShape)
        } else {
            CydhranianProjectileAngleCalculator.calculateAngleFor(projectileInfo, sourcePos, targetPosFunction, targetShape)
        } ?: return null

        val world = player.world

        return if (isTrajectoryClear(world,
                sourcePos,
                candidate.pitch,
                candidate.yaw, projectileInfo, targetPosFunction, targetShape)) {
            candidate
        } else {
            null
        }
    }

    fun isTrajectoryClear(
        world: World,
        startPos: Vec3d,
        pitch: Float,
        yaw: Float,
        projectileInfo: TrajectoryInfo,
        targetPosFunction: PositionExtrapolation,
        targetShape: EntityDimensions
    ): Boolean {
        val clientWorld = world as? ClientWorld ?: return false

        var pos = startPos
        var velocity = calculateVelocityVector(pitch, yaw, projectileInfo.initialVelocity)
        val arrowEntity = ArrowEntity(clientWorld, pos.x, pos.y, pos.z, ItemStack(Items.ARROW), null)

        val maxSteps = 300
        repeat(maxSteps) { tick ->
            val newPos = pos.add(velocity)

            val predictedTargetPos = targetPosFunction.getPositionInTicks(tick.toDouble())
            val currentTargetBox: Box = targetShape.getBoxAt(predictedTargetPos)

            val size = max(0.3, projectileInfo.hitboxRadius)
            val entityHit = ProjectileUtil.getEntityCollision(
                clientWorld,
                arrowEntity,
                pos,
                newPos,
                Box(-size, -size, -size, size, size, size)
                    .offset(pos)
                    .stretch(newPos.subtract(pos))
                    .expand(1.0)
            ) { ent: Entity ->
                val canBeHit = !ent.isSpectator && ent.isAlive
                if (!canBeHit) return@getEntityCollision false
                if (arrowEntity.isConnectedThroughVehicle(ent)) return@getEntityCollision false
                return@getEntityCollision true
            }

            if (entityHit != null && entityHit.type != HitResult.Type.MISS) {
                val hitEntity = entityHit.entity

                return hitEntity.boundingBox.intersects(currentTargetBox)
            }


            val blockHit = clientWorld.raycast(
                RaycastContext(
                    pos,
                    newPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    arrowEntity
                )
            )
            if (blockHit != null && blockHit.type != HitResult.Type.MISS) {
                return false
            }

            if (currentTargetBox.raycast(pos, newPos).isPresent) {
                return true
            }

            pos = newPos
            velocity = velocity.multiply(projectileInfo.drag)
            velocity = velocity.add(0.0, -projectileInfo.gravity, 0.0)
            arrowEntity.setPosition(pos.x, pos.y, pos.z)
        }

        return false
    }


    fun calculateVelocityVector(pitch: Float, yaw: Float, initialVelocity: Double): Vec3d {
        val pitchRad = Math.toRadians(pitch.toDouble())
        val yawRad = Math.toRadians(yaw.toDouble())

        val x = -sin(yawRad) * cos(pitchRad)
        val y = -sin(pitchRad)
        val z = cos(yawRad) * cos(pitchRad)

        return Vec3d(x * initialVelocity, y * initialVelocity, z * initialVelocity)
    }
}
