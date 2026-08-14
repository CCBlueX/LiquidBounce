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
package net.ccbluex.liquidbounce.utils.entity

import it.unimi.dsi.fastutil.floats.FloatArraySet
import it.unimi.dsi.fastutil.floats.FloatArrays
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.BlockCollisions
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.EntityCollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.sqrt

@Suppress("LongParameterList", "TooManyFunctions")
class FallingPlayer(
    private val player: LocalPlayer,
    var x: Double,
    var y: Double,
    var z: Double,
    private var motionX: Double,
    private var motionY: Double,
    private var motionZ: Double,
    private val yRot: Float
) {
    companion object {
        private const val SUPPORT_EPSILON = 1.0E-6

        @JvmStatic
        @JvmOverloads
        fun fromPlayer(player: LocalPlayer, movementYaw: Float = player.yRot): FallingPlayer {
            return FallingPlayer(
                player,
                player.x,
                player.y,
                player.z,
                player.deltaMovement.x,
                player.deltaMovement.y,
                player.deltaMovement.z,
                movementYaw
            )
        }
    }

    private var simulatedTicks: Int = 0
    private var boundingBox = player.getDimensions(player.pose).makeBoundingBox(x, y, z)
    private var lastResolvedMovement = Vec3.ZERO

    private fun calculateMovementForTick(rotationVec: Vec3): Vec3 {
        if (player.isFallFlying) {
            calculateElytraTick(rotationVec)
        } else {
            applyAirInput()
        }

        return Vec3(motionX, motionY, motionZ)
    }

    /**
     * Applies the player's air input before movement, matching vanilla's
     * {@code LivingEntity.handleRelativeFrictionAndCalculateMovement()}.
     */
    private fun applyAirInput() {
        val speed = player.speed * 0.1f
        if (speed > 0f) {
            val inputVec = Entity.getInputVector(
                playerMovementInput(),
                speed, yRot
            )
            motionX += inputVec.x
            motionZ += inputVec.z
        }
    }

    /**
     * Applies gravity and drag after movement, matching vanilla's
     * {@code LivingEntity.travelInAir()} ordering.
     */
    private fun applyFreeFallForces() {
        motionY -= effectiveGravity()
        motionX *= LivingEntity.BASE_HORIZONTAL_AIR_DRAG.toDouble()
        motionY *= LivingEntity.BASE_VERTICAL_AIR_DRAG.toDouble()
        motionZ *= LivingEntity.BASE_HORIZONTAL_AIR_DRAG.toDouble()
    }

    /**
     * Simulates one tick of elytra flight physics,
     * matching Minecraft 26.2 {@code LivingEntity.updateFallFlyingMovement()}.
     */
    private fun calculateElytraTick(rotationVec: Vec3) {
        val gravity = effectiveGravity()

        val pitchRad: Double = this.player.xRot.toDouble() * Mth.DEG_TO_RAD

        val k = sqrt(rotationVec.x * rotationVec.x + rotationVec.z * rotationVec.z)
        val l = sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ)

        val m = rotationVec.length()
        var n = Mth.cos(pitchRad)

        n = (n.toDouble() * n.toDouble() * 1.0.coerceAtMost(m / 0.4)).toFloat()

        var vec3d5 = Vec3(this.motionX, this.motionY + gravity * (-1.0 + n.toDouble() * 0.75), this.motionZ)

        var q: Double
        if (vec3d5.y < 0.0 && k > 0.0) {
            q = vec3d5.y * -0.1 * n.toDouble()
            vec3d5 = vec3d5.add(rotationVec.x * q / k, q, rotationVec.z * q / k)
        }

        if (pitchRad < 0.0 && k > 0.0) {
            q = l * (-Mth.sin(pitchRad)).toDouble() * 0.04
            vec3d5 = vec3d5.add(-rotationVec.x * q / k, q * 3.2, -rotationVec.z * q / k)
        }

        if (k > 0.0) {
            vec3d5 = vec3d5.add((rotationVec.x / k * l - vec3d5.x) * 0.1, 0.0, (rotationVec.z / k * l - vec3d5.z) * 0.1)
        }

        this.motionX = vec3d5.x * LivingEntity.ELYTRA_HORIZONTAL_AIR_DRAG.toDouble()
        this.motionY = vec3d5.y * LivingEntity.ELYTRA_VERTICAL_AIR_DRAG.toDouble()
        this.motionZ = vec3d5.z * LivingEntity.ELYTRA_HORIZONTAL_AIR_DRAG.toDouble()
    }

    /**
     * Mirrors Minecraft 26.2 {@code LivingEntity.getEffectiveGravity()}.
     */
    private fun effectiveGravity(): Double {
        val rawGravity = player.gravity
        return if (motionY <= 0.0 && hasStatusEffect(MobEffects.SLOW_FALLING)) {
            minOf(rawGravity, 0.01)
        } else {
            rawGravity
        }
    }

    private fun hasStatusEffect(effect: Holder<MobEffect>): Boolean {
        val instance = player.getEffect(effect) ?: return false

        return instance.duration >= this.simulatedTicks
    }

    /**
     * @see LivingEntity.INPUT_FRICTION
     */
    private fun playerMovementInput() =
        Vec3(
            player.input.movementSideways.toDouble() * 0.98,
            0.0,
            player.input.movementForward.toDouble() * 0.98,
        )

    fun findCollision(ticks: Int): CollisionResult? {
        val rotationVec = player.lookAngle

        for (i in 0 until ticks) {
            val positionBeforeMovement = Vec3(x, y, z)
            if (advanceSimulation(rotationVec, forceDescending = false)) {
                return CollisionResult(
                    findSupportingBlock(boundingBox, lastResolvedMovement),
                    i,
                    positionBeforeMovement,
                )
            }
        }
        return null
    }

    /**
     * Checks whether a future movement tick starts with the player's feet in [targetPos] before landing.
     * This matches the block-position lookup in Minecraft 26.2 {@code LivingEntity.onClimbable()}.
     */
    fun willStartTickInBlockBeforeCollision(
        targetPos: BlockPos,
        ticks: Int,
        forceDescending: Boolean = false,
    ): Boolean {
        val rotationVec = player.lookAngle

        repeat(ticks) {
            if (BlockPos.containing(x, y, z) == targetPos) {
                return true
            }

            if (advanceSimulation(rotationVec, forceDescending)) {
                return false
            }
        }

        return false
    }

    private fun advanceSimulation(rotationVec: Vec3, forceDescending: Boolean): Boolean {
        val intendedMovement = calculateMovementForTick(rotationVec)
        val resolvedMovement = if (forceDescending) {
            val collisionContext = EntityCollisionContext(
                true,
                false,
                y,
                player.mainHandItem,
                false,
                player,
            )
            Entity.collideBoundingBox(collisionContext, intendedMovement, boundingBox, world, emptyList())
        } else {
            collidePlayer(intendedMovement)
        }
        val collidedDownwards = intendedMovement.y < 0.0 && resolvedMovement.y != intendedMovement.y

        lastResolvedMovement = resolvedMovement
        x += resolvedMovement.x
        y += resolvedMovement.y
        z += resolvedMovement.z
        boundingBox = boundingBox.move(resolvedMovement)

        if (collidedDownwards) {
            return true
        }

        applyCollisionResponse(intendedMovement, resolvedMovement)
        if (!player.isFallFlying) {
            applyFreeFallForces()
        }

        simulatedTicks++
        return false
    }

    /**
     * Mirrors Minecraft 26.2's private {@code Entity.collide(Vec3)} step-up path.
     */
    private fun collidePlayer(movement: Vec3): Vec3 {
        val entityColliders = world.getEntityCollisions(player, boundingBox.expandTowards(movement))
        val directMovement = if (movement.lengthSqr() == 0.0) {
            movement
        } else {
            Entity.collideBoundingBox(player, movement, boundingBox, world, entityColliders)
        }
        val collidedHorizontally = movement.x != directMovement.x || movement.z != directMovement.z
        val landed = movement.y < 0.0 && movement.y != directMovement.y

        if (player.maxUpStep() <= 0f || (!landed && !player.onGround()) || !collidedHorizontally) {
            return directMovement
        }

        val groundedBox = if (landed) boundingBox.move(0.0, directMovement.y, 0.0) else boundingBox
        var stepSearchBox = groundedBox.expandTowards(movement.x, player.maxUpStep().toDouble(), movement.z)
        if (!landed) {
            stepSearchBox = stepSearchBox.expandTowards(0.0, -1.0E-5, 0.0)
        }

        val colliders = buildList {
            addAll(entityColliders)
            world.worldBorder.takeIf { it.isInsideCloseToBorder(player, stepSearchBox) }
                ?.let { add(it.collisionShape) }
            addAll(world.getBlockCollisions(player, stepSearchBox))
        }

        return resolveStepUpMovement(
            movement,
            directMovement,
            boundingBox,
            groundedBox,
            player.maxUpStep(),
            colliders,
        )
    }

    /**
     * Mirrors Minecraft 26.2 {@code Entity.checkSupportingBlock()} including its moved-back fallback.
     */
    private fun findSupportingBlock(boundingBox: AABB, movement: Vec3): BlockPos? {
        val testArea = AABB(
            boundingBox.minX,
            boundingBox.minY - SUPPORT_EPSILON,
            boundingBox.minZ,
            boundingBox.maxX,
            boundingBox.minY,
            boundingBox.maxZ,
        )

        return findSupportingBlock(testArea)
            ?: findSupportingBlock(testArea.move(-movement.x, 0.0, -movement.z))
    }

    private fun findSupportingBlock(testArea: AABB): BlockPos? {
        val candidates = BlockCollisions(world, player, testArea, false) { pos, _ -> pos }
        return selectSupportingBlock(candidates, Vec3(x, y, z))
    }

    /**
     * Matches the zero-restitution player path in Minecraft 26.2
     * {@code Entity.restituteMovementAfterCollisions()}.
     */
    private fun applyCollisionResponse(intendedMovement: Vec3, resolvedMovement: Vec3) {
        motionX = if (resolvedMovement.x != intendedMovement.x) 0.0 else motionX
        motionY = if (resolvedMovement.y != intendedMovement.y) 0.0 else motionY
        motionZ = if (resolvedMovement.z != intendedMovement.z) 0.0 else motionZ
    }

    /**
     * [positionBeforeMovement] matches the position from which Minecraft 26.2 performs item use
     * before the movement step represented by [tick].
     */
    class CollisionResult(
        val pos: BlockPos?,
        val tick: Int,
        val positionBeforeMovement: Vec3,
    )
}

