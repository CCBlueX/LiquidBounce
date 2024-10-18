/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.utils.aiming.Rotation
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
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.TntEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.mob.CreeperEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.vehicle.TntMinecartEntity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import net.minecraft.stat.Stats
import net.minecraft.util.UseAction
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.*
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.Difficulty
import net.minecraft.world.GameRules
import net.minecraft.world.RaycastContext
import net.minecraft.world.explosion.Explosion
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

val ClientPlayerEntity.moving
    get() = input.movementForward != 0.0f || input.movementSideways != 0.0f

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
    pos: Vec3d = this.pos
): Boolean {
    val alpha = (getMovementDirectionOfInput(this.yaw, directionalInput) + 90.0F).toRadians()

    val simulatedInput = SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(directionalInput)

    simulatedInput.jumping = false
    simulatedInput.sneaking = false

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

    val from = pos + Vec3d(0.0, -0.1, 0.0)
    val to = from + direction.multiply(distance)

    if (findEdgeCollision(from, to) != null) {
        return true
    }

    val playerPosInTwoTicks = simulatedPlayer.pos.add(nextVelocity.multiply(1.0, 0.0, 1.0))

    return wouldBeCloseToFallOff(pos) || wouldBeCloseToFallOff(playerPosInTwoTicks)
}

val ClientPlayerEntity.pressingMovementButton
    get() = input.pressingForward || input.pressingBack || input.pressingLeft || input.pressingRight

val Entity.exactPosition
    get() = Triple(x, y, z)

val PlayerEntity.ping: Int
    get() = mc.networkHandler?.getPlayerListEntry(uuid)?.latency ?: 0

val ClientPlayerEntity.directionYaw: Float
    get() = getMovementDirectionOfInput(this.yaw, DirectionalInput(this.input))

val ClientPlayerEntity.isBlockAction: Boolean
    get() = player.isUsingItem && player.activeItem.useAction == UseAction.BLOCK

fun getMovementDirectionOfInput(facingYaw: Float, input: DirectionalInput): Float {
    var actualYaw = facingYaw
    var forward = 1f

    // Check if client-user tries to walk backwards (+180 to turn around)
    if (input.backwards) {
        actualYaw += 180f
        forward = -0.5f
    } else if (input.forwards) {
        forward = 0.5f
    }

    // Check which direction the client-user tries to walk sideways
    if (input.left) {
        actualYaw -= 90f * forward
    }
    if (input.right) {
        actualYaw += 90f * forward
    }

    return actualYaw
}

val PlayerEntity.sqrtSpeed: Double
    get() = velocity.sqrtSpeed

val LivingEntity.nextTickPos: Vec3d
    get() = pos.add(velocity)

fun ClientPlayerEntity.upwards(height: Float, increment: Boolean = true) {
    // Might be a jump
    if (isOnGround && increment) {
        // Allows to bypass modern anti-cheat techniques
        incrementStat(Stats.JUMP)
    }

    velocity.y = height.toDouble()
    velocityDirty = true
}

fun ClientPlayerEntity.downwards(motion: Float) {
    velocity.y = -motion.toDouble()
    velocityDirty = true
}

fun ClientPlayerEntity.strafe(yaw: Float = directionYaw, speed: Double = sqrtSpeed, strength: Double = 1.0,
                              keyboardCheck: Boolean = true) {
    if (keyboardCheck && !moving) {
        velocity.x = 0.0
        velocity.z = 0.0
        return
    }

    velocity.strafe(yaw, speed, strength)
}

val Vec3d.sqrtSpeed: Double
    get() = sqrt(x * x + z * z)

fun Vec3d.strafe(yaw: Float = player.directionYaw, speed: Double = sqrtSpeed, strength: Double = 1.0,
                 keyboardCheck: Boolean = false): Vec3d {
    if (keyboardCheck && !player.pressingMovementButton) {
        x = 0.0
        z = 0.0
        return this
    }

    val prevX = x * (1.0 - strength)
    val prevZ = z * (1.0 - strength)
    val useSpeed = speed * strength

    val angle = Math.toRadians(yaw.toDouble())
    x = (-sin(angle) * useSpeed) + prevX
    z = (cos(angle) * useSpeed) + prevZ
    return this
}

