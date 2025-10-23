/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package net.ccbluex.liquidbounce.utils.render.trajectory

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam
import net.ccbluex.liquidbounce.features.module.modules.render.trajectories.ModuleTrajectories
import net.ccbluex.liquidbounce.features.module.modules.render.trajectories.ModuleTrajectories.maxSimulatedTicks
import net.ccbluex.liquidbounce.features.module.modules.render.trajectories.ModuleTrajectories.simulationResults
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.math.*
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer.Companion.getHypotheticalTrajectory
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import org.joml.Matrix4f
import org.joml.Quaternionf
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

            //In Freeze, this momentum is the residual value before freezing.
            if (trajectoryInfo.copiesPlayerVelocity && !ModuleFreeze.running) {
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
        fun tickVelocity() {
            val blockState = world.getBlockState(mutableBlockPos.set(pos.x, pos.y, pos.z))
            // Check is next position water
            val drag = if (!blockState.fluidState.isEmpty) {
                trajectoryInfo.dragInWater
            } else {
                trajectoryInfo.drag
            }

            velocity.scale(drag).move(y = -trajectoryInfo.gravity)
        }

        val positions = mutableListOf<Vec3d>()

        // Apply first-tick physics to velocity only for affected projectiles
        if (trajectoryInfo.requiresInitialTickCorrection()) {
            tickVelocity()
        }

        positions += pos.copy()
        val prevPos = pos.copy()
        // Now start normal simulation, starting from currTicks = 1
        var currTicks = if (trajectoryInfo.requiresInitialTickCorrection()) {
            1
        } else {
            0
        }

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

            tickVelocity()

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
        positions: List<Vec3d>,
        color: Color4b,
        matrixStack: MatrixStack,
    ) {
        if (positions.size < 2) return

        RenderSystem.depthMask(false)
        renderEnvironmentForWorld(matrixStack) {
            startBatch()

            val endAlpha = color.a.toFloat() / 255f
            val startAlpha = 0f
            val transitionSegments = 3

            drawCustomMesh(VertexFormat.DrawMode.DEBUG_LINES, VertexInputType.PosColor) { matrix ->
                for (i in 0 until positions.size - 1) {
                    val pos1 = relativeToCamera(positions[i] + renderOffset).toVec3()
                    val pos2 = relativeToCamera(positions[i + 1] + renderOffset).toVec3()

                    val alpha1 = if (i < transitionSegments) {
                        startAlpha + (endAlpha - startAlpha) * (i / transitionSegments.toFloat())
                    } else endAlpha
                    val alpha2 = if (i + 1 < transitionSegments) {
                        startAlpha + (endAlpha - startAlpha) * ((i + 1) / transitionSegments.toFloat())
                    } else endAlpha

                    val r = color.r / 255f
                    val g = color.g / 255f
                    val b = color.b / 255f

                    vertex(matrix, pos1.x, pos1.y, pos1.z).color(
                        r, g, b, alpha1)
                    vertex(matrix, pos2.x, pos2.y, pos2.z).color(
                        r, g, b, alpha2)
                }
            }

            commitBatch()
        }
        RenderSystem.depthMask(true)
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
        startBatch()
        for (entity in entities) {
            if (entity === player) {
                continue
            }

            val pos = entity.interpolateCurrentPosition(partialTicks)

            withPositionRelativeToCamera(pos) {
                drawBox(
                    entity
                        .getDimensions(entity.pose)!!
                        .getBoxAt(Vec3d.ZERO),
                    entityHitColor,
                )
            }
        }
        commitBatch()
    }
}