/**
 * Follows Minecraft 26.2 {@code Entity.collectCandidateStepUpHeights()} and
 * {@code Entity.collideWithShapes()} when selecting a step-up movement.
 */
internal fun resolveStepUpMovement(
    movement: Vec3,
    directMovement: Vec3,
    boundingBox: AABB,
    groundedBox: AABB,
    maxUpStep: Float,
    colliders: List<VoxelShape>,
): Vec3 {
    val candidates = FloatArraySet(4)
    for (collider in colliders) {
        val coords = collider.getCoords(Direction.Axis.Y)
        for (i in coords.indices) {
            val coordinate = coords.getDouble(i)
            val relativeCoordinate = (coordinate - groundedBox.minY).toFloat()
            if (relativeCoordinate >= 0f && relativeCoordinate != directMovement.y.toFloat()) {
                if (relativeCoordinate > maxUpStep) {
                    break
                }
                candidates.add(relativeCoordinate)
            }
        }
    }

    val sortedCandidates = candidates.toFloatArray()
    FloatArrays.unstableSort(sortedCandidates)
    for (candidate in sortedCandidates) {
        val steppedMovement = collideWithShapes(
            Vec3(movement.x, candidate.toDouble(), movement.z),
            groundedBox,
            colliders,
        )
        if (steppedMovement.horizontalDistanceSqr() > directMovement.horizontalDistanceSqr()) {
            return steppedMovement.subtract(0.0, boundingBox.minY - groundedBox.minY, 0.0)
        }
    }

    return directMovement
}

