/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
 */

package net.ccbluex.liquidbounce.utils.render.trajectory

import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.drawBoxSide
import net.ccbluex.liquidbounce.render.drawLineStripAsLines
import net.ccbluex.liquidbounce.render.engine.type.Color4b
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
import net.ccbluex.liquidbounce.utils.kotlin.subList
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.move
import net.ccbluex.liquidbounce.utils.math.scaleMut
import net.ccbluex.liquidbounce.utils.math.set
import net.ccbluex.liquidbounce.utils.math.withLength
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer.Companion.getHypotheticalTrajectory
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import kotlin.jvm.optionals.getOrNull
import kotlin.math.cos
import kotlin.math.sin

class TrajectoryInfoRenderer @Suppress("LongParameterList") constructor(
    val owner: Entity,
    val icon: ItemStack,
    velocity: Vec3,
    pos: Vec3,
    val trajectoryInfo: TrajectoryInfo,
    val trajectoryType: TrajectoryType,
    /**
     * Only used for rendering. No effect on simulation.
     */
    val type: Type,
    /**
     * The visualization should be what-you-see-is-what-you-get, so we use the actual current position of the player
     * for simulation. Since the trajectory line should follow the player smoothly, we offset it by some amount.
     */
    private val renderOffset: Vec3
) {
    enum class Type {
        /**
         * From the entity holding items.
         *
         * @see [getHypotheticalTrajectory]
         */
        HYPOTHETICAL,

        /**
         * From a moving entity, such as [net.minecraft.world.entity.projectile.Projectile].
         */
        REAL,
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun getHypotheticalTrajectory(
            owner: Entity,
            trajectoryInfo: TrajectoryInfo,
            trajectoryType: TrajectoryType,
            rotation: Rotation,
            icon: ItemStack = ItemStack.EMPTY,
            partialTicks: Float = mc.deltaTracker.getGameTimeDeltaPartialTick(true),
        ): TrajectoryInfoRenderer {
            val yawRadians = rotation.yaw.toRadians()
            val pitchRadians = rotation.pitch.toRadians()

            val interpolatedOffset = owner.interpolateCurrentPosition(partialTicks) - owner.position()

            val pos = Vec3(
                owner.x,
                owner.eyeY - 0.10000000149011612,
                owner.z
            )

            var velocity = Vec3(
                -sin(yawRadians) * cos(pitchRadians).toDouble(),
                -sin((rotation.pitch + trajectoryInfo.roll).toRadians()).toDouble(),
                cos(yawRadians) * cos(pitchRadians).toDouble()
            ).withLength(trajectoryInfo.initialVelocity)

            //In Freeze, this momentum is the residual value before freezing.
            if (trajectoryInfo.copiesPlayerVelocity && !ModuleFreeze.running) {
                velocity = velocity.add(
                    owner.deltaMovement.x,
                    if (owner.onGround()) 0.0 else owner.deltaMovement.y,
                    owner.deltaMovement.z
                )
            }

            return TrajectoryInfoRenderer(
                owner = owner,
                icon = icon,
                velocity = velocity,
                pos = pos,
                trajectoryInfo = trajectoryInfo,
                trajectoryType = trajectoryType,
                type = Type.HYPOTHETICAL,
                renderOffset = interpolatedOffset.add(-cos(yawRadians) * 0.16, 0.0, -sin(yawRadians) * 0.16)
            )
        }
    }

    private val velocity = velocity.copy() // Used as mutable
    private val pos = pos.copy() // Used as mutable

    private val hitbox = trajectoryInfo.hitbox()
    private val mutableBlockPos = BlockPos.MutableBlockPos()

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

            velocity.scaleMut(drag).move(y = -trajectoryInfo.gravity)
        }

        val positions = mutableListOf<Vec3>()
        val requiresInitialTickCorrection = this.trajectoryType.requiresInitialTickCorrection

        // Apply first-tick physics to velocity only, mimicking server spawn reset
        if (requiresInitialTickCorrection) {
            tickVelocity()
        }

        // Now start normal simulation, starting from currTicks = 1
        val prevPos = pos.copy()
        var currTicks = if (requiresInitialTickCorrection) 1 else 0

        while (currTicks < maxTicks) {
            if (pos.y < world.minY) {
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
        posBefore: Vec3,
        posAfter: Vec3
    ): Pair<HitResult, Vec3?>? {
        val blockHitResult = world.clip(
            ClipContext(
                posBefore,
                posAfter,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                owner
            )
        )
        if (blockHitResult.type != HitResult.Type.MISS) {
            return blockHitResult to blockHitResult.location
        }

        val entityHitResult = ProjectileUtil.getEntityHitResult(
            world,
            owner,
            posBefore,
            posAfter,
            hitbox.move(pos).expandTowards(velocity).inflate(1.0),
            {
                val canCollide = !it.isSpectator && it.isAlive
                val shouldCollide = it.isPickable || owner !== player && it === player

                return@getEntityHitResult canCollide && shouldCollide && !owner.isPassengerOfSameVehicle(it)
            },
            if (owner is Projectile) ProjectileUtil.computeMargin(owner) else 0f,
        )

        return if (entityHitResult != null && entityHitResult.type != HitResult.Type.MISS) {
            val hitPos = entityHitResult.entity.box.inflate(trajectoryInfo.hitboxRadius).clip(posBefore, posAfter)

            entityHitResult to hitPos.getOrNull()
        } else {
            null
        }
    }

    context(env: WorldRenderEnvironment)
    fun drawTrajectoryForProjectile(
        maxTicks: Int,
        partialTicks: Float,
        trajectoryColor: Color4b,
        blockHitColor: Color4b?,
        entityHitColor: Color4b?,
    ): SimulationResult {
        val simulationResult = runSimulation(maxTicks)

        val (landingPosition, positions) = simulationResult

        env.drawTrajectoryForProjectile(positions, trajectoryColor.argb)

        when (landingPosition) {
            null -> return simulationResult
            is BlockHitResult -> if (blockHitColor != null) {
                env.renderHitBlockFace(landingPosition, blockHitColor)
            }
            is EntityHitResult -> if (entityHitColor != null) {
                val entities = listOf(landingPosition.entity)

                env.drawHitEntities(entityHitColor, entities, partialTicks)
            }
            else -> error("Unexpected HitResult type: ${landingPosition::class.java.name}")
        }

        if (trajectoryInfo == TrajectoryInfo.POTION && entityHitColor != null) {
            env.drawSplashPotionTargets(landingPosition.location, trajectoryInfo, partialTicks, entityHitColor)
        }

        return simulationResult
    }

    private fun WorldRenderEnvironment.drawTrajectoryForProjectile(positions: List<Vec3>, argb: Int) {
        // Don't use LineStrip because in batch mode
        matrixStack.pushPose()
        matrixStack.translate(renderOffset - camera.position())
        drawLineStripAsLines(argb, if (positions.size and 1 != 0) positions.subList(1) else positions)
        matrixStack.popPose()
    }

    @JvmRecord
    data class SimulationResult(
        val hitResult: HitResult?,
        val positions: List<Vec3>,
    )
}

