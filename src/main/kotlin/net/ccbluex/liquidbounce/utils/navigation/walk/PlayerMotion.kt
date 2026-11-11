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

package net.ccbluex.liquidbounce.utils.navigation.walk

import net.ccbluex.liquidbounce.utils.math.fastCos
import net.ccbluex.liquidbounce.utils.math.fastSin
import net.ccbluex.liquidbounce.utils.math.toRadians
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity.DELTA_AFFECTED_BY_BLOCKS_BELOW_0_2
import net.minecraft.world.entity.Entity.DELTA_AFFECTED_BY_BLOCKS_BELOW_0_5
import net.minecraft.world.entity.LivingEntity.BASE_HORIZONTAL_AIR_DRAG
import net.minecraft.world.entity.LivingEntity.BASE_JUMP_POWER
import net.minecraft.world.entity.LivingEntity.BASE_VERTICAL_AIR_DRAG
import net.minecraft.world.entity.LivingEntity.DEFAULT_BASE_GRAVITY
import net.minecraft.world.entity.LivingEntity.MIN_MOVEMENT_DISTANCE
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.FenceGateBlock
import net.minecraft.world.level.block.SlimeBlock
import net.minecraft.world.level.block.WallBlock
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

private const val SPRINT_FACTOR = 1.3f
private const val SUPPORT_EPSILON = 1.0E-6

/**
 * A player walking, sprinting, sneaking and jumping on land, tick for tick as LivingEntity.aiStep moves it.
 * Fluids, climbing and slow falling are left out. Collisions come from [terrain], so it runs off the game thread.
 */
