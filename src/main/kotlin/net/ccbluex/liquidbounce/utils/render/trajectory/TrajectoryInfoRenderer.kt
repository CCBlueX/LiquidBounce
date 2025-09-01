package net.ccbluex.liquidbounce.utils.render.trajectory

import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.jvm.optionals.getOrNull
import kotlin.math.cos
import kotlin.math.sin

class TrajectoryInfoRenderer(
    private val owner: Entity,
    private var velocity: Vec3d,
    private var pos: Vec3d,
    private val trajectoryInfo: TrajectoryInfo,
    /**
     * The visualization should be what-you-see-is-what-you-get, so we use the actual current position of the player
     * for simulation. Since the trajectory line should follow the player smoothly, we offset it by some amount.
     */
    private val renderOffset: Vec3d
) {
    companion object {
        @JvmStatic
        fun getHypotheticalTrajectory(
            entity: Entity,
            trajectoryInfo: TrajectoryInfo,
            rotation: Rotation,
            partialTicks: Float = mc.renderTickCounter.getTickDelta(true)
        ): TrajectoryInfoRenderer {
            val yawRadians = rotation.yaw / 180f * Math.PI.toFloat()
            val pitchRadians = rotation.pitch / 180f * Math.PI.toFloat()

            val interpolatedOffset = entity.interpolateCurrentPosition(partialTicks) - entity.pos

            val pos = Vec3d(
                entity.x,
                entity.eyeY - 0.10000000149011612,
                entity.z
            )

            var velocity = Vec3d(
                -sin(yawRadians) * cos(pitchRadians).toDouble(),
                -sin((rotation.pitch + trajectoryInfo.roll).toRadians()).toDouble(),
                cos(yawRadians) * cos(pitchRadians).toDouble()
            ).normalize() * trajectoryInfo.initialVelocity

            if (trajectoryInfo.copiesPlayerVelocity) {
                velocity += Vec3d(
                    entity.velocity.x,
                    if (entity.isOnGround) 0.0 else entity.velocity.y,
                    entity.velocity.z
                )
            }

            return TrajectoryInfoRenderer(
                owner = entity,
                velocity = velocity,
                pos = pos,
                trajectoryInfo = trajectoryInfo,
                renderOffset = interpolatedOffset + Vec3d(-cos(yawRadians) * 0.16, 0.0, -sin(yawRadians) * 0.16)
            )
        }
    }

    private val hitbox = Box.of(
        Vec3d.ZERO,
        trajectoryInfo.hitboxRadius * 2.0,
        trajectoryInfo.hitboxRadius * 2.0,
        trajectoryInfo.hitboxRadius * 2.0
    )

    fun drawTrajectoryForProjectile(
        maxTicks: Int,
        color: Color4b,
        matrixStack: MatrixStack
    ): HitResult? {
        // Start drawing of path
        val positions = mutableListOf<Vec3d>()

        val hitResult = runSimulation(maxTicks, positions)

        renderEnvironmentForWorld(matrixStack) {
            withColor(color) {
                drawLineStrip(positions.map { relativeToCamera(it + renderOffset).toVec3() })
            }
        }

        return hitResult
    }

    fun runSimulation(
        maxTicks: Int,
        outPositions: MutableList<Vec3d> = mutableListOf(),
    ): HitResult? {
        var currTicks = 0

        for (ignored in 0 until maxTicks) {
            if (pos.y < world.bottomY) {
                break
            }

            val prevPos = pos

            pos += velocity

            val hitResult = checkForHits(prevPos, pos)

            if (hitResult != null) {
                hitResult.second?.let {
                    outPositions += it
                }

                return hitResult.first
            }

            val blockState = world.getBlockState(BlockPos.ofFloored(pos))

            // Check is next position water
            val drag = if (!blockState.fluidState.isEmpty) {
                trajectoryInfo.dragInWater
            } else {
                trajectoryInfo.drag
            }

            velocity *= drag
            velocity -= Vec3d(0.0, trajectoryInfo.gravity, 0.0)

            // Draw path
            outPositions += pos

            currTicks++
        }

        return null
    }

    private fun checkForHits(
        posBefore: Vec3d,
        posAfter: Vec3d
    ): Pair<HitResult, Vec3d?>? {
        val blockHitResult = world.raycast(
            RaycastContext(
                posBefore,
                posAfter,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                owner
            )
        )
        if (blockHitResult != null && blockHitResult.type != HitResult.Type.MISS) {
            return blockHitResult to blockHitResult.pos
        }

        val entityHitResult = ProjectileUtil.getEntityCollision(
            world,
            owner,
            posBefore,
            posAfter,
            hitbox.offset(pos).stretch(velocity).expand(1.0)
        ) {
            val canCollide = !it.isSpectator && it.isAlive
            val shouldCollide = it.canHit() || owner != mc.player && it == mc.player

            return@getEntityCollision canCollide && shouldCollide && !owner.isConnectedThroughVehicle(it)
        }

        return if (entityHitResult != null && entityHitResult.type != HitResult.Type.MISS) {
            val hitPos = entityHitResult.entity.box.expand(trajectoryInfo.hitboxRadius).raycast(posBefore, posAfter)

            entityHitResult to hitPos.getOrNull()
        } else {
            null
        }
    }
}