private fun WorldRenderEnvironment.drawSplashPotionTargets(
    landingPosition: Vec3,
    trajectoryInfo: TrajectoryInfo,
    partialTicks: Float,
    entityHitColor: Color4b,
) {
    val box: AABB = trajectoryInfo.hitbox(landingPosition).inflate(4.0, 2.0, 4.0)

    val hitTargets =
        world.getEntitiesOfClass(LivingEntity::class.java, box) {
            it.distanceToSqr(landingPosition) <= 16.0 && it.isAffectedByPotions
        }

    drawHitEntities(entityHitColor, hitTargets, partialTicks)
}

private fun WorldRenderEnvironment.drawHitEntities(
    entityHitColor: Color4b,
    entities: List<Entity>,
    partialTicks: Float
) {
    for (entity in entities) {
        if (entity === player) {
            continue
        }

        val pos = entity.interpolateCurrentPosition(partialTicks)

        withPositionRelativeToCamera(pos) {
            drawBox(
                entity
                    .getDimensions(entity.pose)
                    .makeBoundingBox(Vec3.ZERO),
                entityHitColor,
            )
        }
    }
}

private fun WorldRenderEnvironment.renderHitBlockFace(blockHitResult: BlockHitResult, color: Color4b) {
    val currPos = blockHitResult.blockPos
    val currState = currPos.getState()!!

    val bestBox = currState.getShape(world, currPos, CollisionContext.of(player)).toAabbs()
        .filter { blockHitResult.location in it.inflate(0.01).move(currPos) }
        .minByOrNull { it.squaredBoxedDistanceTo(blockHitResult.location) }

    if (bestBox != null) {
        withPositionRelativeToCamera(currPos) {
            drawBoxSide(
                bestBox,
                side = blockHitResult.direction,
                faceColor = color,
            )
        }
    }
}