private fun renderHitBlockFace(
    matrixStack: MatrixStack,
    blockHitResult: BlockHitResult,
    color: Color4b,
    event: WorldRenderEvent,
    renderer: TrajectoryInfoRenderer
) {
    val points = listOf(
        Vec3(0.0, 0.001, -0.5),
        Vec3(0.5, 0.001, 0.0),
        Vec3(0.0, 0.001, 0.5),
        Vec3(-0.5, 0.001, 0.0)
    )
    val side = blockHitResult.side
    val hitPos = blockHitResult.pos
    val partialTicks = event.partialTicks
    var interpolatedHitPos = hitPos
    if (renderer.type == TrajectoryInfoRenderer.Type.HYPOTHETICAL && renderer.owner === player) {
        val playerPrevPos = Vec3d(player.prevX, player.prevY, player.prevZ)
        val playerInterpolated = player.interpolateCurrentPosition(partialTicks)
        val posOffset = playerInterpolated.subtract(playerPrevPos)
        interpolatedHitPos = hitPos.add(posOffset)
    }

    fun getQuaternionForSide(side: Direction): Quaternionf = when (side) {
        Direction.UP -> Quaternionf().identity()
        Direction.DOWN -> Quaternionf().rotationX(Math.PI.toFloat())
        Direction.NORTH -> Quaternionf().rotationX(-Math.PI.toFloat() / 2)
        Direction.SOUTH -> Quaternionf().rotationX(Math.PI.toFloat() / 2)
        Direction.WEST -> Quaternionf().rotationZ(Math.PI.toFloat() / 2)
        Direction.EAST -> Quaternionf().rotationZ(-Math.PI.toFloat() / 2)
    }

    renderEnvironmentForWorld(matrixStack) {
        matrixStack.withPush {
            val quat = getQuaternionForSide(side)
            val fillColor = color.alpha(70)
            val outlineColor = color.alpha(150)

            val drawQuad: (Matrix4f) -> Unit = { matrix ->
                RenderSystem.disableCull()
                drawCustomMesh(VertexFormat.DrawMode.QUADS, VertexInputType.PosColor) { m ->
                    points.forEach { (x, y, z) -> vertex(m, x, y, z).color(fillColor.toARGB()) }
                }
                RenderSystem.enableCull()

                drawCustomMesh(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexInputType.PosColor) { m ->
                    points.forEach { (x, y, z) -> vertex(m, x, y, z).color(outlineColor.toARGB()) }
                    val first = points[0]
                    vertex(m, first.x, first.y, first.z).color(outlineColor.toARGB())
                }

            }

            val relHit = relativeToCamera(interpolatedHitPos)

            translate(relHit.x, relHit.y, relHit.z)
            multiply(quat)
            drawQuad(matrixStack.peek().positionMatrix)
        }
    }
}


fun renderSimulationResult(
    renderer: TrajectoryInfoRenderer,
    simulationResult: TrajectoryInfoRenderer.SimulationResult,
    type: ModuleTrajectories.ProjectileType,
    trajectoryInfo: TrajectoryInfo,
    event: WorldRenderEvent
) {
    val (landingPosition, positions) = simulationResult

    renderer.drawTrajectoryForProjectile(positions, type.color, event.matrixStack)

    when (landingPosition) {
        null -> {}
        is BlockHitResult -> if (type.blockHitESP) {
            renderHitBlockFace(
                event.matrixStack, landingPosition, type.color, event, renderer
            )
        }

        is EntityHitResult -> if (type.entityHitESP) {
            val entities = listOf(landingPosition.entity)
            drawHitEntities(
                event.matrixStack, type.color, entities, event.partialTicks
            )
        }

        else -> error("Unexpected HitResult type: ${landingPosition::class.java.name}")
    }

    if (trajectoryInfo == TrajectoryInfo.POTION && type.entityHitESP) {
        landingPosition?.let {
            drawSplashPotionTargets(
                it.pos, trajectoryInfo, event, type.color
            )
        }
    }
}

fun drawHypotheticalTrajectoryWithSimulation(
    playerEntity: PlayerEntity,
    event: WorldRenderEvent,
    shouldRender: Boolean,
    alwaysShowBow: Boolean
) {
    val trajectoryInfo = playerEntity.handItems.firstNotNullOfOrNull {
        TrajectoryData.getRenderedTrajectoryInfo(playerEntity, it.item, alwaysShowBow)
    } ?: return

    val type = trajectoryInfo.categorize() ?: return
    if (!type.enabled) return

    val rotation = if (playerEntity === player) {
        if (ModuleFreeCam.running) {
            RotationManager.serverRotation
        } else {
            RotationManager.activeRotationTarget?.rotation
                ?: RotationManager.currentRotation
                ?: playerEntity.rotation
        }
    } else {
        playerEntity.rotation
    }

    val renderer = getHypotheticalTrajectory(
        entity = playerEntity,
        trajectoryInfo = trajectoryInfo,
        rotation = rotation,
        partialTicks = event.partialTicks
    )

    val simulationResult = renderer.runSimulation(maxSimulatedTicks)
    simulationResults += renderer to simulationResult

    if (shouldRender) {
        renderSimulationResult(
            renderer, simulationResult, type, trajectoryInfo, event
        )
    }
}
