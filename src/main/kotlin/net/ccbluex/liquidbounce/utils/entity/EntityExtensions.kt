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
 */

@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.common.ShapeFlag
import net.ccbluex.liquidbounce.interfaces.ClientPlayerEntityAddition
import net.ccbluex.liquidbounce.interfaces.InputAddition
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.DIRECTIONS_EXCLUDING_UP
import net.ccbluex.liquidbounce.utils.block.isBlastResistant
import net.ccbluex.liquidbounce.utils.block.raycast
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.findEdgeCollision
import net.minecraft.block.EntityShapeContext
import net.minecraft.block.ShapeContext
import net.minecraft.client.input.Input
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.TntEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.mob.CreeperEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.vehicle.TntMinecartEntity
import net.minecraft.item.consume.UseAction
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import net.minecraft.util.Hand
import net.minecraft.util.PlayerInput
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.*
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.Difficulty
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import net.minecraft.world.explosion.ExplosionBehavior
import net.minecraft.world.explosion.ExplosionImpl
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

val Entity.netherPosition: Vec3d
    get() = if (world.registryKey == World.NETHER) {
        Vec3d(x, y, z)
    } else {
        Vec3d(x / 8.0, y, z / 8.0)
    }

val ClientPlayerEntity.moving
    get() = input.movementForward != 0.0f || input.movementSideways != 0.0f

val Input.untransformed: PlayerInput
    get() = (this as InputAddition).`liquid_bounce$getUntransformed`()

val Input.initial: PlayerInput
    get() = (this as InputAddition).`liquid_bounce$getInitial`()

val Entity.exactPosition
    get() = Vec3d(x, y, z)

val Entity.blockVecPosition
    get() = Vec3i(blockX, blockY, blockZ)

val PlayerEntity.ping: Int
    get() = mc.networkHandler?.getPlayerListEntry(uuid)?.latency ?: 0

val ClientPlayerEntity.airTicks: Int
    get() = (this as ClientPlayerEntityAddition).`liquid_bounce$getAirTicks`()

val ClientPlayerEntity.onGroundTicks: Int
    get() = (this as ClientPlayerEntityAddition).`liquid_bounce$getOnGroundTicks`()

val ClientPlayerEntity.direction: Float
    get() = getMovementDirectionOfInput(DirectionalInput(input))

fun ClientPlayerEntity.getMovementDirectionOfInput(input: DirectionalInput): Float {
    return getMovementDirectionOfInput(this.yaw, input)
}

val ClientPlayerEntity.isBlockAction: Boolean
    get() = isUsingItem && activeItem.useAction == UseAction.BLOCK

fun Entity.lastRenderPos() = Vec3d(this.lastRenderX, this.lastRenderY, this.lastRenderZ)

val Hand.equipmentSlot: EquipmentSlot
    get() = when (this) {
        Hand.MAIN_HAND -> EquipmentSlot.MAINHAND
        Hand.OFF_HAND -> EquipmentSlot.OFFHAND
    }

fun ClientPlayerEntity.wouldBeCloseToFallOff(position: Vec3d): Boolean {
    val hitbox =
        this.dimensions
            .getBoxAt(position)
            .expand(-0.05, 0.0, -0.05)
            .offset(0.0, (this.fallDistance - this.stepHeight).toDouble(), 0.0)

    return world.isSpaceEmpty(this, hitbox)
}

fun ClientPlayerEntity.isCloseToEdge(
    directionalInput: DirectionalInput,
    distance: Double = 0.1,
    pos: Vec3d = this.pos,
): Boolean {
    val alpha = (getMovementDirectionOfInput(this.yaw, directionalInput) + 90.0F).toRadians()
    val simulatedInput = SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(directionalInput)
    simulatedInput.set(
        jump = false,
        sneak = false
    )

    val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
        simulatedInput
    )

    simulatedPlayer.pos = pos
    simulatedPlayer.tick()

    val nextVelocity = simulatedPlayer.velocity
    val direction = if (nextVelocity.horizontalLengthSquared() > 0.003 * 0.003) {
        nextVelocity.multiply(1.0, 0.0, 1.0).normalize()
    } else {
        Vec3d(cos(alpha).toDouble(), 0.0, sin(alpha).toDouble())
    }

    val from = pos.add(0.0, -0.1, 0.0)
    val to = from + direction.multiply(distance)

    if (findEdgeCollision(from, to) != null) {
        return true
    }

    val playerPosInTwoTicks = simulatedPlayer.pos.add(nextVelocity.multiply(1.0, 0.0, 1.0))

    return wouldBeCloseToFallOff(pos) || wouldBeCloseToFallOff(playerPosInTwoTicks)
}

