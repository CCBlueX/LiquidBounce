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

import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
import net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.MixinEntityFluidInteractionAccessor
import net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.MixinEntityFluidInteractionTrackerAccessor
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.math.fastCos
import net.ccbluex.liquidbounce.utils.math.fastSin
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.math.toRadians
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.toBlockPos
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
import net.minecraft.world.attribute.EnvironmentAttributes
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityFluidInteraction
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LadderBlock
import net.minecraft.world.level.block.PowderSnowBlock
import net.minecraft.world.level.block.TrapDoorBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluid
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
    private var jumpTriggerTime: Int,
    private var jumping: Boolean,
    private var fallFlying: Boolean,
    var onGround: Boolean,
    var horizontalCollision: Boolean,
    private var verticalCollision: Boolean,

    private var wasTouchingWater: Boolean,
    private var isSwimming: Boolean,
    private var wasUnderwater: Boolean,
    private val fluidInteraction: EntityFluidInteraction,
) : PlayerSimulation {
    private val level: Level get() = player.level()

    companion object {
        private fun EntityFluidInteraction.deepCopy(): EntityFluidInteraction {
            val sourceTrackers = (this as MixinEntityFluidInteractionAccessor).trackerByFluid()
            val copy = EntityFluidInteraction(sourceTrackers.keys)
            @Suppress("CAST_NEVER_SUCCEEDS")
            val targetTrackers = (copy as MixinEntityFluidInteractionAccessor).trackerByFluid()

            for ((fluid, sourceTracker) in sourceTrackers) {
                val targetTracker = targetTrackers[fluid] ?: continue

                val sourceAccessor = sourceTracker as MixinEntityFluidInteractionTrackerAccessor
                val targetAccessor = targetTracker as MixinEntityFluidInteractionTrackerAccessor

                targetAccessor.height(sourceAccessor.height())
                targetAccessor.eyesInside(sourceAccessor.eyesInside())
                targetAccessor.accumulatedCurrent(sourceAccessor.accumulatedCurrent())
                targetAccessor.currentCount(sourceAccessor.currentCount())
            }

            return copy
        }

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
                player.fluidInteraction.deepCopy(),
            )
        }

        @JvmStatic
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
                player.fluidInteraction.deepCopy(),
            )
        }
    }

    private var simulatedTicks: Int = 0
    var clipLedged = false
        private set

    override fun tick() {
        clipLedged = false

        if (pos.y <= -70) {
            return
        }

        this.input.update()

        updateFluidInteraction()
        updateIsUnderwater()
        updateSwimming()

        if (this.jumpTriggerTime > 0) {
            this.jumpTriggerTime--
        }

        this.jumping = this.input.keyPresses.jump

        val movement = this.deltaMovement

        var motionX = movement.x
        var motionY = movement.y
        var motionZ = movement.z

        if (abs(movement.x) < 0.003) {
            motionX = 0.0
        }
        if (abs(movement.y) < 0.003) {
            motionY = 0.0
        }
        if (abs(movement.z) < 0.003) {
            motionZ = 0.0
        }
        if (onGround) {
            this.fallFlying = false
        }

        this.deltaMovement = Vec3(motionX, motionY, motionZ)

        if (this.jumping) {
            val fluidHeight =
                if (this.isInLava()) this.getFluidHeight(FluidTags.LAVA) else this.getFluidHeight(FluidTags.WATER)
            val inWater = this.isInWater() && fluidHeight > 0.0

            val swimHeight = this.getFluidJumpThreshold()

            if (inWater && (!this.onGround || fluidHeight > swimHeight)) {
                this.swimUpward(FluidTags.WATER)
            } else if (this.isInLava() && (!this.onGround || fluidHeight > swimHeight)) {
                this.swimUpward(FluidTags.LAVA)
            } else if ((this.onGround || inWater && fluidHeight <= swimHeight) && jumpTriggerTime == 0) {
                this.jumpFromGround()
                jumpTriggerTime = 10
            }
        }

        val sidewaysSpeed = input.movementSideways * 0.98
        val forwardSpeed = input.movementForward * 0.98
        val upwardsSpeed = 0.0

        if (this.hasStatusEffect(MobEffects.SLOW_FALLING) || this.hasStatusEffect(MobEffects.LEVITATION)) {
            this.onLanding()
        }

        this.travel(Vec3(sidewaysSpeed, upwardsSpeed, forwardSpeed))
        simulatedTicks++
    }

    private fun travel(movementInput: Vec3) {
        if (this.isSwimming && !this.player.isPassenger) {
            val viewY = this.getViewVector().y
            val swimLift = if (viewY < -0.2) 0.085 else 0.06
            if (viewY <= 0.0 || this.input.keyPresses.jump || !this.player.level()
                    .getBlockState(BlockPos.containing(this.pos.x, this.pos.y + 1.0 - 0.1, this.pos.z))
                    .fluidState.isEmpty
            ) {
                deltaMovement = deltaMovement.add(0.0, (viewY - deltaMovement.y) * swimLift, 0.0)
            }
        }

        val beforeTravelVelocityY = this.deltaMovement.y

        var gravity = 0.08
        val isFalling = deltaMovement.y <= 0.0
        if (deltaMovement.y <= 0.0 && hasStatusEffect(MobEffects.SLOW_FALLING)) {
            gravity = 0.01
            this.onLanding()
        }

        if (isInWater() && this.player.isAffectedByFluids) {
            val playerY: Double = this.pos.y
            var movementSpeed = if (isSprinting) 0.9f else 0.8f
            var movementEfficiency = 0.02f
            var waterMovementEfficiency = this.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY).toFloat()

            if (!onGround) {
                waterMovementEfficiency *= 0.5f
            }
            if (waterMovementEfficiency > 0.0f) {
                movementSpeed += (0.54600006f - movementSpeed) * waterMovementEfficiency / 3.0f
                movementEfficiency += (this.getSpeed() - movementEfficiency) * waterMovementEfficiency / 3.0f
            }
            if (hasStatusEffect(MobEffects.DOLPHINS_GRACE)) {
                movementSpeed = 0.96f
            }
            this.updateVelocity(movementEfficiency, movementInput)
            this.move(deltaMovement)
            var movement = deltaMovement
            if (this.horizontalCollision && this.isClimbing()) {
                movement = Vec3(movement.x, 0.2, movement.z)
            }
            deltaMovement = movement.multiply(movementSpeed.toDouble(), 0.8, movementSpeed.toDouble())
            val adjustedMovement = this.player.getFluidFallingAdjustedMovement(gravity, isFalling, deltaMovement)
            deltaMovement = adjustedMovement
            if (this.horizontalCollision && this.doesNotCollide(
                    adjustedMovement.x,
                    adjustedMovement.y + 0.6 - this.pos.y + playerY,
                    adjustedMovement.z
                )
            ) {
                this.deltaMovement = Vec3(adjustedMovement.x, 0.3, adjustedMovement.z)
            }
        } else if (isInLava() && this.player.isAffectedByFluids) {
            val playerY: Double = this.pos.y
            this.updateVelocity(0.02f, movementInput)
            this.move(deltaMovement)
            if (getFluidHeight(FluidTags.LAVA) <= getFluidJumpThreshold()) {
                deltaMovement = deltaMovement.multiply(0.5, 0.8, 0.5)
                deltaMovement = this.player.getFluidFallingAdjustedMovement(gravity, isFalling, deltaMovement)
            } else {
                deltaMovement = deltaMovement.scale(0.5)
            }
            if (!this.player.isNoGravity) {
                deltaMovement = this.deltaMovement.add(0.0, -gravity / 4.0, 0.0)
            }
            if (this.horizontalCollision && this.doesNotCollide(
                    deltaMovement.x,
                    deltaMovement.y + 0.6 - this.pos.y + playerY,
                    deltaMovement.z
                )
            ) {
                deltaMovement = Vec3(deltaMovement.x, 0.3, deltaMovement.z)
            }
        } else if (this.fallFlying) {
            var lift: Double
            var velocity: Vec3 = this.deltaMovement
            if (velocity.y > -0.5) {
                fallDistance = 1.0
            }
            val vec3d3 = this.getViewVector()
            val pitchRadians: Float = this.xRot * (Math.PI.toFloat() / 180)
            val horizontalViewMagnitude = sqrt(vec3d3.x * vec3d3.x + vec3d3.z * vec3d3.z)
            val horizontalVelocity = velocity.horizontalDistance()
            val viewVectorLength = vec3d3.length()
            var liftFactor = pitchRadians.fastCos()
            liftFactor =
                (liftFactor.toDouble() * (liftFactor.toDouble() * 1.0.coerceAtMost(viewVectorLength / 0.4))).toFloat()
            velocity = this.deltaMovement.add(0.0, gravity * (-1.0 + liftFactor.toDouble() * 0.75), 0.0)
            if (velocity.y < 0.0 && horizontalViewMagnitude > 0.0) {
                lift = velocity.y * -0.1 * liftFactor.toDouble()
                velocity = velocity.add(
                    vec3d3.x * lift / horizontalViewMagnitude,
                    lift,
                    vec3d3.z * lift / horizontalViewMagnitude
                )
            }
            if (pitchRadians < 0.0f && horizontalViewMagnitude > 0.0) {
                lift = horizontalVelocity * (-pitchRadians.fastSin()).toDouble() * 0.04
                velocity = velocity.add(
                    -vec3d3.x * lift / horizontalViewMagnitude,
                    lift * 3.2,
                    -vec3d3.z * lift / horizontalViewMagnitude
                )
            }
            if (horizontalViewMagnitude > 0.0) {
                velocity = velocity.add(
                    (vec3d3.x / horizontalViewMagnitude * horizontalVelocity - velocity.x) * 0.1,
                    0.0,
                    (vec3d3.z / horizontalViewMagnitude * horizontalVelocity - velocity.z) * 0.1
                )
            }
            this.deltaMovement = velocity.multiply(0.99, 0.98, 0.99)

            move(this.deltaMovement)
        } else {
            val blockPos = this.getBlockPosBelowThatAffectsMyMovement()
            val p: Float = this.player.level().getBlockState(blockPos).block.friction
            val friction = if (onGround) p * 0.91f else 0.91f
            val movement = this.applyMovementInput(movementInput, p)
            var verticalMovement = movement.y
            if (hasStatusEffect(MobEffects.LEVITATION)) {
                verticalMovement += (0.05 * (getStatusEffect(MobEffects.LEVITATION)!!.amplifier + 1).toDouble() - movement.y) * 0.2
            } else if (this.player.level().isClientSide && !this.player.level().hasChunkAt(blockPos.x, blockPos.z)) {
                verticalMovement = if (this.pos.y > this.player.level().minY.toDouble()) {
                    -0.1
                } else {
                    0.0
                }
            } else if (!this.player.isNoGravity) {
                verticalMovement -= gravity
            }

            deltaMovement = if (this.player.shouldDiscardFriction()) {
                Vec3(movement.x, verticalMovement, movement.z)
            } else {
                Vec3(
                    movement.x * friction.toDouble(),
                    verticalMovement * 0.9800000190734863,
                    movement.z * friction.toDouble()
                )
            }
        }

        if (player.abilities.flying && !this.player.isPassenger) {
            deltaMovement = Vec3(deltaMovement.x, beforeTravelVelocityY * 0.6, deltaMovement.z)
            this.onLanding()
        }
    }

    /**
     * @see net.minecraft.world.entity.LivingEntity.handleRelativeFrictionAndCalculateMovement(Vec3, float)
     */
    private fun applyMovementInput(movementInput: Vec3, slipperiness: Float): Vec3 {
        this.updateVelocity(this.getFrictionInfluencedSpeed(slipperiness), movementInput)
        this.deltaMovement = handleOnClimbable(this.deltaMovement)
        this.deltaMovement = applyWebSpeed(this.deltaMovement)
        this.move(this.deltaMovement)


        var vec3d = this.deltaMovement
        if ((horizontalCollision || this.jumping) && (
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

    /**
     * @see net.minecraft.world.entity.LivingEntity.getFrictionInfluencedSpeed(float)
     */
    private fun getFrictionInfluencedSpeed(slipperiness: Float): Float {
        return if (this.onGround) {
            getSpeed() * (0.21600002f / (slipperiness * slipperiness * slipperiness))
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

    private fun getSpeed(): Float = 0.10000000149011612.toFloat()

    private fun move(input: Vec3) {
        val event = callEvent(PlayerMoveEvent(MoverType.SELF, input))
        val movement = event.movement

        val backedOffMovement = this.maybeBackOffFromEdge(movement)
        val adjustedMovement = this.adjustMovementForCollisions(backedOffMovement)

        if (adjustedMovement.lengthSqr() > 1.0E-7) {
            this.pos += adjustedMovement
            this.boundingBox = player.dimensions.makeBoundingBox(this.pos)
        }

        val xCollision = !Mth.equal(backedOffMovement.x, adjustedMovement.x)
        val zCollision = !Mth.equal(backedOffMovement.z, adjustedMovement.z)

        this.horizontalCollision = xCollision || zCollision
        this.verticalCollision = backedOffMovement.y != adjustedMovement.y

        onGround = verticalCollision && backedOffMovement.y < 0.0

        if (!isInWater()) {
            updateFluidInteraction()
        }

        if (onGround) {
            onLanding()
        } else if (backedOffMovement.y < 0) {
            fallDistance -= backedOffMovement.y.toFloat()
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
        val onGroundOrFalling: Boolean
        val collisionBox: AABB = AABB(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).move(this.pos)

        val entityCollisionList = emptyList<VoxelShape>()

        val adjustedMovement = if (movement.lengthSqr() == 0.0) {
            movement
        } else {
            Entity.collideBoundingBox(
                this.player,
                movement,
                collisionBox,
                this.player.level(),
                entityCollisionList
            )
        }
        val collidedX = movement.x != adjustedMovement.x
        val collidedY = movement.y != adjustedMovement.y
        val collidedZ = movement.z != adjustedMovement.z

        onGroundOrFalling = onGround || collidedY && movement.y < 0.0

        if (this.player.maxUpStep() > 0.0f && onGroundOrFalling && (collidedX || collidedZ)) {
            var steppedMovement = Entity.collideBoundingBox(
                this.player,
                Vec3(movement.x, this.player.maxUpStep().toDouble(), movement.z),
                collisionBox,
                this.player.level(),
                entityCollisionList
            )
            val stepUpMovement = Entity.collideBoundingBox(
                this.player,
                Vec3(0.0, this.player.maxUpStep().toDouble(), 0.0),
                collisionBox.expandTowards(movement.x, 0.0, movement.z),
                this.player.level(),
                entityCollisionList
            )
            val stepDownMovement = Entity.collideBoundingBox(
                this.player,
                Vec3(movement.x, 0.0, movement.z),
                collisionBox.move(stepUpMovement),
                this.player.level(),
                entityCollisionList
            ).add(stepUpMovement)

            if (stepUpMovement.y < this.player.maxUpStep()
                    .toDouble() && stepDownMovement.horizontalDistanceSqr() > steppedMovement.horizontalDistanceSqr()
            ) {
                steppedMovement = stepDownMovement
            }

            if (steppedMovement.horizontalDistanceSqr() > adjustedMovement.horizontalDistanceSqr()) {
                return steppedMovement.add(
                    Entity.collideBoundingBox(
                        this.player,
                        Vec3(0.0, -steppedMovement.y + movement.y, 0.0),
                        collisionBox.move(steppedMovement),
                        this.player.level(),
                        entityCollisionList
                    )
                )
            }
        }
        return adjustedMovement
    }

    private fun onLanding() {
        this.fallDistance = 0.0
    }

    /**
     * @see net.minecraft.world.entity.LivingEntity.jumpFromGround()
     */
    fun jumpFromGround() {
        val jumpPower = this.getJumpPower().toDouble()
        this.deltaMovement = Vec3(this.deltaMovement.x, max(jumpPower, this.deltaMovement.y), this.deltaMovement.z)

        if (isSprinting) {
            val yawRadians: Float = this.yRot.toRadians()

            this.deltaMovement += Vec3(
                (-yawRadians.fastSin() * 0.2f).toDouble(),
                0.0,
                (yawRadians.fastCos() * 0.2f).toDouble()
            )
        }

    }

    fun jump() = jumpFromGround()

    /**
     * @see net.minecraft.world.entity.LivingEntity.handleOnClimbable(Vec3)
     */
    private fun handleOnClimbable(motion: Vec3): Vec3 {
        if (!isClimbing()) {
            return motion
        }

        onLanding()
        val clampedX = Mth.clamp(motion.x, -0.15000000596046448, 0.15000000596046448)
        val clampedZ = Mth.clamp(motion.z, -0.15000000596046448, 0.15000000596046448)
        var clampedY = max(motion.y, -0.15000000596046448)
        if (clampedY < 0.0 && !pos.toBlockPos().getState()!!
                .`is`(Blocks.SCAFFOLDING) && player.isSuppressingSlidingDownLadder
        ) {
            clampedY = 0.0
        }

        return Vec3(clampedX, clampedY, clampedZ)
    }

    private fun applyWebSpeed(motion: Vec3): Vec3 {
        val blockState = level.getBlockState(pos.toBlockPos())
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
        } else if (blockState.block is TrapDoorBlock && this.trapdoorUsableAsLadder(blockPos, blockState)) {
            true
        } else {
            false
        }
    }

    /**
     * @see net.minecraft.world.entity.LivingEntity.trapdoorUsableAsLadder(BlockPos, BlockState)
     */
    private fun trapdoorUsableAsLadder(pos: BlockPos, state: BlockState): Boolean {
        if (!state.getValue(TrapDoorBlock.OPEN)) {
            return false
        }
        val blockState = this.player.level().getBlockState(pos.below())
        return blockState.`is`(Blocks.LADDER) && blockState.getValue(LadderBlock.FACING) == state.getValue(TrapDoorBlock.FACING)
    }

    /**
     * @see net.minecraft.world.entity.player.Player.maybeBackOffFromEdge(Vec3, MoverType)
     */
    private fun maybeBackOffFromEdge(movement: Vec3): Vec3 {
        var adjustedMovement = movement

        if (adjustedMovement.y <= 0.0 && this.isAboveGround()) {
            var xMovement = adjustedMovement.x
            var zMovement = adjustedMovement.z
            val step = 0.05
            while (xMovement != 0.0 && level.noCollision(
                    player,
                    boundingBox.move(xMovement, -STEP_HEIGHT, 0.0)
                )
            ) {
                if (xMovement < step && xMovement >= -step) {
                    xMovement = 0.0
                    continue
                }
                if (xMovement > 0.0) {
                    xMovement -= step
                    continue
                }
                xMovement += step
            }
            while (zMovement != 0.0 && level.noCollision(
                    player,
                    boundingBox.move(0.0, -STEP_HEIGHT, zMovement)
                )
            ) {
                if (zMovement < step && zMovement >= -step) {
                    zMovement = 0.0
                    continue
                }
                if (zMovement > 0.0) {
                    zMovement -= step
                    continue
                }
                zMovement += step
            }
            while (xMovement != 0.0 && zMovement != 0.0 && level.noCollision(
                    player,
                    boundingBox.move(xMovement, -STEP_HEIGHT, zMovement)
                )
            ) {
                xMovement =
                    if (xMovement < step && xMovement >= -step) 0.0 else if (xMovement > 0.0) {
                        xMovement - step
                    } else {
                        xMovement + step
                    }
                if (zMovement < step && zMovement >= -step) {
                    zMovement = 0.0
                    continue
                }
                if (zMovement > 0.0) {
                    zMovement -= step
                    continue
                }
                zMovement += step
            }

            if (adjustedMovement.x != xMovement || adjustedMovement.z != zMovement) {
                clipLedged = true
            }

            if (this.shouldClipAtLedge()) {
                adjustedMovement = Vec3(xMovement, adjustedMovement.y, zMovement)
            }
        }
        return adjustedMovement
    }

    private fun shouldClipAtLedge(): Boolean {
        return !this.input.ignoreClippingAtLedge && (this.input.keyPresses.shift || this.input.forceSafeWalk)
    }

    private fun isAboveGround(): Boolean {
        return onGround || this.fallDistance < STEP_HEIGHT && !level.noCollision(
            player,
            boundingBox.move(0.0, this.fallDistance - STEP_HEIGHT, 0.0)
        )
    }

    /**
     * Mirrors 26.1 `LivingEntity#getJumpPower()`.
     * @see net.minecraft.world.entity.LivingEntity.getJumpPower
     */
    private fun getJumpPower(): Float =
        this.getAttributeValue(Attributes.JUMP_STRENGTH)
            .toFloat() * this.getJumpVelocityMultiplier() + this.getJumpBoostPower()

    /**
     * Mirrors 26.1 `LivingEntity#getJumpBoostPower()`.
     * @see net.minecraft.world.entity.LivingEntity.getJumpBoostPower
     */
    private fun getJumpBoostPower() =
        if (hasStatusEffect(MobEffects.JUMP_BOOST)) {
            0.1f * (getStatusEffect(MobEffects.JUMP_BOOST)!!.amplifier.toFloat() + 1f)
        } else {
            0f
        }

    /**
     * Mirrors the 26.1 block jump factor lookup used by `LivingEntity#getJumpPower()`.
     * @see net.minecraft.world.entity.Entity.getBlockJumpFactor
     */
    private fun getJumpVelocityMultiplier(): Float {
        val f = pos.toBlockPos().getBlock()?.jumpFactor ?: 0f
        val g = getBlockPosBelowThatAffectsMyMovement().getBlock()?.jumpFactor ?: 0f

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

    /**
     * Mirrors 26.1 `Entity#getBlockPosBelowThatAffectsMyMovement()`.
     * @see net.minecraft.world.entity.Entity.getBlockPosBelowThatAffectsMyMovement
     */
    private fun getBlockPosBelowThatAffectsMyMovement() =
        BlockPos.containing(this.pos.x, this.boundingBox.minY - 0.5000001, this.pos.z)

    /**
     * Mirrors 26.1 `Entity#getFluidJumpThreshold()`.
     * @see net.minecraft.world.entity.Entity.getFluidJumpThreshold
     */
    private fun getFluidJumpThreshold(): Double {
        return if (player.eyeHeight.toDouble() < 0.4) 0.0 else 0.4
    }

    /**
     * Mirrors 26.1 `Entity#isEyeInFluid(TagKey)`, restricted to water for the underwater state.
     * @see net.minecraft.world.entity.Entity.isEyeInFluid
     */
    private fun isEyeInWater(): Boolean {
        return this.fluidInteraction.isEyeInFluid(FluidTags.WATER)
    }

    private fun isInWater(): Boolean = wasTouchingWater

    /**
     * @see Entity.isInLava
     */
    private fun isInLava(): Boolean {
        return this.fluidInteraction.isInFluid(FluidTags.LAVA)
    }

    /**
     * Mirrors 26.1 `Player#updateSwimming()`.
     * @see net.minecraft.world.entity.player.Player.updateSwimming
     */
    private fun updateSwimming() {
        isSwimming = if (this.isSwimming) {
            isSprinting && isInWater() && !this.player.isPassenger
        } else {
            isSprinting && this.isSubmergedInWater() &&
                !this.player.isPassenger &&
                this.player.level()
                    .getFluidState(this.pos.toBlockPos())
                    .`is`(FluidTags.WATER)
        }
    }

    /**
     * Mirrors 26.1 `Entity#updateFluidInteraction()`.
     * @see net.minecraft.world.entity.Entity.updateFluidInteraction
     */
    private fun updateFluidInteraction(): Boolean {
        this.fluidInteraction.update(this.player, !this.player.isAffectedByFluids)

        val inWater = this.fluidInteraction.isInFluid(FluidTags.WATER)
        val inLava = this.fluidInteraction.isInFluid(FluidTags.LAVA)

        if (inWater) {
            onLanding()
        }

        this.wasTouchingWater = inWater
        if (this.player.isAffectedByFluids) {
            if (inWater) {
                this.fluidInteraction.applyCurrentTo(FluidTags.WATER, this.player, 0.014)
            }

            if (inLava) {
                val lavaFlowScale = if (this.level.environmentAttributes()
                        .getDimensionValue(EnvironmentAttributes.FAST_LAVA)
                ) {
                    0.007
                } else {
                    0.0023333333333333335
                }
                this.fluidInteraction.applyCurrentTo(FluidTags.LAVA, this.player, lavaFlowScale)
            }
        }

        return inWater || inLava
    }

    /**
     * Mirrors 26.1 `Player#updateIsUnderwater()`.
     * @see net.minecraft.world.entity.player.Player.updateIsUnderwater
     */
    private fun updateIsUnderwater(): Boolean {
        this.wasUnderwater = this.isEyeInWater()
        return this.wasUnderwater
    }

    private fun isSubmergedInWater(): Boolean {
        return this.wasUnderwater && isInWater()
    }

    private fun getFluidHeight(tags: TagKey<Fluid>): Double =
        this.fluidInteraction.getFluidHeight(tags)

    /**
     * Mirrors 26.1 `Entity#getViewVector()`.
     * @see net.minecraft.world.entity.Entity.getViewVector
     */
    private fun getViewVector(): Vec3 = calculateViewVector(this.xRot, this.yRot)

    /**
     * Mirrors 26.1 `Entity#calculateViewVector(float, float)`.
     * @see net.minecraft.world.entity.Entity.calculateViewVector
     */
    private fun calculateViewVector(xRot: Float, yRot: Float): Vec3 {
        val realXRot = xRot * (Math.PI.toFloat() / 180f)
        val realYRot = -yRot * (Math.PI.toFloat() / 180f)
        val yCos = Mth.cos(realYRot.toDouble())
        val ySin = Mth.sin(realYRot.toDouble())
        val xCos = Mth.cos(realXRot.toDouble())
        val xSin = Mth.sin(realXRot.toDouble())
        return Vec3((ySin * xCos).toDouble(), (-xSin).toDouble(), (yCos * xCos).toDouble())
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
            jumpTriggerTime,
            jumping,
            fallFlying,
            onGround,
            horizontalCollision,
            verticalCollision,
            wasTouchingWater,
            isSwimming,
            wasUnderwater,
            fluidInteraction.deepCopy(),
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
                    sneaking = entity.isShiftKeyDown
                )
            }
        }

    }
}
