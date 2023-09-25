/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.client.input.Input
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageSources
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.stat.Stats
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.Difficulty
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

val ClientPlayerEntity.moving
    get() = input.movementForward != 0.0f || input.movementSideways != 0.0f


fun Entity.isCloseToEdge(distance: Double = 0.1): Boolean {
    Direction.values().drop(2).forEach { side ->
        if (!this.pos.offset(side, distance).add(0.0, -1.0, 0.0).toBlockPos().canStandOn())
            return true
    }
    return false
}

val ClientPlayerEntity.pressingMovementButton
    get() = input.pressingForward || input.pressingBack || input.pressingLeft || input.pressingRight

val Entity.exactPosition
    get() = Triple(x, y, z)

val PlayerEntity.ping: Int
    get() = mc.networkHandler?.getPlayerListEntry(uuid)?.latency ?: 0

val ClientPlayerEntity.directionYaw: Float
    get() = getMovementDirectionOfInput(this.yaw, DirectionalInput(this.input))

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
    velocity.y = motion.toDouble()
    velocityDirty = true
}

fun ClientPlayerEntity.strafe(yaw: Float = directionYaw, speed: Double = sqrtSpeed) {
    if (!moving) {
        velocity.x = 0.0
        velocity.z = 0.0
        return
    }

    val angle = Math.toRadians(yaw.toDouble())
    velocity.x = -sin(angle) * speed
    velocity.z = cos(angle) * speed
}

val Vec3d.sqrtSpeed: Double
    get() = sqrt(x * x + z * z)

fun Vec3d.strafe(yaw: Float, speed: Double = sqrtSpeed, strength: Double = 1.0) {
    val prevX = x * (1.0 - strength)
    val prevZ = z * (1.0 - strength)
    val useSpeed = speed * strength

    val angle = Math.toRadians(yaw.toDouble())
    x = (-sin(angle) * useSpeed) + prevX
    z = (cos(angle) * useSpeed) + prevZ
}

fun Vec3d.strafe(yaw: Float, speed: Double = sqrtSpeed, strength: Double = 1.0, keyboardCheck: Boolean = false) {
    val player = mc.player ?: return

    if (keyboardCheck && !player.pressingMovementButton) {
        x = 0.0
        z = 0.0
        return
    }

    this.strafe(yaw, speed, strength)
}

val Entity.eyes: Vec3d
    get() = eyePos

val Input.yAxisMovement: Float
    get() = when {
        jumping -> 1.0f
        sneaking -> -1.0f
        else -> 0.0f
    }

val Entity.rotation: Rotation
    get() = Rotation(yaw, pitch)

val Entity.box: Box
    get() = boundingBox.expand(targetingMargin.toDouble())

/**
 * Allows to calculate the distance between the current entity and [entity] from the nearest corner of the bounding box
 */
fun Entity.boxedDistanceTo(entity: Entity): Double {
    return sqrt(squaredBoxedDistanceTo(entity))
}

fun Entity.squaredBoxedDistanceTo(entity: Entity): Double {
    return this.squaredBoxedDistanceTo(entity.getCameraPosVec(1.0F))
}

fun Entity.squaredBoxedDistanceTo(otherPos: Vec3d): Double {
    return this.boundingBox.squaredBoxedDistanceTo(otherPos)
}

fun Box.squaredBoxedDistanceTo(otherPos: Vec3d): Double {
    val pos = getNearestPoint(otherPos, this)

    return pos.squaredDistanceTo(otherPos)
}

fun Entity.interpolateCurrentPosition(tickDelta: Float): Vec3 {
    return Vec3(
        this.lastRenderX + (this.x - this.lastRenderX) * tickDelta,
        this.lastRenderY + (this.y - this.lastRenderY) * tickDelta,
        this.lastRenderZ + (this.z - this.lastRenderZ) * tickDelta
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
 * that is actually applied.
 */
@Suppress("detekt:complexity")
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