/**
 * Check if the player can step up by [height] blocks.
 *
 * TODO: Use Minecraft Step logic instead of this basic collision check.
 */
fun ClientPlayerEntity.canStep(height: Double = 1.0): Boolean {
    if (!horizontalCollision || isDescending || !isOnGround) {
        // If we are not colliding with anything, we are not meant to step
        return false
    }

    val box = this.boundingBox
    val direction = this.direction

    val angle = Math.toRadians(direction.toDouble())
    val xOffset = -sin(angle) * 0.1
    val zOffset = cos(angle) * 0.1

    val offsetBox = box.offset(xOffset, 0.0, zOffset)
    val stepBox = offsetBox.offset(0.0, height, 0.0)

    return world.getBlockCollisions(this, stepBox).all { shape ->
        shape == VoxelShapes.empty()
    } && world.getBlockCollisions(this, offsetBox).all { shape ->
        shape != VoxelShapes.empty()
    }
}


fun getMovementDirectionOfInput(facingYaw: Float, input: DirectionalInput): Float {
    val forwards = input.forwards && !input.backwards
    val backwards = input.backwards && !input.forwards
    val left = input.left && !input.right
    val right = input.right && !input.left

    var actualYaw = facingYaw
    var forward = 1f

    if (backwards) {
        actualYaw += 180f
        forward = -0.5f
    } else if (forwards) {
        forward = 0.5f
    }

    if (left) {
        actualYaw -= 90f * forward
    }
    if (right) {
        actualYaw += 90f * forward
    }

    return actualYaw
}

val PlayerEntity.sqrtSpeed: Double
    get() = velocity.sqrtSpeed

val Vec3d.sqrtSpeed: Double
    get() = sqrt(x * x + z * z)

fun Vec3d.withStrafe(
    speed: Double = sqrtSpeed,
    strength: Double = 1.0,
    input: DirectionalInput? = DirectionalInput(player.input),
    yaw: Float = player.getMovementDirectionOfInput(input ?: DirectionalInput(player.input)),
): Vec3d {
    if (input?.isMoving == false) {
        return Vec3d(0.0, y, 0.0)
    }

    val prevX = x * (1.0 - strength)
    val prevZ = z * (1.0 - strength)
    val useSpeed = speed * strength

    val angle = Math.toRadians(yaw.toDouble())
    val x = (-sin(angle) * useSpeed) + prevX
    val z = (cos(angle) * useSpeed) + prevZ
    return Vec3d(x, y, z)
}

val Entity.prevPos: Vec3d
    get() = Vec3d(this.prevX, this.prevY, this.prevZ)

val Entity.rotation: Rotation
    get() = Rotation(this.yaw, this.pitch, true)

val ClientPlayerEntity.lastRotation: Rotation
    get() = Rotation(this.lastYaw, this.lastPitch, true)

val Entity.box: Box
    get() = boundingBox.expand(targetingMargin.toDouble())

/**
 * Allows to calculate the distance between the current entity and [entity] from the nearest corner of the bounding box
 */
fun Entity.boxedDistanceTo(entity: Entity): Double {
    return sqrt(squaredBoxedDistanceTo(entity))
}

fun Entity.squaredBoxedDistanceTo(entity: Entity): Double {
    return this.squaredBoxedDistanceTo(entity.eyePos)
}

fun Entity.squaredBoxedDistanceTo(otherPos: Vec3d): Double {
    return this.box.squaredBoxedDistanceTo(otherPos)
}

fun Entity.squareBoxedDistanceTo(entity: Entity, offsetPos: Vec3d): Double {
    return this.box.offset(offsetPos - this.pos).squaredBoxedDistanceTo(entity.eyePos)
}

fun Box.squaredBoxedDistanceTo(otherPos: Vec3d): Double {
    val pos = getNearestPoint(otherPos, this)

    return pos.squaredDistanceTo(otherPos)
}

