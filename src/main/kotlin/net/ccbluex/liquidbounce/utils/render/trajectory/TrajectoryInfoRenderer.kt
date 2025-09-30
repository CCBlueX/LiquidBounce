package net.ccbluex.liquidbounce.utils.render.trajectory

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.drawSideBox
import net.ccbluex.liquidbounce.render.drawSolidBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.move
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.scale
import net.ccbluex.liquidbounce.utils.math.set
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.block.ShapeContext
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.jvm.optionals.getOrNull
import kotlin.math.cos
import kotlin.math.sin

class TrajectoryInfoRenderer(
    val owner: Entity,
    velocity: Vec3d,
    pos: Vec3d,
    val trajectoryInfo: TrajectoryInfo,
    /**
     * Only used for rendering. No effect on simulation.
     */
    val type: Type,
    /**
     * The visualization should be what-you-see-is-what-you-get, so we use the actual current position of the player
     * for simulation. Since the trajectory line should follow the player smoothly, we offset it by some amount.
     */
    private val renderOffset: Vec3d
) {
    enum class Type {
        /**
         * From the entity holding items.
         *
         * @see [getHypotheticalTrajectory]
         */
        HYPOTHETICAL,

        /**
         * From a moving entity, such as [net.minecraft.entity.projectile.ProjectileEntity].
         */
        REAL,
    }

    companion object {
        @JvmStatic
        @JvmOverloads
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

            val velocity = Vec3d(
                -sin(yawRadians) * cos(pitchRadians).toDouble(),
                -sin((rotation.pitch + trajectoryInfo.roll).toRadians()).toDouble(),
                cos(yawRadians) * cos(pitchRadians).toDouble()
            ).normalize().scale(trajectoryInfo.initialVelocity)

            if (trajectoryInfo.copiesPlayerVelocity) {
                velocity.move(
                    x = entity.velocity.x,
                    y = if (entity.isOnGround) 0.0 else entity.velocity.y,
                    z = entity.velocity.z
                )
            }

            return TrajectoryInfoRenderer(
                owner = entity,
                velocity = velocity,
                pos = pos,
                trajectoryInfo = trajectoryInfo,
                type = Type.HYPOTHETICAL,
                renderOffset = interpolatedOffset.add(-cos(yawRadians) * 0.16, 0.0, -sin(yawRadians) * 0.16)
            )
        }
    }

    private val velocity = velocity.copy() // Used as mutable
    private val pos = pos.copy() // Used as mutable

    private val hitbox = trajectoryInfo.hitbox()
    private val mutableBlockPos = BlockPos.Mutable()

    fun runSimulation(
        maxTicks: Int,
    ): SimulationResult {
        val positions = mutableListOf<Vec3d>()
        val prevPos = pos.copy()
        var currTicks = 0

        while (currTicks < maxTicks) {
            if (pos.y < world.bottomY) {
                break
            }

            val hitResult = checkForHits(prevPos.set(pos), pos.move(velocity))

            if (hitResult != null) {
                hitResult.second?.let {
                    positions += it
                }

                return SimulationResult(hitResult.first, positions)
            }

            val blockState = world.getBlockState(mutableBlockPos.set(pos.x, pos.y, pos.z))

            // Check is next position water
            val drag = if (!blockState.fluidState.isEmpty) {
                trajectoryInfo.dragInWater
            } else {
                trajectoryInfo.drag
            }

            velocity.scale(drag)
                .move(y = -trajectoryInfo.gravity)

            // Draw path
            positions += pos.copy()

            currTicks++
        }

        if (positions.isEmpty()) {
            positions += pos
        }
        return SimulationResult(null, positions)
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
            val shouldCollide = it.canHit() || owner !== player && it === player

            return@getEntityCollision canCollide && shouldCollide && !owner.isConnectedThroughVehicle(it)
        }

        return if (entityHitResult != null && entityHitResult.type != HitResult.Type.MISS) {
            val hitPos = entityHitResult.entity.box.expand(trajectoryInfo.hitboxRadius).raycast(posBefore, posAfter)

            entityHitResult to hitPos.getOrNull()
        } else {
            null
        }
    }

    fun drawTrajectoryForProjectile(
        maxTicks: Int,
        event: WorldRenderEvent,
        trajectoryColor: Color4b,
        blockHitColor: Color4b?,
        entityHitColor: Color4b?,
    ): SimulationResult {
        val simulationResult = runSimulation(maxTicks)

        val (landingPosition, positions) = simulationResult

        drawTrajectoryForProjectile(positions, trajectoryColor, event.matrixStack)

        when (landingPosition) {
            null -> return simulationResult
            is BlockHitResult -> if (blockHitColor != null) {
                renderHitBlockFace(event.matrixStack, landingPosition, blockHitColor)
            }
            is EntityHitResult -> if (entityHitColor != null) {
                val entities = listOf(landingPosition.entity)

                drawHitEntities(event.matrixStack, entityHitColor, entities, event.partialTicks)
            }
            else -> error("Unexpected HitResult type: ${landingPosition::class.java.name}")
        }

        if (trajectoryInfo == TrajectoryInfo.POTION && entityHitColor != null) {
            drawSplashPotionTargets(landingPosition.pos, trajectoryInfo, event, entityHitColor)
        }

        return simulationResult
    }

    private fun drawTrajectoryForProjectile(
        positions: List<Vec3d>,
        color: Color4b,
        matrixStack: MatrixStack,
    ) {
        renderEnvironmentForWorld(matrixStack) {
            withColor(color) {
                drawLineStrip(positions = positions.mapToArray { relativeToCamera(it + renderOffset).toVec3() })
            }
        }
    }

    @JvmRecord
    data class SimulationResult(
        val hitResult: HitResult?,
        val positions: List<Vec3d>,
    )
}