val Entity.eyes: Vec3d
    get() = eyePos

val Entity.prevPos: Vec3d
    get() = Vec3d(this.prevX, this.prevY, this.prevZ)

val Entity.rotation: Rotation
    get() = Rotation(this.yaw, this.pitch)

val ClientPlayerEntity.lastRotation: Rotation
    get() = Rotation(this.lastYaw, this.lastPitch)

val Entity.box: Box
    get() = boundingBox.expand(targetingMargin.toDouble())

/**
 * Allows to calculate the distance between the current entity and [entity] from the nearest corner of the bounding box
 */
fun Entity.boxedDistanceTo(entity: Entity): Double {
    return sqrt(squaredBoxedDistanceTo(entity))
}

fun Entity.squaredBoxedDistanceTo(entity: Entity): Double {
    return this.squaredBoxedDistanceTo(entity.eyes)
}

fun Entity.squaredBoxedDistanceTo(otherPos: Vec3d): Double {
    return this.box.squaredBoxedDistanceTo(otherPos)
}

fun Entity.squareBoxedDistanceTo(entity: Entity, offsetPos: Vec3d): Double {
    return this.box.offset(offsetPos - this.pos).squaredBoxedDistanceTo(entity.eyes)
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

fun PlayerEntity.wouldBlockHit(source: PlayerEntity): Boolean {
    if (!this.isBlocking) {
        return false
    }

    val vec3d = source.pos

    val facingVec = getRotationVec(1.0f)
    var deltaPos = vec3d.relativize(pos).normalize()

    deltaPos = Vec3d(deltaPos.x, 0.0, deltaPos.z)

    return deltaPos.dotProduct(facingVec) < 0.0
}

/**
 * Applies armor, enchantments, effects, etc. to the damage and returns the damage
 * that is actually applied. This function is so damn ugly that I turned off code smell analysis for it.
 */
@Suppress("detekt:all")
fun LivingEntity.getEffectiveDamage(source: DamageSource, damage: Float, ignoreShield: Boolean = false): Float {
    val world = this.world

    if (this.isInvulnerableTo(source))
        return 0.0F

    // EDGE CASE!!! Might cause weird bugs
    if (this.isDead)
        return 0.0F

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
        is EndCrystalEntity -> getDamageFromExplosion(entity.pos, entity, 6f, 12f, 144f)
        is TntEntity -> getDamageFromExplosion(entity.pos.add(0.0, 0.0625, 0.0), entity, 4f, 8f, 64f)
        is TntMinecartEntity -> {
            val d = 5f
            getDamageFromExplosion(entity.pos, entity, 4f + d * 1.5f)
        }

        is CreeperEntity -> {
            val f = if (entity.shouldRenderOverlay()) 2f else 1f
            getDamageFromExplosion(entity.pos, entity, entity.explosionRadius * f)
        }

        else -> 0f
    }
}

fun LivingEntity.getDamageFromExplosion(
    pos: Vec3d,
    exploding: Entity? = null,
    power: Float = 6f,
    explosionRange: Float = power * 2f, // allows setting precomputed values
    damageDistance: Float = explosionRange * explosionRange,
    exclude: Array<BlockPos>? = null
): Float {
    // no damage will be dealt if the entity is outside the explosion range
    if (this.squaredDistanceTo(pos) > damageDistance) {
        return 0f
    }

    val distanceDecay = 1f - sqrt(this.squaredDistanceTo(pos).toFloat()) / explosionRange
    val exposure = exclude?.let { getExposureToExplosion(pos, it) } ?: Explosion.getExposure(pos, this)
    val pre1 = exposure * distanceDecay

    val preprocessedDamage = floor((pre1 * pre1 + pre1) / 2f * 7f * explosionRange + 1f)

    val explosion = Explosion(
        world,
        exploding,
        pos.x,
        pos.y,
        pos.z,
        power,
        false,
        world.getDestructionType(GameRules.BLOCK_EXPLOSION_DROP_DECAY)
    )

    return getEffectiveDamage(world.damageSources.explosion(explosion), preprocessedDamage)
}

