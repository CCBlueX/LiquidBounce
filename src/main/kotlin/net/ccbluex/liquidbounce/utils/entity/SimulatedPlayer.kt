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
@file:Suppress("All")

package net.ccbluex.liquidbounce.utils.entity

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap
import it.unimi.dsi.fastutil.objects.Object2DoubleMap
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.fastCos
import net.ccbluex.liquidbounce.utils.client.fastSin
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.math.withLength
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDegreesRelativeToView
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees
import net.minecraft.client.player.ClientInput
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.tags.BlockTags
import net.minecraft.tags.FluidTags
import net.minecraft.tags.TagKey
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.boat.Boat
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LadderBlock
import net.minecraft.world.level.block.PowderSnowBlock
import net.minecraft.world.level.block.TrapDoorBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

private const val STEP_HEIGHT = 0.5

class SimulatedPlayer(
    private val player: Player,
    var input: SimulatedPlayerInput,
    override var pos: Vec3,
    var deltaMovement: Vec3,
    var boundingBox: AABB,
    var yRot: Float,
    var xRot: Float,
    var isSprinting: Boolean,

    var fallDistance: Double,
    private var jumpingCooldown: Int,
    private var isJumping: Boolean,
    private var isFallFlying: Boolean,
    var onGround: Boolean,
    var horizontalCollision: Boolean,
    private var verticalCollision: Boolean,

    private var wasTouchingWater: Boolean,
    private var isSwimming: Boolean,
    private var wasUnderwater: Boolean,
    private var fluidHeight: Object2DoubleMap<TagKey<Fluid>>,
    private var fluidOnEyes: HashSet<TagKey<Fluid>>
) : PlayerSimulation {
    private val world: Level
        get() = player.level()

    companion object {
        @JvmStatic
        fun fromClientPlayer(input: SimulatedPlayerInput): SimulatedPlayer {
            return SimulatedPlayer(
                player,
                input,
                player.position(),
                player.deltaMovement,
                player.boundingBox,
                player.yRot,
                player.xRot,

                player.isSprinting,

                player.fallDistance,
                player.noJumpDelay,
                player.jumping,
                player.isFallFlying,
                player.onGround(),
                player.horizontalCollision,
                player.verticalCollision,

                player.isInWater,
                player.isSwimming,
                player.isUnderWater,
                Object2DoubleArrayMap(player.fluidHeight),
                HashSet(player.fluidOnEyes)
            )
        }

        fun fromOtherPlayer(player: Player, input: SimulatedPlayerInput): SimulatedPlayer {
            return SimulatedPlayer(
                player,
                input,
                player.position(),
                deltaMovement = player.position().subtract(player.lastPos),
                player.boundingBox,
                player.yRot,
                player.xRot,

                player.isSprinting,

                player.fallDistance,
                player.noJumpDelay,
                player.jumping,
                player.isFallFlying,
                player.onGround(),
                player.horizontalCollision,
                player.verticalCollision,

                player.isInWater,
                player.isSwimming,
                player.isUnderWater,
                Object2DoubleArrayMap(player.fluidHeight),
                HashSet(player.fluidOnEyes)
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

        this.isJumping = this.input.keyPresses.jump

        val d: Vec3 = this.deltaMovement

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

        this.deltaMovement = Vec3(h, i, j)

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

        if (this.hasStatusEffect(MobEffects.SLOW_FALLING) || this.hasStatusEffect(MobEffects.LEVITATION)) {
            this.onLanding()
        }

        this.travel(Vec3(sidewaysSpeed, upwardsSpeed, forwardSpeed))
    }

    private fun travel(movementInput: Vec3) {
        // PlayerEntity
        if (this.isSwimming && !this.player.isPassenger) {
            val g = this.getRotationVector().y
            val h = if (g < -0.2) 0.085 else 0.06
            if (g <= 0.0 || this.input.keyPresses.jump || !this.player.level()
                .getBlockState(BlockPos.containing(this.pos.x, this.pos.y + 1.0 - 0.1, this.pos.z))
                .fluidState.isEmpty
            ) {
                deltaMovement = deltaMovement.add(0.0, (g - deltaMovement.y) * h, 0.0)
            }
        }

//        if (this.abilities.flying && !this.hasVehicle()) {
        val beforeTravelVelocityY = this.deltaMovement.y
//            super.travel(movementInput)
//            val vec3d2: Vec3d = this.getVelocity()
//            this.setVelocity(vec3d2.x, g * 0.6, vec3d2.z)
//            onLanding()
//            this.setFlag(7, false)
//        }

        var d = 0.08
        val bl: Boolean = deltaMovement.y <= 0.0
        if (deltaMovement.y <= 0.0 && hasStatusEffect(MobEffects.SLOW_FALLING)) {
            d = 0.01
            this.onLanding()
        }

//        val fluidState: FluidState = this.player.entityWorld.getFluidState(pos.toBlockPos())

        if (isTouchingWater() && this.player.isAffectedByFluids /*&& !this.player.canWalkOnFluid(fluidState.fluid)*/) {
            val e: Double = this.pos.y
            var f = if (isSprinting) 0.9f else 0.8f // this.player.getBaseMovementSpeedMultiplier()
            var g = 0.02f
            var h = this.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY).toFloat()

            if (!onGround) {
                h *= 0.5f
            }
            if (h > 0.0f) {
                f += (0.54600006f - f) * h / 3.0f
                g += (this.getMovementSpeed() - g) * h / 3.0f
            }
            if (hasStatusEffect(MobEffects.DOLPHINS_GRACE)) {
                f = 0.96f
            }
            this.updateVelocity(g, movementInput)
            this.move(deltaMovement)
            var vec3d: Vec3 = deltaMovement
            if (this.horizontalCollision && this.isClimbing()) {
                vec3d = Vec3(vec3d.x, 0.2, vec3d.z)
            }
            deltaMovement = vec3d.multiply(f.toDouble(), 0.8, f.toDouble())
            val vec3d2: Vec3 = this.player.getFluidFallingAdjustedMovement(d, bl, deltaMovement)
            deltaMovement = vec3d2
            if (this.horizontalCollision && this.doesNotCollide(vec3d2.x, vec3d2.y + 0.6 - this.pos.y + e, vec3d2.z)) {
                this.deltaMovement = Vec3(vec3d2.x, 0.3, vec3d2.z)
            }
        } else if (isInLava() && this.player.isAffectedByFluids /*&& !this.canWalkOnFluid(fluidState.fluid)*/) {
            val e: Double = this.pos.y
            this.updateVelocity(0.02f, movementInput)
            this.move(deltaMovement)
            if (getFluidHeight(FluidTags.LAVA) <= getSwimHeight()) {
                deltaMovement = deltaMovement.multiply(0.5, 0.8, 0.5)
                deltaMovement = this.player.getFluidFallingAdjustedMovement(d, bl, deltaMovement)
            } else {
                deltaMovement = deltaMovement.scale(0.5)
            }
            if (!this.player.isNoGravity) {
                deltaMovement = this.deltaMovement.add(0.0, -d / 4.0, 0.0)
            }
            if (this.horizontalCollision && this.doesNotCollide(
                    deltaMovement.x,
                    deltaMovement.y + 0.6 - this.pos.y + e,
                    deltaMovement.z
                )
            ) {
                deltaMovement = Vec3(deltaMovement.x, 0.3, deltaMovement.z)
            }
        } else if (this.isFallFlying) {
            var k: Double
            var e: Vec3 = this.deltaMovement
            if (e.y > -0.5) {
                fallDistance = 1.0
            }
            val vec3d3 = this.getRotationVector()
            val f: Float = this.xRot * (Math.PI.toFloat() / 180)
            val g = sqrt(vec3d3.x * vec3d3.x + vec3d3.z * vec3d3.z)
            val vec3d = e.horizontalDistance()
            val i = vec3d3.length()
            var j = f.fastCos()
            j = (j.toDouble() * (j.toDouble() * 1.0.coerceAtMost(i / 0.4))).toFloat()
            e = this.deltaMovement.add(0.0, d * (-1.0 + j.toDouble() * 0.75), 0.0)
            if (e.y < 0.0 && g > 0.0) {
                k = e.y * -0.1 * j.toDouble()
                e = e.add(vec3d3.x * k / g, k, vec3d3.z * k / g)
            }
            if (f < 0.0f && g > 0.0) {
                k = vec3d * (-f.fastSin()).toDouble() * 0.04
                e = e.add(-vec3d3.x * k / g, k * 3.2, -vec3d3.z * k / g)
            }
            if (g > 0.0) {
                e = e.add((vec3d3.x / g * vec3d - e.x) * 0.1, 0.0, (vec3d3.z / g * vec3d - e.z) * 0.1)
            }
            this.deltaMovement = e.multiply(0.99, 0.98, 0.99)

            move(this.deltaMovement)
        } else {
            val blockPos = this.getVelocityAffectingPos()
            val p: Float = this.player.level().getBlockState(blockPos).block.friction
            val f = if (onGround) p * 0.91f else 0.91f
            val vec3d6 = this.applyMovementInput(movementInput, p)
            var q = vec3d6.y
            if (hasStatusEffect(MobEffects.LEVITATION)) {
                q += (0.05 * (getStatusEffect(MobEffects.LEVITATION)!!.amplifier + 1).toDouble() - vec3d6.y) * 0.2
            } else if (this.player.level().isClientSide && !this.player.level().hasChunkAt(blockPos)) {
                q = if (this.pos.y > this.player.level().minY.toDouble()) {
                    -0.1
                } else {
                    0.0
                }
            } else if (!this.player.isNoGravity) {
                q -= d
            }

            deltaMovement = if (this.player.shouldDiscardFriction()) {
                Vec3(vec3d6.x, q, vec3d6.z)
            } else {
                Vec3(vec3d6.x * f.toDouble(), q * 0.9800000190734863, vec3d6.z * f.toDouble())
            }
        }

        // PlayerEntity
        if (player.abilities.flying && !this.player.isPassenger) {
            deltaMovement = Vec3(deltaMovement.x, beforeTravelVelocityY * 0.6, deltaMovement.z)
            this.onLanding()
        }
    }

    private fun applyMovementInput(movementInput: Vec3, slipperiness: Float): Vec3 {
        this.updateVelocity(this.getMovementSpeed(slipperiness), movementInput)
        this.deltaMovement = applyClimbingSpeed(this.deltaMovement)
        this.deltaMovement = applyWebSpeed(this.deltaMovement)
        this.move(this.deltaMovement)


        var vec3d = this.deltaMovement
        if ((horizontalCollision || this.isJumping) && (
            this.isClimbing() || pos.toBlockPos().getState()
                ?.`is`(Blocks.POWDER_SNOW) == true && PowderSnowBlock.canEntityWalkOnPowderSnow(player)
            )
        ) {
            vec3d = Vec3(vec3d.x, 0.2, vec3d.z)
        }

        return vec3d
    }

    private fun updateVelocity(speed: Float, movementInput: Vec3) {
        val vec3d = Entity.getInputVector(movementInput, speed, this.yRot)

        this.deltaMovement += vec3d
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

    private fun move(input: Vec3) {
        val event = callEvent(PlayerMoveEvent(MoverType.SELF, input))
        val vec3d = event.movement

        val movement = this.adjustMovementForSneaking(vec3d)
        val adjustedMovement = this.adjustMovementForCollisions(movement)

        if (adjustedMovement.lengthSqr() > 1.0E-7) {
            this.pos += adjustedMovement
            this.boundingBox = player.dimensions.makeBoundingBox(this.pos)
        }

        val xCollision = !Mth.equal(movement.x, adjustedMovement.x)
        val zCollision = !Mth.equal(movement.z, adjustedMovement.z)

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

        val vec3d2: Vec3 = this.deltaMovement
        if (horizontalCollision || verticalCollision) {
            this.deltaMovement = Vec3(
                if (xCollision) 0.0 else vec3d2.x,
                if (onGround) 0.0 else vec3d2.y,
                if (zCollision) 0.0 else vec3d2.z
            )
        }
    }

    private fun adjustMovementForCollisions(movement: Vec3): Vec3 {
        val bl4: Boolean
        val box: AABB = AABB(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).move(this.pos)

        val entityCollisionList = emptyList<VoxelShape>()

        val vec3d = if (movement.lengthSqr() == 0.0) {
            movement
        } else {
            Entity.collideBoundingBox(
                this.player,
                movement,
                box,
                this.player.level(),
                entityCollisionList
            )
        }
        val bl = movement.x != vec3d.x
        val bl2 = movement.y != vec3d.y
        val bl3 = movement.z != vec3d.z

        bl4 = onGround || bl2 && movement.y < 0.0

        if (this.player.maxUpStep() > 0.0f && bl4 && (bl || bl3)) {
            var vec3d2 = Entity.collideBoundingBox(
                this.player,
                Vec3(movement.x, this.player.maxUpStep().toDouble(), movement.z),
                box,
                this.player.level(),
                entityCollisionList
            )
            val vec3d3 = Entity.collideBoundingBox(
                this.player,
                Vec3(0.0, this.player.maxUpStep().toDouble(), 0.0),
                box.expandTowards(movement.x, 0.0, movement.z),
                this.player.level(),
                entityCollisionList
            )
            val asdf = Entity.collideBoundingBox(
                this.player,
                Vec3(movement.x, 0.0, movement.z),
                box.move(vec3d3),
                this.player.level(),
                entityCollisionList
            ).add(vec3d3)

            if (vec3d3.y < this.player.maxUpStep()
                    .toDouble() && asdf.horizontalDistanceSqr() > vec3d2.horizontalDistanceSqr()) {
                vec3d2 = asdf
            }

            if (vec3d2.horizontalDistanceSqr() > vec3d.horizontalDistanceSqr()) {
                return vec3d2.add(
                    Entity.collideBoundingBox(
                        this.player,
                        Vec3(0.0, -vec3d2.y + movement.y, 0.0),
                        box.move(vec3d2),
                        this.player.level(),
                        entityCollisionList
                    )
                )
            }
        }
        return vec3d
    }

    private fun onLanding() {
        this.fallDistance = 0.0
    }

    fun jump() {
        this.deltaMovement += Vec3(
            0.0,
            this.getJumpVelocity().toDouble() - this.deltaMovement.y,
            0.0
        )

        if (isSprinting) {
            val f: Float = this.yRot.toRadians()

            this.deltaMovement += Vec3((-f.fastSin() * 0.2f).toDouble(), 0.0, (f.fastCos() * 0.2f).toDouble())
        }

    }

    private fun applyClimbingSpeed(motion: Vec3): Vec3 {
        if (!isClimbing()) {
            return motion
        }

        onLanding()
        val d = Mth.clamp(motion.x, -0.15000000596046448, 0.15000000596046448)
        val e = Mth.clamp(motion.z, -0.15000000596046448, 0.15000000596046448)
        var g = max(motion.y, -0.15000000596046448)
        if (g < 0.0 && !pos.toBlockPos().getState()!!.`is`(Blocks.SCAFFOLDING) && player.isSuppressingSlidingDownLadder) {
            g = 0.0
        }

        return Vec3(d, g, e)
    }
    private fun applyWebSpeed(motion: Vec3): Vec3 {
        val blockState = world.getBlockState(pos.toBlockPos())
        if (blockState.block != Blocks.COBWEB) {
            return motion
        }
        val multiplier = if (hasStatusEffect(MobEffects.WEAVING)) {
            Vec3(0.5, 0.25, 0.5)
        } else {
            Vec3(0.25, 0.05, 0.25)
        }
        return motion.multiply(multiplier.x, multiplier.y, multiplier.z)
    }

    private fun isClimbing(): Boolean {
        val blockPos = pos.toBlockPos()
        val blockState = blockPos.getState()!!
        return if (blockState.`is`(BlockTags.CLIMBABLE)) {
            true
        } else if (blockState.block is TrapDoorBlock && this.canEnterTrapdoor(blockPos, blockState)) {
            true
        } else {
            false
        }
    }

    private fun canEnterTrapdoor(pos: BlockPos, state: BlockState): Boolean {
        if (!(state.getValue(TrapDoorBlock.OPEN) as Boolean)) {
            return false
        }
        val blockState = this.player.level().getBlockState(pos.below())
        return blockState.`is`(Blocks.LADDER) && blockState.getValue(LadderBlock.FACING) == state.getValue(TrapDoorBlock.FACING)
    }

    private fun adjustMovementForSneaking(movement: Vec3): Vec3 {
        var movement = movement
        val isSelfMovement = true // (type == MovementType.SELF || type == MovementType.PLAYER)
        val isFlying = false // abilities.isFlying

        if (!isFlying && movement.y <= 0.0 && isSelfMovement && this.isAboveGround()) {
            var d = movement.x
            var e = movement.z
            val f = 0.05
            while (d != 0.0 && world.noCollision(
                    player,
                    boundingBox.move(d, -STEP_HEIGHT, 0.0)
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
            while (e != 0.0 && world.noCollision(
                    player,
                    boundingBox.move(0.0, -STEP_HEIGHT, e)
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
            while (d != 0.0 && e != 0.0 && world.noCollision(
                    player,
                    boundingBox.move(d, -STEP_HEIGHT, e)
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
                movement = Vec3(d, movement.y, e)
            }
        }
        return movement
    }

    private fun shouldClipAtLedge(): Boolean {
        return !this.input.ignoreClippingAtLedge && (this.input.keyPresses.shift || this.input.forceSafeWalk)
    }

    private fun isAboveGround(): Boolean {
        return onGround || this.fallDistance < STEP_HEIGHT && !world.noCollision(
            player,
            boundingBox.move(0.0, this.fallDistance - STEP_HEIGHT, 0.0)
        )
    }

    private fun getJumpVelocity(): Float =
        0.42f * this.getJumpVelocityMultiplier() +
            this.getJumpBoostVelocityModifier()

    private fun getJumpBoostVelocityModifier() =
        if (hasStatusEffect(MobEffects.JUMP_BOOST)) {
            0.1f * (getStatusEffect(MobEffects.JUMP_BOOST)!!.amplifier.toFloat() + 1f)
        } else {
            0f
        }

    private fun getJumpVelocityMultiplier(): Float {
        val f = pos.toBlockPos().getBlock()?.jumpFactor ?: 0f
        val g = getVelocityAffectingPos().getBlock()?.jumpFactor ?: 0f

        return if (f.toDouble() == 1.0) g else f
    }

    private fun doesNotCollide(offsetX: Double, offsetY: Double, offsetZ: Double): Boolean {
        return this.doesNotCollide(this.boundingBox.move(offsetX, offsetY, offsetZ))
    }

    private fun doesNotCollide(box: AABB): Boolean {
        return this.player.level().noCollision(this.player, box) && !this.player.level().containsAnyLiquid(box)
    }

    private fun swimUpward(fluid: TagKey<Fluid>) {
        deltaMovement += Vec3(
            0.0,
            if (fluid === FluidTags.WATER) 0.03999999910593033 else 0.005999999865889549,
            0.0
        )
    }

    private fun getVelocityAffectingPos() =
        BlockPos.containing(this.pos.x, this.boundingBox.minY - 0.5000001, this.pos.z)

    private fun getSwimHeight(): Double {
        return if (player.eyeHeight.toDouble() < 0.4) 0.0 else 0.4
    }

    private fun isTouchingWater(): Boolean = wasTouchingWater
    private fun isInLava(): Boolean {
        return this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0
    }

    private fun checkWaterState() {
        val var2 = player.vehicle
        if (var2 is Boat) {
            if (!var2.isUnderWater) {
                this.wasTouchingWater = false
                return
            }
        }
        if (updateMovementInFluid(FluidTags.WATER, 0.014)) {
            onLanding()
            this.wasTouchingWater = true
        } else {
            this.wasTouchingWater = false
        }
    }

    private fun updateSwimming() {
        isSwimming = if (this.isSwimming) {
            isSprinting && isTouchingWater() && !this.player.isPassenger
        } else {
            isSprinting && this.isSubmergedInWater() &&
                !this.player.isPassenger &&
                this.player.level()
                    .getFluidState(this.pos.toBlockPos())
                    .`is`(FluidTags.WATER)
        }
    }

    private fun updateSubmergedInWaterState() {
        wasUnderwater = this.fluidOnEyes.contains(FluidTags.WATER)
        fluidOnEyes.clear()
        val d: Double = this.getEyeY() - 0.1111111119389534
        val entity = this.player.vehicle
        if (entity is Boat) {
            if (!entity.isUnderWater && entity.boundingBox.maxY >= d && entity.boundingBox.minY <= d) {
                return
            }
        }
        val blockPos = BlockPos.containing(this.pos.x, d, this.pos.z)
        val fluidState: FluidState = this.player.level().getFluidState(blockPos)
        val e = (blockPos.y.toFloat() + fluidState.getHeight(this.player.level(), blockPos)).toDouble()
        if (e > d) {
            fluidState.tags.forEach {
                fluidOnEyes.add(it)
            }
        }
    }

    private fun getEyeY(): Double {
        return this.pos.y + this.player.eyeHeight.toDouble()
    }

    private fun isSubmergedInWater(): Boolean {
        return this.wasUnderwater && isTouchingWater()
    }

    private fun getFluidHeight(tags: TagKey<Fluid>): Double = this.fluidHeight.getDouble(tags)

    private fun updateMovementInFluid(tag: TagKey<Fluid>, speed: Double): Boolean {
        if (this.isRegionUnloaded()) {
            return false
        }
        val box = this.boundingBox.deflate(0.001)
        val i = Mth.floor(box.minX)
        val j = Mth.ceil(box.maxX)
        val k = Mth.floor(box.minY)
        val l = Mth.ceil(box.maxY)
        val m = Mth.floor(box.minZ)
        val n = Mth.ceil(box.maxZ)
        var d = 0.0
        val bl = true // this.isPushedByFluids()
        var bl2 = false
        var vec3d = Vec3.ZERO
        var o = 0
        val mutable = BlockPos.MutableBlockPos()

        for (p in i until j) {
            for (q in k until l) {
                for (r in m until n) {
                    mutable[p, q] = r
                    val fluidState: FluidState = this.player.level().getFluidState(mutable)
                    if (fluidState.`is`(tag)) {
                        val e = (q.toFloat() + fluidState.getHeight(this.player.level(), mutable)).toDouble()
                        if (e >= box.minY) {
                            bl2 = true
                            d = max(e - box.minY, d)
                            if (bl) {
                                var vec3d2 = fluidState.getFlow(this.player.level(), mutable)
                                if (d < 0.4) {
                                    vec3d2 = vec3d2.scale(d)
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
                vec3d = vec3d.scale(1.0 / o.toDouble())
            }
//            if (this !is PlayerEntity) {
//                vec3d = vec3d.normalize()
//            }
            val vec3d3: Vec3 = deltaMovement
            vec3d = vec3d.scale(speed * 1.0)
            val f = 0.003
            if (abs(vec3d3.x) < 0.003 && abs(vec3d3.z) < 0.003 && vec3d.length() < 0.0045000000000000005) {
                vec3d = vec3d.withLength(0.0045000000000000005)
            }
            deltaMovement += vec3d
        }

        this.fluidHeight.put(tag, d)
        return bl2
    }

    private fun isRegionUnloaded(): Boolean {
        val box = this.boundingBox.inflate(1.0)
        val i = Mth.floor(box.minX)
        val j = Mth.ceil(box.maxX)
        val k = Mth.floor(box.minZ)
        val l = Mth.ceil(box.maxZ)
        return !this.player.level().hasChunksAt(i, k, j, l)
    }

    private fun getRotationVector() = getRotationVector(this.xRot, this.yRot)

    private fun getRotationVector(pitch: Float, yaw: Float): Vec3 {
        val f = pitch * (Math.PI.toFloat() / 180)
        val g = -yaw * (Math.PI.toFloat() / 180)

        val h = g.fastCos()
        val i = g.fastSin()
        val j = f.fastCos()
        val k = f.fastSin()

        return Vec3((i * j).toDouble(), (-k).toDouble(), (h * j).toDouble())
    }

    private fun hasStatusEffect(effect: Holder<MobEffect>): Boolean {
        val instance = player.getEffect(effect) ?: return false

        return instance.duration >= this.simulatedTicks
    }

    private fun getStatusEffect(effect: Holder<MobEffect>): MobEffectInstance? {
        val instance = player.getEffect(effect) ?: return null

        if (instance.duration < this.simulatedTicks) {
            return null
        }

        return instance
    }

    fun getAttributeValue(attribute: Holder<Attribute>): Double {
        return player.attributes.getValue(attribute)
    }

    fun clone(): SimulatedPlayer {
        return SimulatedPlayer(
            player,
            input,
            pos,
            deltaMovement,
            boundingBox,
            yRot,
            xRot,
            isSprinting,
            fallDistance,
            jumpingCooldown,
            isJumping,
            isFallFlying,
            onGround,
            horizontalCollision,
            verticalCollision,
            wasTouchingWater,
            isSwimming,
            wasUnderwater,
            Object2DoubleArrayMap(fluidHeight),
            HashSet(fluidOnEyes)
        )
    }

    class SimulatedPlayerInput(
        val directionalInput: DirectionalInput,
        jumping: Boolean,
        var sprinting: Boolean,
        sneaking: Boolean,
        var ignoreClippingAtLedge: Boolean = false
    ) : ClientInput() {
        var forceSafeWalk: Boolean = false

        init {
            set(
                forward = directionalInput.forwards,
                backward = directionalInput.backwards,
                left = directionalInput.left,
                right = directionalInput.right,
                jump = jumping,
                sneak = sneaking
            )
        }

        fun update() {
            if (this.keyPresses.forward != this.keyPresses.backward) {
                this.movementForward = if (this.keyPresses.forward) 1.0f else -1.0f
            } else {
                this.movementForward = 0.0f
            }

            movementSideways = if (keyPresses.left == keyPresses.right) {
                0.0f
            } else if (keyPresses.left) {
                1.0f
            } else {
                -1.0f
            }

            if (keyPresses.shift) {
                movementSideways = (movementSideways.toDouble() * 0.3).toFloat()
                movementForward = (movementForward.toDouble() * 0.3).toFloat()
            }
        }

        override fun toString(): String {
            return "SimulatedPlayerInput(forwards={${this.keyPresses.forward}}, backwards={${this.keyPresses.backward}}, left={${this.keyPresses.left}}, right={${this.keyPresses.right}}, jumping={${this.keyPresses.jump}}, sprinting=$sprinting, slowDown=${keyPresses.shift})"
        }

        companion object {
            private const val MAX_WALKING_SPEED = 0.121

            @JvmStatic
            fun fromClientPlayer(
                directionalInput: DirectionalInput,
                jump: Boolean = player.input.keyPresses.jump,
                sprinting: Boolean = player.isSprinting,
                sneaking: Boolean = player.isShiftKeyDown
            ): SimulatedPlayerInput {
                val input = SimulatedPlayerInput(
                    directionalInput,
                    jump,
                    sprinting,
                    sneaking
                )

                val safeWalkEvent = PlayerSafeWalkEvent()

                callEvent(safeWalkEvent)

                if (safeWalkEvent.isSafeWalk) {
                    input.forceSafeWalk = true
                }

                return input
            }

            /**
             * Guesses the current input of a server player based on player position and velocity
             */
            @JvmStatic
            fun guessInput(entity: Player): SimulatedPlayerInput {
                val velocity = entity.position().subtract(entity.lastPos)

                val horizontalVelocity = velocity.horizontalDistanceSqr()

                val sprinting = horizontalVelocity >= MAX_WALKING_SPEED * MAX_WALKING_SPEED

                val input = if (horizontalVelocity > 0.05 * 0.05) {
                    val velocityAngle = getDegreesRelativeToView(velocity, yaw = entity.yRot)

                    val velocityAngle1 = Mth.wrapDegrees(velocityAngle)

                    getDirectionalInputForDegrees(DirectionalInput.NONE, velocityAngle1)
                } else {
                    DirectionalInput.NONE
                }

                val jumping = !entity.onGround()

                return SimulatedPlayerInput(
                    input,
                    jumping,
                    sprinting,
                    sneaking=entity.isShiftKeyDown
                )
            }
        }

    }
}