/**
 * Mirrors Minecraft 26.2 {@code Entity.collideWithShapes()}.
 */
private fun collideWithShapes(movement: Vec3, boundingBox: AABB, shapes: List<VoxelShape>): Vec3 {
    if (shapes.isEmpty()) {
        return movement
    }

    var resolvedMovement = Vec3.ZERO
    for (axis in Direction.axisStepOrder(movement)) {
        val axisMovement = movement.get(axis)
        if (axisMovement != 0.0) {
            val collision = Shapes.collide(axis, boundingBox.move(resolvedMovement), shapes, axisMovement)
            resolvedMovement = resolvedMovement.with(axis, collision)
        }
    }
    return resolvedMovement
}

/**
 * Mirrors Minecraft 26.2 {@code CollisionGetter.findSupportingBlock()}: nearest first,
 * then the greater {@code BlockPos.compareTo()} position on an exact distance tie.
 */
internal fun selectSupportingBlock(candidates: Iterator<BlockPos>, position: Vec3): BlockPos? {
    val support = BlockPos.MutableBlockPos()
    var supportDistance = Double.POSITIVE_INFINITY

    for (candidate in candidates) {
        val distance = candidate.distToCenterSqr(position)
        if (distance < supportDistance ||
            distance == supportDistance && support.compareTo(candidate) < 0
        ) {
            support.set(candidate)
            supportDistance = distance
        }
    }

    return support.takeIf { supportDistance.isFinite() }
}