/**
 * Basically [Explosion.getExposure] but this method allows us to exclude blocks using [exclude].
 */
fun LivingEntity.getExposureToExplosion(source: Vec3d, exclude: Array<BlockPos>): Float {
    val entityBoundingBox = boundingBox

    val stepX = 1.0 / ((entityBoundingBox.maxX - entityBoundingBox.minX) * 2.0 + 1.0)
    val stepY = 1.0 / ((entityBoundingBox.maxY - entityBoundingBox.minY) * 2.0 + 1.0)
    val stepZ = 1.0 / ((entityBoundingBox.maxZ - entityBoundingBox.minZ) * 2.0 + 1.0)

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
                val sampleX = MathHelper.lerp(currentXStep, entityBoundingBox.minX, entityBoundingBox.maxX)
                val sampleY = MathHelper.lerp(currentYStep, entityBoundingBox.minY, entityBoundingBox.maxY)
                val sampleZ = MathHelper.lerp(currentZStep, entityBoundingBox.minZ, entityBoundingBox.maxZ)

                val samplePoint = Vec3d(sampleX + offsetX, sampleY, sampleZ + offsetZ)
                val hitResult = world.raycast(
                    RaycastContext(
                        samplePoint,
                        source,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        this
                    ), exclude
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

fun LivingEntity.getActualHealth(fromScoreboard: Boolean = true): Float {
    if (fromScoreboard) {
        world.scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME)?.let { objective ->
            objective.scoreboard.getScore(this, objective)?.let { scoreboard ->
                val displayName = objective.displayName

                if (displayName != null && scoreboard.score > 0 && displayName.string.contains("❤")) {
                    return scoreboard.score.toFloat()
                }
            }
        }
    }

    return health
}

/**
 * Check if the entity is likely falling to the void based on the current position and bounding box.
 */
fun Entity.isFallingToVoid(voidLevel: Double = -64.0, safetyExpand: Double = 0.0): Boolean {
    if (this.y < voidLevel || boundingBox.minY < voidLevel) {
        return true
    }

    // If there is no collision to void threshold, we do not want to teleport down.
    val boundingBox = boundingBox
        // Set the minimum Y to the void threshold to check for collisions below the player
        .withMinY(voidLevel)
        // Expand the bounding box to check if there might blocks to safely land on
        .expand(safetyExpand, 0.0, safetyExpand)
    return world.getBlockCollisions(this, boundingBox)
        .all { shape -> shape == VoxelShapes.empty() }
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
        .all { shape -> shape == VoxelShapes.empty() }
}


fun Float.toValidYaw(): Float {
    return ((this + 180) % 360) - 180
}

fun ClientPlayerEntity.warp(pos: Vec3d? = null, onGround: Boolean = false) {
    val vehicle = this.vehicle

    if (vehicle != null) {
        pos?.let { pos -> vehicle.setPosition(pos) }
        network.sendPacket(VehicleMoveC2SPacket(vehicle))
        return
    }

    if (pos != null) {
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, onGround))
    } else {
        network.sendPacket(PlayerMoveC2SPacket.OnGroundOnly(onGround))
    }
}

fun ClientPlayerEntity.isInHole(): Boolean {
    val pos = BlockPos.ofFloored(pos)
    return Direction.entries.all {
        if (it == Direction.UP) {
            return@all true
        }

        pos.offset(it).isBlastResistant()
    }
}

fun ClientPlayerEntity.isBurrowed(): Boolean {
    return BlockPos.ofFloored(pos).isBlastResistant()
}