fun Entity.interpolateCurrentPosition(tickDelta: Float): Vec3d {
    if (this.age == 0) {
        return this.pos
    }

    return Vec3d(
        this.lastRenderX + (this.x - this.lastRenderX) * tickDelta,
        this.lastRenderY + (this.y - this.lastRenderY) * tickDelta,
        this.lastRenderZ + (this.z - this.lastRenderZ) * tickDelta
    )
}

fun Entity.interpolateCurrentRotation(tickDelta: Float): Rotation {
    if (this.age == 0) {
        return this.rotation
    }

    return Rotation(
        this.prevYaw + (this.yaw - this.prevYaw) * tickDelta,
        this.prevPitch + (this.pitch - this.prevPitch) * tickDelta,
    )
}

/**
 * Get the nearest point of a box. Very useful to calculate the distance of an enemy.
 */
fun getNearestPoint(eyes: Vec3d, box: Box): Vec3d {
    val origin = doubleArrayOf(eyes.x, eyes.y, eyes.z)
    val destMins = doubleArrayOf(box.minX, box.minY, box.minZ)
    val destMaxs = doubleArrayOf(box.maxX, box.maxY, box.maxZ)

    // It loops through every coordinate of the double arrays and picks the nearest point
    for (i in 0..2) {
        origin[i] = origin[i].coerceIn(destMins[i], destMaxs[i])
    }

    return Vec3d(origin[0], origin[1], origin[2])
}

fun getNearestPointOnSide(eyes: Vec3d, box: Box, side: Direction): Vec3d {
    val nearestPointInBlock = getNearestPoint(eyes, box)

    val x = nearestPointInBlock.x
    val y = nearestPointInBlock.y
    val z = nearestPointInBlock.z

    val nearestPointOnSide =
        when (side) {
            Direction.DOWN -> Vec3d(x, box.minY, z)
            Direction.UP -> Vec3d(x, box.maxY, z)
            Direction.NORTH -> Vec3d(x, y, box.minZ)
            Direction.SOUTH -> Vec3d(x, y, box.maxZ)
            Direction.WEST -> Vec3d(box.maxX, y, z)
            Direction.EAST -> Vec3d(box.minX, y, z)
        }

    return nearestPointOnSide

}

fun LivingEntity.wouldBlockHit(source: PlayerEntity): Boolean {
    if (!this.isBlocking) {
        return false
    }

    val facingVec = getRotationVec(1.0f)
    val deltaPos = (pos - source.pos).multiply(1.0, 0.0, 1.0)

    return deltaPos.dotProduct(facingVec) < 0.0
}

/**
 * Applies armor, enchantments, effects, etc. to the damage and returns the damage
 * that is actually applied. This function is so damn ugly that I turned off code smell analysis for it.
 */
@Suppress("detekt:all")
fun LivingEntity.getEffectiveDamage(source: DamageSource, damage: Float, ignoreShield: Boolean = false): Float {
    val world = this.world

    if (this.isAlwaysInvulnerableTo(source)) {
        return 0.0F
    }

    // EDGE CASE!!! Might cause weird bugs
    if (this.isDead) {
        return 0.0F
    }

    var amount = damage

    if (this is PlayerEntity) {
        if (this.abilities.invulnerable && source.type.msgId != mc.world!!.damageSources.outOfWorld().type.msgId)
            return 0.0F

        if (source.isScaledWithDifficulty) {
            if (world.difficulty == Difficulty.PEACEFUL) {
                amount = 0.0f
            }

            if (world.difficulty == Difficulty.EASY) {
                amount = (amount / 2.0f + 1.0f).coerceAtMost(amount)
            }

            if (world.difficulty == Difficulty.HARD) {
                amount = amount * 3.0f / 2.0f
            }
        }
    }

    if (amount == 0.0F)
        return 0.0F

    if (source == mc.world!!.damageSources.onFire() && this.hasStatusEffect(StatusEffects.FIRE_RESISTANCE))
        return 0.0F


    if (!ignoreShield && blockedByShield(source))
        return 0.0F

    // Do we need to take the timeUntilRegen mechanic into account?

    amount = this.applyArmorToDamage(source, amount)
    amount = this.modifyAppliedDamage(source, amount)

    return amount
}