private fun drawSplashPotionTargets(
    landingPosition: Vec3d,
    trajectoryInfo: TrajectoryInfo,
    event: WorldRenderEvent,
    entityHitColor: Color4b,
) {
    val box: Box = trajectoryInfo.hitbox(landingPosition).expand(4.0, 2.0, 4.0)

    val hitTargets =
        world.getNonSpectatingEntities(LivingEntity::class.java, box)
            .takeWhile { it.squaredDistanceTo(landingPosition) <= 16.0 }
            .filter { it.isAffectedBySplashPotions }

    drawHitEntities(event.matrixStack, entityHitColor, hitTargets, event.partialTicks)
}

private fun drawHitEntities(
    matrixStack: MatrixStack,
    entityHitColor: Color4b,
    entities: List<Entity>,
    partialTicks: Float
) {
    renderEnvironmentForWorld(matrixStack) {
        withColor(entityHitColor) {
            for (entity in entities) {
                if (entity === player) {
                    continue
                }

                val pos = entity.interpolateCurrentPosition(partialTicks)

                withPositionRelativeToCamera(pos) {
                    drawSolidBox(
                        entity
                            .getDimensions(entity.pose)!!
                            .getBoxAt(Vec3d.ZERO)
                    )
                }
            }
        }

    }
}

private fun renderHitBlockFace(matrixStack: MatrixStack, blockHitResult: BlockHitResult, color: Color4b) {
    val currPos = blockHitResult.blockPos
    val currState = currPos.getState()!!

    val bestBox = currState.getOutlineShape(world, currPos, ShapeContext.of(player)).boundingBoxes
        .filter { blockHitResult.pos in it.expand(0.01, 0.01, 0.01).offset(currPos) }
        .minByOrNull { it.squaredBoxedDistanceTo(blockHitResult.pos) }

    if (bestBox != null) {
        renderEnvironmentForWorld(matrixStack) {
            withPositionRelativeToCamera(currPos) {
                withColor(color) {
                    drawSideBox(bestBox, blockHitResult.side)
                }
            }
        }
    }
}
