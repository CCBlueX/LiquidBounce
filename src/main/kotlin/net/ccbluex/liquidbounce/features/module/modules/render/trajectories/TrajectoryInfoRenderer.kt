package net.ccbluex.liquidbounce.features.module.modules.render.trajectories

import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.entity.projectile.thrown.EggEntity
import net.minecraft.entity.projectile.thrown.EnderPearlEntity
import net.minecraft.entity.projectile.thrown.PotionEntity
import net.minecraft.entity.projectile.thrown.SnowballEntity
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.jvm.optionals.getOrNull

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

    private fun runSimulation(
        maxTicks: Int,
        outPositions: MutableList<Vec3d>,
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