@Suppress("TooManyFunctions")
internal class PlayerMotion(
    private val terrain: NavigationTerrain,
    private val traits: MotionTraits = MotionTraits.DEFAULT,
    private val colliders: BlockColliders = BlockColliders(terrain),
) {

    var x = 0.0
        private set
    var y = 0.0
        private set
    var z = 0.0
        private set
    var motionX = 0.0
        private set
    var motionY = 0.0
        private set
    var motionZ = 0.0
        private set
    var onGround = false
        private set
    var horizontalCollision = false
        private set
    var sprinting = false
        private set

    /** Fall distance of the last landing, from the highest point since leaving the ground. */
    var landingFall = 0.0
        private set

    private var peakY = 0.0
    private var jumpDelay = 0
    private var supportX = 0
    private var supportY = 0
    private var supportZ = 0
    private var hasSupport = false

    /** Entity.onGroundNoBlocks */
    private var groundWithoutBlocks = false

    private var crouching = false
    private var sneaking = false
    private var heldX = 0.0
    private var heldZ = 0.0

    val position: Vec3
        get() = Vec3(x, y, z)

    val horizontalSpeed: Double
        get() = Mth.length(motionX, motionZ)

    fun reset(position: Vec3, motion: Vec3, onGround: Boolean, sprinting: Boolean = false, jumpDelay: Int = 0) {
        x = position.x
        y = position.y
        z = position.z
        motionX = motion.x
        motionY = motion.y
        motionZ = motion.z
        this.onGround = onGround
        this.sprinting = sprinting
        this.jumpDelay = jumpDelay
        horizontalCollision = false
        crouching = false
        peakY = y
        if (onGround) findSupport(0.0, 0.0) else hasSupport = false
        groundWithoutBlocks = onGround && !hasSupport
    }

    /** Call on the render thread. */
    fun reset(player: Player) {
        reset(player.position(), player.deltaMovement, player.onGround(), player.isSprinting, player.noJumpDelay)
        crouching = player.isCrouching
        player.mainSupportingBlockPos.ifPresent { support ->
            supportX = support.x
            supportY = support.y
            supportZ = support.z
            hasSupport = true
        }
    }

    fun copy() = PlayerMotion(terrain, traits, colliders).also { it.copyFrom(this) }

    fun copyFrom(other: PlayerMotion) {
        x = other.x
        y = other.y
        z = other.z
        motionX = other.motionX
        motionY = other.motionY
        motionZ = other.motionZ
        onGround = other.onGround
        horizontalCollision = other.horizontalCollision
        sprinting = other.sprinting
        landingFall = other.landingFall
        peakY = other.peakY
        jumpDelay = other.jumpDelay
        supportX = other.supportX
        supportY = other.supportY
        supportZ = other.supportZ
        hasSupport = other.hasSupport
        groundWithoutBlocks = other.groundWithoutBlocks
        crouching = other.crouching
    }

    /** One tick with the [forward] and [strafe] keys (-1, 0 or 1, strafe positive to the left) relative to [yaw]. */
    fun tick(yaw: Float, forward: Int, strafe: Int, jump: Boolean, sprint: Boolean, sneak: Boolean = false) {
        if (jumpDelay > 0) {
            jumpDelay--
        }
        if (Mth.lengthSquared(motionX, motionZ) < MIN_HORIZONTAL_SQR) {
            motionX = 0.0
            motionZ = 0.0
        }
        if (abs(motionY) < MIN_MOVEMENT_DISTANCE) {
            motionY = 0.0
        }

        // LocalPlayer.aiStep works out crouching before it reads the keys, so the sneak key slows down a tick later.
        val slow = crouching
        crouching = sneak
        sneaking = sneak
        sprinting = sprint && forward > 0 && !horizontalCollision && (sprinting || !slow)

        if (!jump) {
            jumpDelay = 0
        } else if (onGround && jumpDelay == 0) {
            jumpFromGround(yaw)
            jumpDelay = JUMP_DELAY
        }

        travel(yaw, forward, strafe, if (slow) INPUT_FRICTION * SNEAK_SPEED else INPUT_FRICTION)
    }

    private fun jumpFromGround(yaw: Float) {
        motionY = maxOf((traits.jumpStrength * jumpFactor() + traits.jumpBoost).toDouble(), motionY)
        if (sprinting) {
            val angle = yaw.toRadians()
            motionX += -angle.fastSin().toDouble() * SPRINT_JUMP_BOOST
            motionZ += angle.fastCos().toDouble() * SPRINT_JUMP_BOOST
        }
    }

    private fun travel(yaw: Float, forward: Int, strafe: Int, inputScale: Float) {
        val blockFriction = if (onGround) movementBlock().friction else 1f
        val walkSpeed = if (sprinting) traits.walkSpeed * SPRINT_FACTOR else traits.walkSpeed
        val speed = when {
            !onGround -> if (sprinting) SPRINT_AIR_SPEED else AIR_SPEED
            blockFriction > PLAIN_FRICTION ->
                walkSpeed * (GROUND_FACTOR / (blockFriction * blockFriction * blockFriction))
            else -> walkSpeed
        }
        accelerate(yaw, forward, strafe, speed, inputScale)
        move(motionX, motionY, motionZ)

        val friction = (blockFriction * BASE_HORIZONTAL_AIR_DRAG).toDouble()
        motionX *= friction
        motionY = (motionY - DEFAULT_BASE_GRAVITY) * BASE_VERTICAL_AIR_DRAG
        motionZ *= friction
    }

    /** LocalPlayer.modifyInput and Entity.getInputVector: the keys stretched to the unit square and turned by [yaw]. */
    private fun accelerate(yaw: Float, forward: Int, strafe: Int, speed: Float, inputScale: Float) {
        if (forward == 0 && strafe == 0) {
            return
        }

        val length = sqrt((strafe * strafe + forward * forward).toFloat())
        var inputX = strafe / length * inputScale
        var inputZ = forward / length * inputScale
        val absX = abs(inputX)
        val absZ = abs(inputZ)
        val tan = if (absZ > absX) absX / absZ else absZ / absX
        val stretched = minOf(inputScale * Mth.sqrt(1f + tan * tan), 1f) / inputScale
        inputX *= stretched
        inputZ *= stretched

        var vectorX = inputX.toDouble()
        var vectorZ = inputZ.toDouble()
        val lengthSqr = Mth.lengthSquared(vectorX, vectorZ)
        if (lengthSqr > 1.0) {
            val vectorLength = sqrt(lengthSqr)
            vectorX /= vectorLength
            vectorZ /= vectorLength
        }
        vectorX *= speed
        vectorZ *= speed

        val angle = yaw.toRadians()
        val sin = angle.fastSin()
        val cos = angle.fastCos()
        motionX += vectorX * cos - vectorZ * sin
        motionZ += vectorZ * cos + vectorX * sin
    }

    private fun move(wantedX: Double, dy: Double, wantedZ: Double) {
        var dx = wantedX
        var dz = wantedZ
        if (sneaking && dy <= 0.0 && isAboveGround()) {
            backOffFromEdge(dx, dz)
            dx = heldX
            dz = heldZ
        }
        val movement = collide(dx, dy, dz)
        x += movement.x
        y += movement.y
        z += movement.z

        val xCollision = !Mth.equal(dx, movement.x)
        val zCollision = !Mth.equal(dz, movement.z)
        val verticalCollision = dy != movement.y
        val wasOnGround = onGround
        horizontalCollision = xCollision || zCollision
        onGround = verticalCollision && dy < 0.0
        updateSupport(movement)

        if (xCollision) {
            motionX = 0.0
        }
        if (zCollision) {
            motionZ = 0.0
        }
        if (verticalCollision) {
            motionY = bounce(movement.y)
        }

        if (onGround) {
            stepOn()
            if (!wasOnGround) {
                landingFall = peakY - y
            }
            peakY = y
        } else {
            peakY = maxOf(peakY, y)
        }

        val speedFactor = speedFactor()
        motionX *= speedFactor
        motionZ *= speedFactor
    }

    /** Player.maybeBackOffFromEdge: how much of [dx] and [dz] keeps a sneaking player over the ground it is on. */
    private fun backOffFromEdge(dx: Double, dz: Double) {
        heldX = dx
        heldZ = dz
        while (heldX != 0.0 && canFall(heldX, 0.0, STEP_HEIGHT.toDouble())) {
            heldX = shortened(heldX)
        }
        while (heldZ != 0.0 && canFall(0.0, heldZ, STEP_HEIGHT.toDouble())) {
            heldZ = shortened(heldZ)
        }
        while (heldX != 0.0 && heldZ != 0.0 && canFall(heldX, heldZ, STEP_HEIGHT.toDouble())) {
            heldX = shortened(heldX)
            heldZ = shortened(heldZ)
        }
    }

    private fun shortened(delta: Double) = if (abs(delta) <= EDGE_STEP) 0.0 else delta - sign(delta) * EDGE_STEP

    private fun isAboveGround(): Boolean {
        val fallen = peakY - y
        return onGround || fallen < STEP_HEIGHT && !canFall(0.0, 0.0, STEP_HEIGHT - fallen)
    }

    /** Whether nothing is within [depth] below the player moved by [dx] and [dz]. */
    private fun canFall(dx: Double, dz: Double, depth: Double): Boolean {
        val minX = x - HALF_WIDTH + EDGE_INSET + dx
        val minZ = z - HALF_WIDTH + EDGE_INSET + dz
        val width = 2 * (HALF_WIDTH - EDGE_INSET)
        val probe = AABB(minX, y - depth - EDGE_INSET, minZ, minX + width, y, minZ + width)
        for (bx in Mth.floor(probe.minX)..Mth.floor(probe.maxX)) {
            for (bz in Mth.floor(probe.minZ)..Mth.floor(probe.maxZ)) {
                for (by in Mth.floor(probe.minY) - 1..Mth.floor(probe.maxY)) {
                    if (colliders.intersects(bx, by, bz, probe)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun updateSupport(movement: Vec3) {
        if (!onGround) {
            hasSupport = false
            groundWithoutBlocks = false
            return
        }

        findSupport(0.0, 0.0)
        // Moved off the edge after landing: the block left behind still counts.
        if (!hasSupport && !groundWithoutBlocks) {
            findSupport(movement.x, movement.z)
        }
        groundWithoutBlocks = !hasSupport
    }

    /** Vertical motion after hitting a floor or ceiling: none, or the rebound of a bouncy floor. */
    private fun bounce(moved: Double): Double {
        if (!onGround || !hasSupport) {
            return 0.0
        }

        val floor = terrain.state(supportX, Mth.floor(y - DELTA_AFFECTED_BY_BLOCKS_BELOW_0_2), supportZ).block
        val restitution = floor.bounceRestitution.toDouble()
        if (-motionY <= DEFAULT_BASE_GRAVITY || restitution <= 0.0) {
            return 0.0
        }

        val portion = moved / motionY
        val compensation = portion * DEFAULT_BASE_GRAVITY
        val drag = Mth.lerp(portion, 1.0, BASE_VERTICAL_AIR_DRAG.toDouble())
        return (compensation - motionY) * drag * restitution
    }

    /** SlimeBlock.stepOn */
    private fun stepOn() {
        if (movementBlock() is SlimeBlock && abs(motionY) < SLIME_STEP_MOTION) {
            val scale = SLIME_STEP_BASE + abs(motionY) * SLIME_STEP_FACTOR
            motionX *= scale
            motionZ *= scale
        }
    }

    /** Entity.getBlockPosBelowThatAffectsMyMovement */
    private fun movementBlock(): Block {
        val below = Mth.floor(y - DELTA_AFFECTED_BY_BLOCKS_BELOW_0_5)
        if (!hasSupport) {
            return terrain.state(Mth.floor(x), below, Mth.floor(z)).block
        }

        val support = terrain.state(supportX, supportY, supportZ).block
        if (support is WallBlock || support is FenceGateBlock) {
            return support
        }
        return terrain.state(supportX, below, supportZ).block
    }

    private fun jumpFactor(): Float {
        val here = terrain.state(Mth.floor(x), Mth.floor(y), Mth.floor(z)).block.jumpFactor
        return if (here == 1f) movementBlock().jumpFactor else here
    }

    private fun speedFactor(): Double {
        val here = terrain.state(Mth.floor(x), Mth.floor(y), Mth.floor(z))
        val factor = here.block.speedFactor
        if (factor != 1f || !here.fluidState.isEmpty) {
            return factor.toDouble()
        }
        return movementBlock().speedFactor.toDouble()
    }

    /** Entity.checkSupportingBlock: the block right below the feet closest to them, looked for [backX] [backZ] back. */
    private fun findSupport(backX: Double, backZ: Double) {
        val minX = x - backX - HALF_WIDTH
        val minZ = z - backZ - HALF_WIDTH
        val probe = AABB(minX, y - SUPPORT_EPSILON, minZ, minX + 2 * HALF_WIDTH, y, minZ + 2 * HALF_WIDTH)
        var best = Double.MAX_VALUE
        hasSupport = false
        for (bx in Mth.floor(probe.minX)..Mth.floor(probe.maxX)) {
            for (bz in Mth.floor(probe.minZ)..Mth.floor(probe.maxZ)) {
                for (by in Mth.floor(probe.minY) - 1..Mth.floor(probe.maxY)) {
                    if (!colliders.intersects(bx, by, bz, probe)) {
                        continue
                    }

                    val distance = Mth.lengthSquared(bx + 0.5 - x, by + 0.5 - y, bz + 0.5 - z)
                    if (distance < best) {
                        best = distance
                        supportX = bx
                        supportY = by
                        supportZ = bz
                        hasSupport = true
                    }
                }
            }
        }
    }

    /** Whether the player stands on the block at [x] [y] [z], the one Entity.checkSupportingBlock picks. */
    fun standsOn(x: Int, y: Int, z: Int) =
        onGround && hasSupport && supportX == x && supportY == y && supportZ == z

    /** How far the player could still move, the shorter way, and keep its box on the block. */
    fun footing(blockX: Int, blockY: Int, blockZ: Int) = colliders.footing(blockX, blockY, blockZ, x, z, HALF_WIDTH)

    /** Entity.collide: the movement the blocks in the way leave, stepping up ledges of up to [STEP_HEIGHT]. */
    private fun collide(dx: Double, dy: Double, dz: Double): Vec3 {
        val box = AABB(x - HALF_WIDTH, y, z - HALF_WIDTH, x + HALF_WIDTH, y + HEIGHT, z + HALF_WIDTH)
        val movement = Vec3(dx, dy, dz)
        val direct = if (movement.lengthSqr() == 0.0) {
            movement
        } else {
            colliders.collect(box.expandTowards(movement))
            colliders.collide(dx, dy, dz, box)
        }
        val lands = dy != direct.y && dy < 0.0
        if (!(lands || onGround) || dx == direct.x && dz == direct.z) {
            return direct
        }

        val grounded = if (lands) box.move(0.0, direct.y, 0.0) else box
        var region = grounded.expandTowards(dx, STEP_HEIGHT.toDouble(), dz)
        if (!lands) {
            region = region.expandTowards(0.0, -STEP_PROBE, 0.0)
        }
        colliders.collect(region)
        for (height in colliders.stepHeights(grounded, STEP_HEIGHT, skip = direct.y.toFloat())) {
            val stepped = colliders.collide(dx, height.toDouble(), dz, grounded)
            if (stepped.horizontalDistanceSqr() > direct.horizontalDistanceSqr()) {
                return stepped.subtract(0.0, box.minY - grounded.minY, 0.0)
            }
        }
        return direct
    }

    companion object {
        const val HALF_WIDTH = NavigationTerrain.PLAYER_WIDTH / 2.0
        const val AIR_SPEED = 0.02f
        const val SPRINT_AIR_SPEED = 0.025999999f
        const val PLAIN_FRICTION = 0.6f

        // LivingEntity.INPUT_FRICTION, which is protected.
        const val INPUT_FRICTION = 0.98f

        /** Motion of a player standing still on the ground, which gravity keeps pulling down every tick. */
        val RESTING: Vec3 = Vec3(0.0, -DEFAULT_BASE_GRAVITY * BASE_VERTICAL_AIR_DRAG, 0.0)

        private const val HEIGHT = NavigationTerrain.PLAYER_HEIGHT
        private const val STEP_HEIGHT = 0.6f
        private const val JUMP_DELAY = 10
        private const val SPRINT_JUMP_BOOST = 0.2f
        private const val GROUND_FACTOR = 0.21600002f
        private const val SNEAK_SPEED = 0.3f
        private const val MIN_HORIZONTAL_SQR = 9.0E-6
        private const val SLIME_STEP_MOTION = 0.1
        private const val SLIME_STEP_BASE = 0.4
        private const val SLIME_STEP_FACTOR = 0.2
        private const val STEP_PROBE = 1.0E-5
        private const val EDGE_STEP = 0.05
        private const val EDGE_INSET = 1.0E-7
    }

}

/** What sets how fast a player walks and how high it jumps; [walkSpeed] is without sprinting. */
class MotionTraits(val walkSpeed: Float, val jumpStrength: Float, val jumpBoost: Float) {

    /** How high a jump from flat ground gets the feet. */
    val jumpHeight: Double = run {
        var motion = (jumpStrength + jumpBoost).toDouble()
        var height = 0.0
        while (motion > 0.0) {
            height += motion
            motion = (motion - DEFAULT_BASE_GRAVITY) * BASE_VERTICAL_AIR_DRAG
        }
        height
    }

    companion object {
        val DEFAULT = MotionTraits(walkSpeed = 0.1f, jumpStrength = BASE_JUMP_POWER, jumpBoost = 0f)

        /** The traits of [player] now, effects included. Call on the render thread. */
        fun capture(player: Player): MotionTraits {
            val speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED).toFloat()
            return MotionTraits(
                walkSpeed = if (player.isSprinting) speed / SPRINT_FACTOR else speed,
                jumpStrength = player.getAttributeValue(Attributes.JUMP_STRENGTH).toFloat(),
                jumpBoost = player.jumpBoostPower,
            )
        }
    }

}