fun LivingEntity.getExplosionDamageFromEntity(entity: Entity): Float {
    return when (entity) {
        is EndCrystalEntity -> getDamageFromExplosion(entity.pos, 6f, 12f, 144f)
        is TntEntity -> getDamageFromExplosion(entity.pos.add(0.0, 0.0625, 0.0), 4f, 8f, 64f)
        is TntMinecartEntity -> {
            val d = 5f
            getDamageFromExplosion(entity.pos, 4f + d * 1.5f)
        }

        is CreeperEntity -> {
            val f = if (entity.isCharged) 2f else 1f
            getDamageFromExplosion(entity.pos, entity.explosionRadius * f)
        }

        else -> 0f
    }
}

/**
 * See [ExplosionBehavior.calculateDamage].
 */
@Suppress("LongParameterList")
fun LivingEntity.getDamageFromExplosion(
    pos: Vec3d,
    power: Float = 6f,
    explosionRange: Float = power * 2f, // allows setting precomputed values
    damageDistance: Float = explosionRange * explosionRange,
    exclude: Array<BlockPos>? = null,
    include: BlockPos? = null,
    maxBlastResistance: Float? = null,
    entityBoundingBox: Box? = null
): Float {
    // no damage will be dealt if the entity is outside the explosion range or when the difficulty is peaceful
    if (this.squaredDistanceTo(pos) > damageDistance || world.difficulty == Difficulty.PEACEFUL) {
        return 0f
    }

    try {
        ShapeFlag.noShapeChange = true

        val useTweakedMethod = exclude != null ||
            maxBlastResistance != null ||
            include != null ||
            entityBoundingBox != null

        val exposure = if (useTweakedMethod) {
            getExposureToExplosion(pos, exclude, include, maxBlastResistance, entityBoundingBox)
        } else {
            ExplosionImpl.calculateReceivedDamage(pos, this)
        }

        val distanceDecay = 1.0 - (sqrt(this.squaredDistanceTo(pos)) / explosionRange.toDouble())
        val pre1 = exposure.toDouble() * distanceDecay

        val preprocessedDamage = (pre1 * pre1 + pre1) / 2.0 * 7.0 * explosionRange.toDouble() + 1.0
        if (preprocessedDamage == 0.0) {
            return 0f
        }

        return getEffectiveDamage(world.damageSources.explosion(null), preprocessedDamage.toFloat())
    } finally {
        ShapeFlag.noShapeChange = false
    }
}

/**
 * Basically [ExplosionImpl.calculateReceivedDamage] but this method allows us to exclude blocks using [exclude].
 */
@Suppress("NestedBlockDepth")
fun LivingEntity.getExposureToExplosion(
    source: Vec3d,
    exclude: Array<BlockPos>?,
    include: BlockPos?,
    maxBlastResistance: Float?,
    entityBoundingBox: Box?
): Float {
    val entityBoundingBox1 = entityBoundingBox ?: boundingBox
    val shapeContext = entityBoundingBox1?.let {
        EntityShapeContext(
            isDescending,
            entityBoundingBox1.minY,
            mainHandStack,
            { state -> canWalkOnFluid(state) },
            this
        )
    } ?: ShapeContext.of(this)

    val stepX = 1.0 / ((entityBoundingBox1.maxX - entityBoundingBox1.minX) * 2.0 + 1.0)
    val stepY = 1.0 / ((entityBoundingBox1.maxY - entityBoundingBox1.minY) * 2.0 + 1.0)
    val stepZ = 1.0 / ((entityBoundingBox1.maxZ - entityBoundingBox1.minZ) * 2.0 + 1.0)

    val offsetX = (1.0 - floor(1.0 / stepX) * stepX) / 2.0
    val offsetZ = (1.0 - floor(1.0 / stepZ) * stepZ) / 2.0

    if (stepX < 0.0 || stepY < 0.0 || stepZ < 0.0) {
        return 0f
    }

    var hits = 0
    var totalRays = 0

    var currentXStep = 0.0
    while (currentXStep <= 1.0) {
        var currentYStep = 0.0
        while (currentYStep <= 1.0) {
            var currentZStep = 0.0
            while (currentZStep <= 1.0) {
                val sampleX = MathHelper.lerp(currentXStep, entityBoundingBox1.minX, entityBoundingBox1.maxX)
                val sampleY = MathHelper.lerp(currentYStep, entityBoundingBox1.minY, entityBoundingBox1.maxY)
                val sampleZ = MathHelper.lerp(currentZStep, entityBoundingBox1.minZ, entityBoundingBox1.maxZ)

                val samplePoint = Vec3d(sampleX + offsetX, sampleY, sampleZ + offsetZ)
                val hitResult = world.raycast(
                    RaycastContext(
                        samplePoint,
                        source,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        shapeContext
                    ),
                    exclude,
                    include,
                    maxBlastResistance
                )

                if (hitResult.type == HitResult.Type.MISS) {
                    hits++
                }

                totalRays++
                currentZStep += stepZ
            }
            currentYStep += stepY
        }
        currentXStep += stepX
    }

    return hits.toFloat() / totalRays.toFloat()
}

