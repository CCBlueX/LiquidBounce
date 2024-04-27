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
@file:Suppress("All")

package net.ccbluex.liquidbounce.utils.entity

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap
import it.unimi.dsi.fastutil.objects.Object2DoubleMap
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDegreesRelativeToView
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees
import net.minecraft.block.*
import net.minecraft.client.input.Input
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.FluidState
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.FluidTags
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.util.shape.VoxelShape
import net.minecraft.world.World
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

private const val STEP_HEIGHT = 0.5

class SimulatedPlayer(
    private val player: PlayerEntity,
    var input: SimulatedPlayerInput,
    override var pos: Vec3d,
    var velocity: Vec3d,
    private var boundingBox: Box,
    var yaw: Float,
    private val pitch: Float,
    private var sprinting: Boolean,

    var fallDistance: Float,
    private var jumpingCooldown: Int,
    private var isJumping: Boolean,
    private var isFallFlying: Boolean,
    var onGround: Boolean,
    var horizontalCollision: Boolean,
    private var verticalCollision: Boolean,

    private var touchingWater: Boolean,
    private var isSwimming: Boolean,
    private var submergedInWater: Boolean,
    private var fluidHeight: Object2DoubleMap<TagKey<Fluid>>,
    private var submergedFluidTag: HashSet<TagKey<Fluid>>
) : PlayerSimulation {
    private val world: World
        get() = player.world!!

    companion object {
        fun fromClientPlayer(input: SimulatedPlayerInput): SimulatedPlayer {
            return SimulatedPlayer(
                player,
                input,
                player.pos,
                player.velocity,
                player.boundingBox,
                player.yaw,
                player.pitch,

                player.isSprinting,

                player.fallDistance,
                player.jumpingCooldown,
                player.jumping,
                player.isFallFlying,
                player.isOnGround,
                player.horizontalCollision,
                player.verticalCollision,

                player.isTouchingWater,
                player.isSwimming,
                player.isSubmergedInWater,
                Object2DoubleArrayMap(player.fluidHeight),
                HashSet(player.submergedFluidTag)
            )
        }

        fun fromOtherPlayer(player: PlayerEntity, input: SimulatedPlayerInput): SimulatedPlayer {
            return SimulatedPlayer(
                player,
                input,
                player.pos,
                velocity = player.pos.subtract(player.prevPos),
                player.boundingBox,
                player.yaw,
                player.pitch,

                player.isSprinting,

                player.fallDistance,
                player.jumpingCooldown,
                player.jumping,
                player.isFallFlying,
                player.isOnGround,
                player.horizontalCollision,
                player.verticalCollision,

                player.isTouchingWater,
                player.isSwimming,
                player.isSubmergedInWater,
                Object2DoubleArrayMap(player.fluidHeight),
                HashSet(player.submergedFluidTag)
            )
        }
    }

    private var simulatedTicks: Int = 0
    var clipLedged = false
        private set

    override fun tick() {
        clipLedged = false

        // ignore because world limit it -65
        if (pos.y <= -70) {
            return
        }

        this.input.update()

        checkWaterState()
        updateSubmergedInWaterState()
        updateSwimming()

        // LivingEntity.tickMovement()
        if (this.jumpingCooldown > 0) {
            this.jumpingCooldown--
        }

        this.isJumping = this.input.jumping

        val d: Vec3d = this.velocity

        var h = d.x
        var i = d.y
        var j = d.z

        if (abs(d.x) < 0.003) {
            h = 0.0
        }
        if (abs(d.y) < 0.003) {
            i = 0.0
        }
        if (abs(d.z) < 0.003) {
            j = 0.0
        }
        if (onGround) {
            this.isFallFlying = false
        }

        this.velocity = Vec3d(h, i, j)

        if (this.isJumping) {
            val k = if (this.isInLava()) this.getFluidHeight(FluidTags.LAVA) else this.getFluidHeight(FluidTags.WATER)
            val bl = this.isTouchingWater() && k > 0.0

            val swimHeight = this.getSwimHeight()

            if (bl && (!this.onGround || k > swimHeight)) {
                this.swimUpward(FluidTags.WATER)
            } else if (this.isInLava() && (!this.onGround || k > swimHeight)) {
                this.swimUpward(FluidTags.LAVA)
            } else if ((this.onGround || bl && k <= swimHeight) && jumpingCooldown == 0) {
                this.jump()
                jumpingCooldown = 10
            }
        }

        val sidewaysSpeed = input.movementSideways * 0.98
        val forwardSpeed = input.movementForward * 0.98
        val upwardsSpeed = 0.0

        if (this.hasStatusEffect(StatusEffects.SLOW_FALLING) || this.hasStatusEffect(StatusEffects.LEVITATION)) {
            this.onLanding()
        }

        this.travel(Vec3d(sidewaysSpeed, upwardsSpeed, forwardSpeed))
    }

    private fun travel(movementInput: Vec3d) {
        // PlayerEntity
        if (this.isSwimming && !this.player.hasVehicle()) {
            val g = this.getRotationVector().y
            val h = if (g < -0.2) 0.085 else 0.06
            if (g <= 0.0 || this.input.jumping || !this.player.world
                .getBlockState(BlockPos.ofFloored(this.pos.x, this.pos.y + 1.0 - 0.1, this.pos.z))
                .fluidState.isEmpty
            ) {
                velocity = velocity.add(0.0, (g - velocity.y) * h, 0.0)
            }
        }

//        if (this.abilities.flying && !this.hasVehicle()) {
        val beforeTravelVelocityY = this.velocity.y
//            super.travel(movementInput)
//            val vec3d2: Vec3d = this.getVelocity()
//            this.setVelocity(vec3d2.x, g * 0.6, vec3d2.z)
//            onLanding()
//            this.setFlag(7, false)
//        }

        var d = 0.08
        val bl: Boolean = velocity.y <= 0.0
        if (velocity.y <= 0.0 && hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            d = 0.01
            this.onLanding()
        }

//        val fluidState: FluidState = this.player.world.getFluidState(pos.toBlockPos())

        if (isTouchingWater() && this.player.shouldSwimInFluids() /*&& !this.player.canWalkOnFluid(fluidState.fluid)*/) {
            val e: Double = this.pos.y
            var f = if (isSprinting()) 0.9f else 0.8f // this.player.getBaseMovementSpeedMultiplier()
            var g = 0.02f
            var h = EnchantmentHelper.getDepthStrider(this.player).toFloat()
            if (h > 3.0f) {
                h = 3.0f
            }
            if (!onGround) {
                h *= 0.5f
            }
            if (h > 0.0f) {
                f += (0.54600006f - f) * h / 3.0f
                g += (this.getMovementSpeed() - g) * h / 3.0f
            }
            if (hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                f = 0.96f
            }
            this.updateVelocity(g, movementInput)
            this.move(velocity)
            var vec3d: Vec3d = velocity
            if (this.horizontalCollision && this.isClimbing()) {
                vec3d = Vec3d(vec3d.x, 0.2, vec3d.z)
            }
            velocity = vec3d.multiply(f.toDouble(), 0.8, f.toDouble())
            val vec3d2: Vec3d = this.player.applyFluidMovingSpeed(d, bl, velocity)
            velocity = vec3d2
            if (this.horizontalCollision && this.doesNotCollide(vec3d2.x, vec3d2.y + 0.6 - this.pos.y + e, vec3d2.z)) {
                this.velocity = Vec3d(vec3d2.x, 0.3, vec3d2.z)
            }
        } else if (isInLava() && this.player.shouldSwimInFluids() /*&& !this.canWalkOnFluid(fluidState.fluid)*/) {
            val e: Double = this.pos.y
            this.updateVelocity(0.02f, movementInput)
            this.move(velocity)
            if (getFluidHeight(FluidTags.LAVA) <= getSwimHeight()) {
                velocity = velocity.multiply(0.5, 0.8, 0.5)
                velocity = this.player.applyFluidMovingSpeed(d, bl, velocity)
            } else {
                velocity = velocity.multiply(0.5)
            }
            if (!this.player.hasNoGravity()) {
                velocity = this.velocity.add(0.0, -d / 4.0, 0.0)
            }
            if (this.horizontalCollision && this.doesNotCollide(
                    velocity.x,
                    velocity.y + 0.6 - this.pos.y + e,
                    velocity.z
                )
            ) {
                velocity = Vec3d(velocity.x, 0.3, velocity.z)
            }
        } else if (this.isFallFlying) {
            var k: Double
            var e: Vec3d = this.velocity
            if (e.y > -0.5) {
                fallDistance = 1.0f
            }
            val vec3d3 = this.getRotationVector()
            val f: Float = this.pitch * (Math.PI.toFloat() / 180)
            val g = sqrt(vec3d3.x * vec3d3.x + vec3d3.z * vec3d3.z)
            val vec3d = e.horizontalLength()
            val i = vec3d3.length()
            var j = MathHelper.cos(f)
            j = (j.toDouble() * (j.toDouble() * 1.0.coerceAtMost(i / 0.4))).toFloat()
            e = this.velocity.add(0.0, d * (-1.0 + j.toDouble() * 0.75), 0.0)
            if (e.y < 0.0 && g > 0.0) {
                k = e.y * -0.1 * j.toDouble()
                e = e.add(vec3d3.x * k / g, k, vec3d3.z * k / g)
            }
            if (f < 0.0f && g > 0.0) {
                k = vec3d * (-MathHelper.sin(f)).toDouble() * 0.04
                e = e.add(-vec3d3.x * k / g, k * 3.2, -vec3d3.z * k / g)
            }
            if (g > 0.0) {
                e = e.add((vec3d3.x / g * vec3d - e.x) * 0.1, 0.0, (vec3d3.z / g * vec3d - e.z) * 0.1)
            }
            this.velocity = e.multiply(0.99, 0.98, 0.99)

            move(this.velocity)
        } else {
            val blockPos = this.getVelocityAffectingPos()
            val p: Float = this.player.world.getBlockState(blockPos).block.slipperiness
            val f = if (onGround) p * 0.91f else 0.91f
            val vec3d6 = this.applyMovementInput(movementInput, p)
            var q = vec3d6.y
            if (hasStatusEffect(StatusEffects.LEVITATION)) {
                q += (0.05 * (getStatusEffect(StatusEffects.LEVITATION)!!.amplifier + 1).toDouble() - vec3d6.y) * 0.2
            } else if (this.player.world.isClient && !this.player.world.isChunkLoaded(blockPos)) {
                q = if (this.pos.y > this.player.world.bottomY.toDouble()) {
                    -0.1
                } else {
                    0.0
                }
            } else if (!this.player.hasNoGravity()) {
                q -= d
            }

            velocity = if (this.player.hasNoDrag()) {
                Vec3d(vec3d6.x, q, vec3d6.z)
            } else {
                Vec3d(vec3d6.x * f.toDouble(), q * 0.9800000190734863, vec3d6.z * f.toDouble())
            }
        }

        // PlayerEntity
        if (player.abilities.flying && !this.player.hasVehicle()) {
            velocity = Vec3d(velocity.x, beforeTravelVelocityY * 0.6, velocity.z)
            this.onLanding()
        }
    }

    private fun applyMovementInput(movementInput: Vec3d?, slipperiness: Float): Vec3d {
        this.updateVelocity(this.getMovementSpeed(slipperiness), movementInput)
        this.velocity = applyClimbingSpeed(this.velocity)
        this.move(this.velocity)

        var vec3d = this.velocity
        if ((horizontalCollision || this.isJumping) && (
            this.isClimbing() || pos.toBlockPos().getState()
                ?.isOf(Blocks.POWDER_SNOW) == true && PowderSnowBlock.canWalkOnPowderSnow(player)
            )
        ) {
            vec3d = Vec3d(vec3d.x, 0.2, vec3d.z)
        }

        return vec3d
    }

    private fun updateVelocity(speed: Float, movementInput: Vec3d?) {
        val vec3d = Entity.movementInputToVelocity(movementInput, speed, this.yaw)

        this.velocity += vec3d
    }

    private fun getMovementSpeed(slipperiness: Float): Float {
        return if (this.onGround) {
            getMovementSpeed() * (0.21600002f / (slipperiness * slipperiness * slipperiness))
        } else {
            this.getAirStrafingSpeed()
        }
    }

    private fun getAirStrafingSpeed(): Float {
        val speed = 0.02f

        if (this.input.sprinting) {
            return (speed + 0.005999999865889549).toFloat()
        }

        return speed
    }

    private fun getMovementSpeed(): Float = 0.10000000149011612.toFloat()

    private fun move(input: Vec3d) {
        val movement = this.adjustMovementForSneaking(input)
        val adjustedMovement = this.adjustMovementForCollisions(movement)

        if (adjustedMovement.lengthSquared() > 1.0E-7) {
            this.pos += adjustedMovement
            this.boundingBox = player.dimensions.getBoxAt(this.pos)
        }

        val xCollision = !MathHelper.approximatelyEquals(movement.x, adjustedMovement.x)
        val zCollision = !MathHelper.approximatelyEquals(movement.z, adjustedMovement.z)

        this.horizontalCollision = xCollision || zCollision
        this.verticalCollision = movement.y != adjustedMovement.y

        onGround = verticalCollision && movement.y < 0.0

        if (!isTouchingWater()) {
            checkWaterState()
        }

        if (onGround) {
            onLanding()
        } else if (movement.y < 0) {
            fallDistance -= movement.y.toFloat()
        }

        val vec3d2: Vec3d = this.velocity
        if (horizontalCollision || verticalCollision) {
            this.velocity = Vec3d(
                if (xCollision) 0.0 else vec3d2.x,
                if (onGround) 0.0 else vec3d2.y,
                if (zCollision) 0.0 else vec3d2.z
            )
        }
    }

    private fun adjustMovementForCollisions(movement: Vec3d): Vec3d {
        val bl4: Boolean
        val box: Box = Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).offset(this.pos)

        val entityCollisionList = emptyList<VoxelShape>()

        val vec3d = if (movement.lengthSquared() == 0.0) {
            movement
        } else {
            Entity.adjustMovementForCollisions(
                this.player,
                movement,
                box,
                this.player.world,
                entityCollisionList
            )
        }
        val bl = movement.x != vec3d.x
        val bl2 = movement.y != vec3d.y
        val bl3 = movement.z != vec3d.z

        bl4 = onGround || bl2 && movement.y < 0.0

        if (this.player.stepHeight > 0.0f && bl4 && (bl || bl3)) {
            var vec3d2 = Entity.adjustMovementForCollisions(
                this.player,
                Vec3d(movement.x, this.player.stepHeight.toDouble(), movement.z),
                box,
                this.player.world,
                entityCollisionList
            )
            val vec3d3 = Entity.adjustMovementForCollisions(
                this.player,
                Vec3d(0.0, this.player.stepHeight.toDouble(), 0.0),
                box.stretch(movement.x, 0.0, movement.z),
                this.player.world,
                entityCollisionList
            )
            val asdf = Entity.adjustMovementForCollisions(
                this.player,
                Vec3d(movement.x, 0.0, movement.z),
                box.offset(vec3d3),
                this.player.world,
                entityCollisionList
            ).add(vec3d3)

            if (vec3d3.y < this.player.stepHeight.toDouble() && asdf.horizontalLengthSquared() > vec3d2.horizontalLengthSquared()) {
                vec3d2 = asdf
            }

            if (vec3d2.horizontalLengthSquared() > vec3d.horizontalLengthSquared()) {
                return vec3d2.add(
                    Entity.adjustMovementForCollisions(
                        this.player,
                        Vec3d(0.0, -vec3d2.y + movement.y, 0.0),
                        box.offset(vec3d2),
                        this.player.world,
                        entityCollisionList
                    )
                )
            }
        }
        return vec3d
    }

    private fun onLanding() {
        this.fallDistance = 0.0f
    }

    fun jump() {
        this.velocity += Vec3d(
            0.0,
            this.getJumpVelocity().toDouble() - this.velocity.y,
            0.0
        )

        if (this.isSprinting()) {
            val f: Float = this.yaw.toRadians()

            this.velocity += Vec3d((-MathHelper.sin(f) * 0.2f).toDouble(), 0.0, (MathHelper.cos(f) * 0.2f).toDouble())
        }

    }

    private fun applyClimbingSpeed(motion: Vec3d): Vec3d {
        if (!isClimbing()) {
            return motion
        }

        onLanding()
        val d = MathHelper.clamp(motion.x, -0.15000000596046448, 0.15000000596046448)
        val e = MathHelper.clamp(motion.z, -0.15000000596046448, 0.15000000596046448)
        var g = max(motion.y, -0.15000000596046448)
        if (g < 0.0 && !pos.toBlockPos().getState()!!.isOf(Blocks.SCAFFOLDING) && player.isHoldingOntoLadder) {
            g = 0.0
        }

        return Vec3d(d, g, e)
    }

    private fun isClimbing(): Boolean {
        val blockPos = pos.toBlockPos()
        val blockState = blockPos.getState()!!
        return if (blockState.isIn(BlockTags.CLIMBABLE)) {
            true
        } else if (blockState.block is TrapdoorBlock && this.canEnterTrapdoor(blockPos, blockState)) {
            true
        } else {
            false
        }
    }

    private fun canEnterTrapdoor(pos: BlockPos, state: BlockState): Boolean {
        if (!(state.get(TrapdoorBlock.OPEN) as Boolean)) {
            return false
        }
        val blockState = this.player.world.getBlockState(pos.down())
        return blockState.isOf(Blocks.LADDER) && blockState.get(LadderBlock.FACING) == state.get(TrapdoorBlock.FACING)
    }

    private fun adjustMovementForSneaking(movement: Vec3d): Vec3d {
        var movement = movement
        val isSelfMovement = true // (type == MovementType.SELF || type == MovementType.PLAYER)
        val isFlying = false // abilities.isFlying

        if (!isFlying && movement.y <= 0.0 && isSelfMovement && this.method_30263()) {
            var d = movement.x
            var e = movement.z
            val f = 0.05
            while (d != 0.0 && world.isSpaceEmpty(
                    player,
                    boundingBox.offset(d, -STEP_HEIGHT, 0.0)
                )
            ) {
                if (d < 0.05 && d >= -0.05) {
                    d = 0.0
                    continue
                }
                if (d > 0.0) {
                    d -= 0.05
                    continue
                }
                d += 0.05
            }
            while (e != 0.0 && world.isSpaceEmpty(
                    player,
                    boundingBox.offset(0.0, -STEP_HEIGHT, e)
                )
            ) {
                if (e < 0.05 && e >= -0.05) {
                    e = 0.0
                    continue
                }
                if (e > 0.0) {
                    e -= 0.05
                    continue
                }
                e += 0.05
            }
            while (d != 0.0 && e != 0.0 && world.isSpaceEmpty(
                    player,
                    boundingBox.offset(d, -STEP_HEIGHT, e)
                )
            ) {
                d =
                    if (d < 0.05 && d >= -0.05) 0.0 else (if (d > 0.0) (0.05.let { d -= it; d }) else (0.05.let { d += it; d }))
                if (e < 0.05 && e >= -0.05) {
                    e = 0.0
                    continue
                }
                if (e > 0.0) {
                    e -= 0.05
                    continue
                }
                e += 0.05
            }

            if (movement.x != d || movement.z != e) {
                clipLedged = true
            }
            
            if (this.shouldClipAtLedge()) {
                movement = Vec3d(d, movement.y, e)
            }
        }
        return movement
    }

    protected fun shouldClipAtLedge(): Boolean {
        return this.input.sneaking || this.input.forceSafeWalk
    }

    private fun method_30263(): Boolean {
        return onGround || this.fallDistance < STEP_HEIGHT && !world.isSpaceEmpty(
            player,
            boundingBox.offset(0.0, this.fallDistance - STEP_HEIGHT, 0.0)
        )
    }

    private fun isSprinting(): Boolean = this.sprinting

    private fun getJumpVelocity(): Float =
        0.42f * this.getJumpVelocityMultiplier() +
            this.getJumpBoostVelocityModifier()

    private fun getJumpBoostVelocityModifier() =
        if (hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            0.1f * (getStatusEffect(StatusEffects.JUMP_BOOST)!!.amplifier.toFloat() + 1f)
        } else {
            0f
        }

    private fun getJumpVelocityMultiplier(): Float {
        val f = pos.toBlockPos().getBlock()?.jumpVelocityMultiplier ?: 0f
        val g = getVelocityAffectingPos().getBlock()?.jumpVelocityMultiplier ?: 0f

        return if (f.toDouble() == 1.0) g else f
    }

    private fun doesNotCollide(offsetX: Double, offsetY: Double, offsetZ: Double): Boolean {
        return this.doesNotCollide(this.boundingBox.offset(offsetX, offsetY, offsetZ))
    }

    private fun doesNotCollide(box: Box): Boolean {
        return this.player.world.isSpaceEmpty(this.player, box) && !this.player.world.containsFluid(box)
    }

    private fun swimUpward(water: TagKey<Fluid>?) {
        velocity += Vec3d(0.0, 0.03999999910593033, 0.0)
    }

    private fun getVelocityAffectingPos() =
        BlockPos.ofFloored(this.pos.x, this.boundingBox.minY - 0.5000001, this.pos.z)

    private fun getSwimHeight(): Double {
        return if (player.standingEyeHeight.toDouble() < 0.4) 0.0 else 0.4
    }

    private fun isTouchingWater(): Boolean = touchingWater
    private fun isInLava(): Boolean {
        return this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0
    }

    private fun checkWaterState() {
        val var2 = player.vehicle
        if (var2 is BoatEntity) {
            if (!var2.isSubmergedInWater()) {
                this.touchingWater = false
                return
            }
        }
        if (updateMovementInFluid(FluidTags.WATER, 0.014)) {
            onLanding()
            this.touchingWater = true
        } else {
            this.touchingWater = false
        }
    }

    private fun updateSwimming() {
        isSwimming = if (this.isSwimming) {
            isSprinting() && isTouchingWater() && !this.player.hasVehicle()
        } else {
            isSprinting() && this.isSubmergedInWater() &&
                !this.player.hasVehicle() &&
                this.player.world
                    .getFluidState(this.pos.toBlockPos())
                    .isIn(FluidTags.WATER)
        }
    }

    private fun updateSubmergedInWaterState() {
        submergedInWater = this.submergedFluidTag.contains(FluidTags.WATER)
        submergedFluidTag.clear()
        val d: Double = this.getEyeY() - 0.1111111119389534
        val entity = this.player.vehicle
        if (entity is BoatEntity) {
            if (!entity.isSubmergedInWater() && entity.getBoundingBox().maxY >= d && entity.getBoundingBox().minY <= d) {
                return
            }
        }
        val blockPos = BlockPos.ofFloored(this.pos.x, d, this.pos.z)
        val fluidState: FluidState = this.player.world.getFluidState(blockPos)
        val e = (blockPos.y.toFloat() + fluidState.getHeight(this.player.world, blockPos)).toDouble()
        if (e > d) {
            fluidState.streamTags().forEach {
                submergedFluidTag.add(it)
            }
        }
    }

    private fun getEyeY(): Double {
        return this.pos.y + this.player.standingEyeHeight.toDouble()
    }

    private fun isSubmergedInWater(): Boolean {
        return this.submergedInWater && isTouchingWater()
    }

    private fun getFluidHeight(tags: TagKey<Fluid>): Double = this.fluidHeight.getDouble(tags)

    private fun updateMovementInFluid(tag: TagKey<Fluid>, speed: Double): Boolean {
        if (this.isRegionUnloaded()) {
            return false
        }
        val box = this.boundingBox.contract(0.001)
        val i = MathHelper.floor(box.minX)
        val j = MathHelper.ceil(box.maxX)
        val k = MathHelper.floor(box.minY)
        val l = MathHelper.ceil(box.maxY)
        val m = MathHelper.floor(box.minZ)
        val n = MathHelper.ceil(box.maxZ)
        var d = 0.0
        val bl = true // this.isPushedByFluids()
        var bl2 = false
        var vec3d = Vec3d.ZERO
        var o = 0
        val mutable = BlockPos.Mutable()

        for (p in i until j) {
            for (q in k until l) {
                for (r in m until n) {
                    mutable[p, q] = r
                    val fluidState: FluidState = this.player.world.getFluidState(mutable)
                    if (fluidState.isIn(tag)) {
                        val e = (q.toFloat() + fluidState.getHeight(this.player.world, mutable)).toDouble()
                        if (e >= box.minY) {
                            bl2 = true
                            d = max(e - box.minY, d)
                            if (bl) {
                                var vec3d2 = fluidState.getVelocity(this.player.world, mutable)
                                if (d < 0.4) {
                                    vec3d2 = vec3d2.multiply(d)
                                }
                                vec3d = vec3d.add(vec3d2)
                                ++o
                            }
                        }
                    }
                }
            }
        }

        if (vec3d.length() > 0.0) {
            if (o > 0) {
                vec3d = vec3d.multiply(1.0 / o.toDouble())
            }
//            if (this !is PlayerEntity) {
//                vec3d = vec3d.normalize()
//            }
            val vec3d3: Vec3d = velocity
            vec3d = vec3d.multiply(speed * 1.0)
            val f = 0.003
            if (abs(vec3d3.x) < 0.003 && abs(vec3d3.z) < 0.003 && vec3d.length() < 0.0045000000000000005) {
                vec3d = vec3d.normalize().multiply(0.0045000000000000005)
            }
            velocity += vec3d
        }

        this.fluidHeight.put(tag, d)
        return bl2
    }

    private fun isRegionUnloaded(): Boolean {
        val box = this.boundingBox.expand(1.0)
        val i = MathHelper.floor(box.minX)
        val j = MathHelper.ceil(box.maxX)
        val k = MathHelper.floor(box.minZ)
        val l = MathHelper.ceil(box.maxZ)
        return !this.player.world.isRegionLoaded(i, k, j, l)
    }

    private fun getRotationVector() = getRotationVector(this.pitch, this.yaw)

    private fun getRotationVector(pitch: Float, yaw: Float): Vec3d {
        val f = pitch * (Math.PI.toFloat() / 180)
        val g = -yaw * (Math.PI.toFloat() / 180)

        val h = MathHelper.cos(g)
        val i = MathHelper.sin(g)
        val j = MathHelper.cos(f)
        val k = MathHelper.sin(f)

        return Vec3d((i * j).toDouble(), (-k).toDouble(), (h * j).toDouble())
    }

    private fun hasStatusEffect(effect: RegistryEntry<StatusEffect>): Boolean {
        val instance = player.getStatusEffect(effect) ?: return false

        return instance.duration >= this.simulatedTicks
    }

    private fun getStatusEffect(effect: RegistryEntry<StatusEffect>): StatusEffectInstance? {
        val instance = player.getStatusEffect(effect) ?: return null

        if (instance.duration < this.simulatedTicks) {
            return null
        }

        return instance
    }

    fun clone(): SimulatedPlayer {
        return SimulatedPlayer(
            player,
            input,
            pos,
            velocity,
            boundingBox,
            yaw,
            pitch,
            sprinting,
            fallDistance,
            jumpingCooldown,
            isJumping,
            isFallFlying,
            onGround,
            horizontalCollision,
            verticalCollision,
            touchingWater,
            isSwimming,
            submergedInWater,
            Object2DoubleArrayMap(fluidHeight),
            HashSet(submergedFluidTag)
        )
    }

    class SimulatedPlayerInput(
        directionalInput: DirectionalInput,
        jumping: Boolean,
        var sprinting: Boolean,
        sneaking: Boolean
    ) : Input() {
        var forceSafeWalk: Boolean = false

        init {
            this.pressingForward = directionalInput.forwards
            this.pressingBack = directionalInput.backwards
            this.pressingLeft = directionalInput.left
            this.pressingRight = directionalInput.right
            this.jumping = jumping
            this.sneaking = sneaking
        }

        fun update() {
            if (this.pressingForward != this.pressingBack) {
                this.movementForward = if (this.pressingForward) 1.0f else -1.0f
            } else {
                this.movementForward = 0.0f
            }

            movementSideways = if (pressingLeft == pressingRight) 0.0f else if (pressingLeft) 1.0f else -1.0f

            if (sneaking) {
                movementSideways = (movementSideways.toDouble() * 0.3).toFloat()
                movementForward = (movementForward.toDouble() * 0.3).toFloat()
            }
        }

        override fun toString(): String {
            return "SimulatedPlayerInput(forwards={${this.pressingForward}}, backwards={${this.pressingBack}}, left={${this.pressingLeft}}, right={${this.pressingRight}}, jumping={${this.jumping}}, sprinting=$sprinting, slowDown=$sneaking)"
        }

        companion object {
            private const val MAX_WALKING_SPEED = 0.121

            fun fromClientPlayer(directionalInput: DirectionalInput): SimulatedPlayerInput {
                val input = SimulatedPlayerInput(
                    directionalInput,
                    player.input.jumping,
                    player.isSprinting,
                    player.isSneaking
                )

                val safeWalkEvent = PlayerSafeWalkEvent()

                EventManager.callEvent(safeWalkEvent)

                if (safeWalkEvent.isSafeWalk) {
                    input.forceSafeWalk = true
                }

                return input
            }

            /**
             * Guesses the current input of a server player based on player position and velocity
             */
            fun guessInput(entity: PlayerEntity): SimulatedPlayerInput {
                val velocity = entity.pos.subtract(entity.prevPos)

                val horizontalVelocity = velocity.horizontalLengthSquared()

                val sprinting = horizontalVelocity >= MAX_WALKING_SPEED * MAX_WALKING_SPEED

                val input = if (horizontalVelocity > 0.05 * 0.05) {
                    val velocityAngle = getDegreesRelativeToView(velocity, yaw = entity.yaw)

                    val velocityAngle1 = MathHelper.wrapDegrees(velocityAngle)

                    getDirectionalInputForDegrees(DirectionalInput.NONE, velocityAngle1)
                } else {
                    DirectionalInput.NONE
                }

                val jumping = !entity.isOnGround

                return SimulatedPlayerInput(
                    input,
                    jumping,
                    sprinting,
                    sneaking=entity.isSneaking
                )
            }
        }

    }
}
