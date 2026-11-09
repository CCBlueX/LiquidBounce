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

import it.unimi.dsi.fastutil.floats.FloatArraySet
import it.unimi.dsi.fastutil.floats.FloatArrays
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.LivingEntity.BASE_HORIZONTAL_AIR_DRAG
import net.minecraft.world.entity.LivingEntity.BASE_VERTICAL_AIR_DRAG
import net.minecraft.world.entity.LivingEntity.DEFAULT_BASE_GRAVITY
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.FenceGateBlock
import net.minecraft.world.level.block.SlimeBlock
import net.minecraft.world.level.block.WallBlock
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A player walking, sprinting and jumping on land, tick by tick as LivingEntity.aiStep moves it: block friction,
 * speed and jump factors, the sprint jump boost, air control, gravity, drag, collisions with stepping up, ceilings
 * and landing, and slime bounces, with the speed and jump of [traits]. Sneaking slows the keys down, but does not stop
 * the player at edges. Fluids, climbing and slow falling are left out; callers keep the player out of them.
 *
 * Collisions come from [terrain], so the same motion serves route searches off the game thread and the live world.
 */
@Suppress("TooManyFunctions")
internal class PlayerMotion(
    private val terrain: NavigationTerrain,
    private val traits: MotionTraits = MotionTraits.DEFAULT,
    private val movedBoxes: Long2ObjectOpenHashMap<DoubleArray> = Long2ObjectOpenHashMap(),
) {

    var x = 0.0
    var y = 0.0
    var z = 0.0
    var motionX = 0.0
    var motionY = 0.0
    var motionZ = 0.0
    var onGround = false
        private set
    var horizontalCollision = false
        private set
    var sprinting = false
        private set

    /**
     * Ticks spent in the air since the last time on the ground.
     */
    var airTicks = 0
        private set

    /**
     * Highest feet height since leaving the ground, for the fall distance of the landing.
     */
    var peakY = 0.0
        private set

    private var jumpDelay = 0
    private var supportX = 0
    private var supportY = 0
    private var supportZ = 0
    private var hasSupport = false

    /**
     * Entity.onGroundNoBlocks: on the ground the last tick without any block below.
     */
    private var groundWithoutBlocks = false

    /**
     * Boxes of the blocks near the player, six coordinates each: minimum x, y, z, then maximum x, y, z.
     */
    private var colliders = DoubleArray(BOX * INITIAL_COLLIDERS)
    private var colliderCount = 0
    private val moving = DoubleArray(BOX)

    fun reset(position: Vec3, motion: Vec3, onGround: Boolean, sprinting: Boolean = false, jumpDelay: Int = 0) {
        x = position.x
        y = position.y
        z = position.z
        motionX = motion.x
        motionY = motion.y
        motionZ = motion.z
        this.onGround = onGround
        this.sprinting = sprinting
        horizontalCollision = false
        this.jumpDelay = jumpDelay
        airTicks = 0
        peakY = y
        if (onGround) {
            findSupport(0.0, 0.0)
        } else {
            hasSupport = false
        }
        groundWithoutBlocks = onGround && !hasSupport
    }

    /**
     * Starts from where [player] is now. Call on the render thread.
     */
    fun reset(player: Player) {
        reset(player.position(), player.deltaMovement, player.onGround(), player.isSprinting, player.noJumpDelay)
        player.mainSupportingBlockPos.ifPresent { support ->
            supportX = support.x
            supportY = support.y
            supportZ = support.z
            hasSupport = true
        }
    }

    /**
     * A copy of this motion that shares its collision cache.
     */
    fun copy() = PlayerMotion(terrain, traits, movedBoxes).also { it.copyFrom(this) }

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
        airTicks = other.airTicks
        peakY = other.peakY
        jumpDelay = other.jumpDelay
        supportX = other.supportX
        supportY = other.supportY
        supportZ = other.supportZ
        hasSupport = other.hasSupport
        groundWithoutBlocks = other.groundWithoutBlocks
        landingFall = other.landingFall
    }

    val position: Vec3
        get() = Vec3(x, y, z)

    val horizontalSpeed: Double
        get() = Mth.length(motionX, motionZ)

    /**
     * Fall distance of the last landing, from the highest point since leaving the ground.
     */
    var landingFall = 0.0
        private set

    /**
     * One tick with [forward] and [strafe] keys (-1, 0 or 1, strafe positive to the left) relative to [yaw].
     * Sprinting needs [sprint], a forward key, no [sneak] and no wall bumped into the tick before.
     */
    @Suppress("LongParameterList")
    fun tick(yaw: Float, forward: Int, strafe: Int, jump: Boolean, sprint: Boolean, sneak: Boolean = false) {
        if (jumpDelay > 0) {
            jumpDelay--
        }
        if (motionX * motionX + motionZ * motionZ < MIN_HORIZONTAL_SQR) {
            motionX = 0.0
            motionZ = 0.0
        }
        if (abs(motionY) < LivingEntity.MIN_MOVEMENT_DISTANCE) {
            motionY = 0.0
        }

        sprinting = sprint && forward > 0 && !horizontalCollision && !sneak

        if (jump) {
            if (onGround && jumpDelay == 0) {
                jumpFromGround(yaw)
                jumpDelay = JUMP_DELAY
            }
        } else {
            jumpDelay = 0
        }

        travel(yaw, forward, strafe, if (sneak) INPUT_SCALE * SNEAK_SPEED else INPUT_SCALE)
    }

    private fun jumpFromGround(yaw: Float) {
        motionY = maxOf((traits.jumpStrength * jumpFactor() + traits.jumpBoost).toDouble(), motionY)
        if (sprinting) {
            val angle = (yaw * Mth.DEG_TO_RAD).toDouble()
            motionX += -Mth.sin(angle).toDouble() * SPRINT_JUMP_BOOST
            motionZ += Mth.cos(angle).toDouble() * SPRINT_JUMP_BOOST
        }
    }

    private fun travel(yaw: Float, forward: Int, strafe: Int, inputScale: Float) {
        val blockFriction = if (onGround) movementBlock().friction else 1f
        val speed = when {
            !onGround -> if (sprinting) SPRINT_AIR_SPEED else AIR_SPEED
            blockFriction > PLAIN_FRICTION ->
                movementSpeed() * (GROUND_FACTOR / (blockFriction * blockFriction * blockFriction))
            else -> movementSpeed()
        }
        accelerate(yaw, forward, strafe, speed, inputScale)
        move(motionX, motionY, motionZ)

        val friction = (blockFriction * BASE_HORIZONTAL_AIR_DRAG).toDouble()
        motionX *= friction
        motionY = (motionY - DEFAULT_BASE_GRAVITY) * BASE_VERTICAL_AIR_DRAG
        motionZ *= friction
    }

    private fun movementSpeed() = if (sprinting) traits.walkSpeed * SPRINT_FACTOR else traits.walkSpeed

    /**
     * LocalPlayer.modifyInput and Entity.getInputVector: the keys scaled by [inputScale], stretched to the unit square
     * and turned by [yaw].
     */
    @Suppress("LongParameterList")
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
        val lengthSqr = vectorX * vectorX + vectorZ * vectorZ
        if (lengthSqr > 1.0) {
            val vectorLength = sqrt(lengthSqr)
            vectorX /= vectorLength
            vectorZ /= vectorLength
        }
        vectorX *= speed
        vectorZ *= speed

        val angle = (yaw * Mth.DEG_TO_RAD).toDouble()
        val sin = Mth.sin(angle)
        val cos = Mth.cos(angle)
        motionX += vectorX * cos - vectorZ * sin
        motionZ += vectorZ * cos + vectorX * sin
    }

    private fun move(dx: Double, dy: Double, dz: Double) {
        val movement = collide(dx, dy, dz)
        x += movement.x
        y += movement.y
        z += movement.z

        val xCollision = !Mth.equal(dx, movement.x)
        val zCollision = !Mth.equal(dz, movement.z)
        horizontalCollision = xCollision || zCollision
        val verticalCollision = dy != movement.y
        val wasOnGround = onGround
        onGround = verticalCollision && dy < 0.0
        if (onGround) {
            findSupport(0.0, 0.0)
            // Moved off the edge after landing: the block left behind still counts.
            if (!hasSupport && !groundWithoutBlocks) {
                findSupport(movement.x, movement.z)
            }
            groundWithoutBlocks = !hasSupport
        } else {
            hasSupport = false
            groundWithoutBlocks = false
        }

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
            airTicks = 0
            peakY = y
        } else {
            airTicks++
            peakY = maxOf(peakY, y)
        }

        val speedFactor = speedFactor()
        motionX *= speedFactor
        motionZ *= speedFactor
    }

    /**
     * Vertical motion after hitting a floor or ceiling: none, or the rebound of a bouncy floor.
     */
    private fun bounce(moved: Double): Double {
        if (!onGround || !hasSupport) {
            return 0.0
        }

        val state = terrain.state(supportX, legacyOnY(), supportZ)
        val restitution = state.block.bounceRestitution.toDouble()
        if (-motionY <= DEFAULT_BASE_GRAVITY || restitution <= 0.0) {
            return 0.0
        }

        val portion = moved / motionY
        val compensation = portion * DEFAULT_BASE_GRAVITY
        val drag = Mth.lerp(portion, 1.0, BASE_VERTICAL_AIR_DRAG.toDouble())
        return (compensation - motionY) * drag * restitution
    }

    private fun stepOn() {
        if (movementBlock() is SlimeBlock && abs(motionY) < SLIME_STEP_MOTION) {
            val scale = SLIME_STEP_BASE + abs(motionY) * SLIME_STEP_FACTOR
            motionX *= scale
            motionZ *= scale
        }
    }

    private fun legacyOnY() = Mth.floor(y - Entity.DELTA_AFFECTED_BY_BLOCKS_BELOW_0_2)

    /**
     * Entity.getBlockPosBelowThatAffectsMyMovement: the supporting block's column, below the feet.
     */
    private fun movementBlock(): Block {
        if (!hasSupport) {
            return terrain.state(Mth.floor(x), Mth.floor(y - Entity.DELTA_AFFECTED_BY_BLOCKS_BELOW_0_5), Mth.floor(z))
                .block
        }

        val support = terrain.state(supportX, supportY, supportZ)
        if (support.block is WallBlock || support.block is FenceGateBlock) {
            return support.block
        }
        return terrain.state(supportX, Mth.floor(y - Entity.DELTA_AFFECTED_BY_BLOCKS_BELOW_0_5), supportZ).block
    }

    private fun jumpFactor(): Float {
        val here = terrain.state(Mth.floor(x), Mth.floor(y), Mth.floor(z)).block.jumpFactor
        return if (here == 1f) movementBlock().jumpFactor else here
    }

    private fun speedFactor(): Double {
        val here = terrain.state(Mth.floor(x), Mth.floor(y), Mth.floor(z))
        val factor = here.block.speedFactor
        if (factor != 1f || here.fluidState.isEmpty.not()) {
            return factor.toDouble()
        }
        return movementBlock().speedFactor.toDouble()
    }

    /**
     * Entity.checkSupportingBlock: the block right below the feet, [backX] [backZ] back, closest to them.
     */
    private fun findSupport(backX: Double, backZ: Double) {
        val minX = x - backX - HALF_WIDTH
        val minZ = z - backZ - HALF_WIDTH
        val probe = AABB(minX, y - SUPPORT_PROBE, minZ, minX + 2 * HALF_WIDTH, y, minZ + 2 * HALF_WIDTH)
        var best = Double.MAX_VALUE
        hasSupport = false
        for (bx in Mth.floor(probe.minX)..Mth.floor(probe.maxX)) {
            for (bz in Mth.floor(probe.minZ)..Mth.floor(probe.maxZ)) {
                for (by in Mth.floor(probe.minY) - 1..Mth.floor(probe.maxY)) {
                    if (!intersects(movedBoxes(bx, by, bz), probe)) {
                        continue
                    }

                    val dx = bx + 0.5 - x
                    val dy = by + 0.5 - y
                    val dz = bz + 0.5 - z
                    val distance = dx * dx + dy * dy + dz * dz
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

    private fun box(): AABB = AABB(x - HALF_WIDTH, y, z - HALF_WIDTH, x + HALF_WIDTH, y + HEIGHT, z + HALF_WIDTH)

    /**
     * Whether the player stands on the block at [x] [y] [z], the one Entity.checkSupportingBlock picks.
     */
    fun standsOn(x: Int, y: Int, z: Int) =
        onGround && hasSupport && supportX == x && supportY == y && supportZ == z

    /**
     * How far the player could still move, along the shorter way, and keep the box on the block at [blockX]
     * [blockY] [blockZ]; on a thin top such as a pane the box may hang over both sides.
     */
    fun footing(blockX: Int, blockY: Int, blockZ: Int): Double {
        val boxes = movedBoxes(blockX, blockY, blockZ)
        var best = 0.0
        for (offset in boxes.indices step BOX) {
            val alongX = minOf(x + HALF_WIDTH - boxes[offset], boxes[offset + AXES] - (x - HALF_WIDTH))
            val alongZ = minOf(z + HALF_WIDTH - boxes[offset + Z], boxes[offset + AXES + Z] - (z - HALF_WIDTH))
            best = maxOf(best, minOf(alongX, alongZ))
        }
        return best
    }

    /**
     * Entity.collide: the movement left after the blocks in the way, stepping up onto ledges up to [STEP].
     */
    private fun collide(dx: Double, dy: Double, dz: Double): Vec3 {
        val box = box()
        val movement = Vec3(dx, dy, dz)
        val step = if (movement.lengthSqr() == 0.0) {
            movement
        } else {
            collideWith(movement, box, box.expandTowards(movement))
        }
        val xCollision = dx != step.x
        val zCollision = dz != step.z
        val groundAfter = dy != step.y && dy < 0.0
        if (!(groundAfter || onGround) || !(xCollision || zCollision)) {
            return step
        }

        val grounded = if (groundAfter) box.move(0.0, step.y, 0.0) else box
        var region = grounded.expandTowards(dx, STEP.toDouble(), dz)
        if (!groundAfter) {
            region = region.expandTowards(0.0, -STEP_PROBE, 0.0)
        }
        collectColliders(region)
        for (height in stepHeights(grounded, step.y.toFloat())) {
            val stepped = collideWithColliders(dx, height.toDouble(), dz, grounded)
            if (stepped.horizontalDistanceSqr() > step.horizontalDistanceSqr()) {
                return stepped.subtract(0.0, box.minY - grounded.minY, 0.0)
            }
        }
        return step
    }

    private fun collideWith(movement: Vec3, box: AABB, region: AABB): Vec3 {
        collectColliders(region)
        return collideWithColliders(movement.x, movement.y, movement.z, box)
    }

    /**
     * Shapes.collide over the colliders, axis by axis in the order of Direction.axisStepOrder.
     */
    private fun collideWithColliders(dx: Double, dy: Double, dz: Double, box: AABB): Vec3 {
        if (colliderCount == 0) {
            return Vec3(dx, dy, dz)
        }

        moving[0] = box.minX
        moving[1] = box.minY
        moving[2] = box.minZ
        moving[3] = box.maxX
        moving[4] = box.maxY
        moving[5] = box.maxZ
        val movedY = clip(Y, dy)
        val movedX: Double
        val movedZ: Double
        if (abs(dx) < abs(dz)) {
            movedZ = clip(Z, dz)
            movedX = clip(X, dx)
        } else {
            movedX = clip(X, dx)
            movedZ = clip(Z, dz)
        }
        return Vec3(movedX, movedY, movedZ)
    }

    /**
     * VoxelShape.collide: how far the [moving] box gets along [axis] before a collider stops it, then moved there.
     * Colliders it already overlaps along the axis do not stop it.
     */
    private fun clip(axis: Int, movement: Double): Double {
        if (abs(movement) < EPSILON) {
            return 0.0
        }

        var moved = movement
        for (offset in 0 until colliderCount * BOX step BOX) {
            moved = clipBy(offset, axis, moved)
            if (abs(moved) < EPSILON) {
                moved = 0.0
                break
            }
        }

        moving[axis] += moved
        moving[AXES + axis] += moved
        return moved
    }

    /**
     * [moved] along [axis], cut short where the collider at [offset] is in the way.
     */
    private fun clipBy(offset: Int, axis: Int, moved: Double): Double {
        if (!isAcross(offset, (axis + 1) % AXES) || !isAcross(offset, (axis + 2) % AXES)) {
            return moved
        }
        return if (moved > 0.0) {
            val gap = colliders[offset + axis] - moving[AXES + axis]
            if (gap > -EPSILON) minOf(moved, gap) else moved
        } else {
            val gap = colliders[offset + AXES + axis] - moving[axis]
            if (gap < EPSILON) maxOf(moved, gap) else moved
        }
    }

    /**
     * Whether the collider at [offset] and the [moving] box overlap along [axis].
     */
    private fun isAcross(offset: Int, axis: Int) =
        colliders[offset + AXES + axis] - EPSILON > moving[axis] &&
            colliders[offset + axis] + EPSILON < moving[AXES + axis]

    /**
     * Entity.collectCandidateStepUpHeights: the heights of the colliders' faces up to a step above [box].
     */
    private fun stepHeights(box: AABB, skip: Float): FloatArray {
        val candidates = FloatArraySet(4)
        for (offset in 0 until colliderCount * BOX step BOX) {
            for (face in Y..AXES + Y step AXES) {
                val relative = (colliders[offset + face] - box.minY).toFloat()
                if (relative >= 0f && relative != skip && relative <= STEP) {
                    candidates.add(relative)
                }
            }
        }

        val sorted = candidates.toFloatArray()
        FloatArrays.unstableSort(sorted)
        return sorted
    }

    private fun collectColliders(region: AABB) {
        colliderCount = 0
        for (bx in Mth.floor(region.minX - EPSILON)..Mth.floor(region.maxX + EPSILON)) {
            for (bz in Mth.floor(region.minZ - EPSILON)..Mth.floor(region.maxZ + EPSILON)) {
                // One layer below for blocks taller than a block, like fences.
                for (by in Mth.floor(region.minY - EPSILON) - 1..Mth.floor(region.maxY + EPSILON)) {
                    addColliders(movedBoxes(bx, by, bz))
                }
            }
        }
    }

    private fun addColliders(boxes: DoubleArray) {
        if (boxes.isEmpty()) {
            return
        }
        val used = colliderCount * BOX
        if (used + boxes.size > colliders.size) {
            colliders = colliders.copyOf(maxOf(colliders.size * 2, used + boxes.size))
        }
        boxes.copyInto(colliders, used)
        colliderCount += boxes.size / BOX
    }

    /**
     * The collision boxes of the block at [x] [y] [z] in world coordinates, laid out like [colliders].
     */
    private fun movedBoxes(x: Int, y: Int, z: Int): DoubleArray {
        val key = BlockPos.asLong(x, y, z)
        movedBoxes.get(key)?.let { return it }

        val local = terrain.shape(x, y, z).toAabbs()
        val boxes = DoubleArray(local.size * BOX)
        for ((index, box) in local.withIndex()) {
            val offset = index * BOX
            boxes[offset] = box.minX + x
            boxes[offset + 1] = box.minY + y
            boxes[offset + 2] = box.minZ + z
            boxes[offset + AXES] = box.maxX + x
            boxes[offset + AXES + 1] = box.maxY + y
            boxes[offset + AXES + 2] = box.maxZ + z
        }
        movedBoxes.put(key, boxes)
        return boxes
    }

    private fun intersects(boxes: DoubleArray, box: AABB): Boolean {
        for (offset in boxes.indices step BOX) {
            val alongX = boxes[offset] < box.maxX && boxes[offset + AXES] > box.minX
            val alongY = boxes[offset + Y] < box.maxY && boxes[offset + AXES + Y] > box.minY
            val alongZ = boxes[offset + Z] < box.maxZ && boxes[offset + AXES + Z] > box.minZ
            if (alongX && alongY && alongZ) {
                return true
            }
        }
        return false
    }

    companion object {
        const val HALF_WIDTH = NavigationTerrain.PLAYER_WIDTH / 2.0
        private const val HEIGHT = NavigationTerrain.PLAYER_HEIGHT
        private const val STEP = 0.6f
        private const val JUMP_DELAY = 10

        /**
         * Motion of a player standing still on the ground, which gravity keeps pulling down every tick.
         */
        val RESTING: Vec3 = Vec3(0.0, -DEFAULT_BASE_GRAVITY * BASE_VERTICAL_AIR_DRAG, 0.0)

        private const val SPRINT_JUMP_BOOST = 0.2f
        private const val SPRINT_FACTOR = 1.3f
        private const val AIR_SPEED = 0.02f
        private const val SPRINT_AIR_SPEED = 0.025999999f
        private const val PLAIN_FRICTION = 0.6f
        private const val GROUND_FACTOR = 0.21600002f
        private const val INPUT_SCALE = 0.98f
        private const val SNEAK_SPEED = 0.3f
        private const val MIN_HORIZONTAL_SQR = 9.0E-6
        private const val SLIME_STEP_MOTION = 0.1
        private const val SLIME_STEP_BASE = 0.4
        private const val SLIME_STEP_FACTOR = 0.2
        private const val SUPPORT_PROBE = 1.0E-6
        private const val STEP_PROBE = 1.0E-5
        private const val EPSILON = 1.0E-7
        private const val X = 0
        private const val Y = 1
        private const val Z = 2
        private const val AXES = 3
        private const val BOX = 2 * AXES
        private const val INITIAL_COLLIDERS = 32
    }

}

/**
 * What sets how fast a player walks and how high it jumps: the movement speed without sprinting, the jump strength
 * and the jump boost of effects.
 */
class MotionTraits(val walkSpeed: Float, val jumpStrength: Float, val jumpBoost: Float) {

    /**
     * How high a jump from flat ground gets the feet.
     */
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
        val DEFAULT = MotionTraits(walkSpeed = 0.1f, jumpStrength = LivingEntity.BASE_JUMP_POWER, jumpBoost = 0f)

        /**
         * The traits of [player] now, effects included. Call on the render thread.
         */
        fun capture(player: Player): MotionTraits {
            val speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED).toFloat()
            return MotionTraits(
                walkSpeed = if (player.isSprinting) speed / SPRINT_FACTOR else speed,
                jumpStrength = player.getAttributeValue(Attributes.JUMP_STRENGTH).toFloat(),
                jumpBoost = player.jumpBoostPower,
            )
        }

        private const val SPRINT_FACTOR = 1.3f
    }

}