/**
 * Sometimes the server does not publish the actual entity health with its metadata.
 * This function incorporates other sources to get the actual value.
 *
 * Currently, uses the following sources:
 * 1. Scoreboard
 */
fun LivingEntity.getActualHealth(fromScoreboard: Boolean = true): Float {
    if (fromScoreboard) {
        val health = getHealthFromScoreboard()

        if (health != null) {
            return health
        }
    }


    return health
}

private fun LivingEntity.getHealthFromScoreboard(): Float? {
    val objective = world.scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME) ?: return null
    val score = objective.scoreboard.getScore(this, objective) ?: return null

    val displayName = objective.displayName

    if (score.score <= 0 || displayName?.string?.contains("❤") != true) {
        return null
    }

    return score.score.toFloat()
}

fun Entity.getBoundingBoxAt(pos: Vec3d): Box {
    return boundingBox.offset(pos - this.pos)
}

/**
 * Check if the entity collides with anything below his bounding box.
 */
fun Entity.doesNotCollideBelow(until: Double = -64.0): Boolean {
    if (this.y < until || boundingBox.minY < until) {
        return true
    }

    val offsetBb = boundingBox.withMinY(until)
    return world.getBlockCollisions(this, offsetBb)
        .all(VoxelShapes.empty()::equals)
}

/**
 * Check if the entity box collides with any block in the world at the given [pos].
 */
fun Entity.doesCollideAt(pos: Vec3d = player.pos): Boolean {
    return !world.getBlockCollisions(this, getBoundingBoxAt(pos)).all(VoxelShapes.empty()::equals)
}

/**
 * Check if the entity is likely falling to the void based on the given position and bounding box.
 */
fun Entity.wouldFallIntoVoid(pos: Vec3d, voidLevel: Double = -64.0, safetyExpand: Double = 0.0): Boolean {
    val offsetBb = boundingBox.offset(pos - this.pos)

    if (pos.y < voidLevel || offsetBb.minY < voidLevel) {
        return true
    }

    // If there is no collision to void threshold, we do not want to teleport down.
    val boundingBox = offsetBb
        // Set the minimum Y to the void threshold to check for collisions below the player
        .withMinY(voidLevel)
        // Expand the bounding box to check if there might blocks to safely land on
        .expand(safetyExpand, 0.0, safetyExpand)
    return world.getBlockCollisions(this, boundingBox)
        .all(VoxelShapes.empty()::equals)
}


fun ClientPlayerEntity.warp(pos: Vec3d? = null, onGround: Boolean = false) {
    val vehicle = this.vehicle

    if (vehicle != null) {
        pos?.let(vehicle::setPosition)
        network.sendPacket(VehicleMoveC2SPacket.fromVehicle(vehicle))
        return
    }

    if (pos != null) {
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, onGround, horizontalCollision))
    } else {
        network.sendPacket(PlayerMoveC2SPacket.OnGroundOnly(onGround, horizontalCollision))
    }
}

fun ClientPlayerEntity.isInHole(feetBlockPos: BlockPos = getFeetBlockPos()): Boolean {
    return DIRECTIONS_EXCLUDING_UP.all {
        feetBlockPos.offset(it).isBlastResistant()
    }
}

fun ClientPlayerEntity.isBurrowed(): Boolean {
    return getFeetBlockPos().isBlastResistant()
}

fun ClientPlayerEntity.getFeetBlockPos(): BlockPos {
    val bb = boundingBox
    return BlockPos(
        MathHelper.floor(MathHelper.lerp(0.5, bb.minX, bb.maxX)),
        MathHelper.ceil(bb.minY),
        MathHelper.floor(MathHelper.lerp(0.5, bb.minZ, bb.maxZ))
    )
}
