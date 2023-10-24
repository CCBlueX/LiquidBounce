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

import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDegreesRelativeToView
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees
import net.minecraft.client.input.Input
import net.minecraft.entity.Entity
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.Fluid
import net.minecraft.registry.tag.FluidTags
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.util.shape.VoxelShape
import kotlin.math.abs
import kotlin.math.sqrt

class SimulatedPlayer(
    private val player: PlayerEntity,
    var input: SimulatedPlayerInput,
    override var pos: Vec3d,
    var velocity: Vec3d,
    private val yaw: Float,
    private val pitch: Float,
    private var sprinting: Boolean,

    private var fallDistance: Float,
    private var jumpingCooldown: Int,
    private var isJumping: Boolean,
    private var onGround: Boolean,
    private var horizontalCollision: Boolean,
    private var verticalCollision: Boolean
) : PlayerSimulation {
    companion object {
        fun fromClientPlayer(input: SimulatedPlayerInput): SimulatedPlayer {
            val player = mc.player!!
            return SimulatedPlayer(
                player,
                input,
                player.pos,
                player.velocity,
                player.yaw,
                player.pitch,

                player.isSprinting,

                player.fallDistance,
                player.jumpingCooldown,
                player.jumping,
                player.isOnGround,
                player.horizontalCollision,
                player.verticalCollision
            )
        }
        fun fromOtherPlayer(player: PlayerEntity, input: SimulatedPlayerInput): SimulatedPlayer {
            return SimulatedPlayer(
                player,
                input,
                player.pos,
                velocity = player.pos.subtract(player.prevPos),
                player.yaw,
                player.pitch,

                player.isSprinting,

                player.fallDistance,
                player.jumpingCooldown,
                player.jumping,
                player.isOnGround,
                player.horizontalCollision,
                player.verticalCollision
            )
        }
    }

    private var simulatedTicks: Int = 0

    override fun tick() {
        this.input.update()

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

        this.travel(Vec3d(sidewaysSpeed, upwardsSpeed, forwardSpeed))
    }

    private fun travel(movementInput: Vec3d) {
        var d = 0.08

        if (velocity.y <= 0.0 && hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            d = 0.01
            this.onLanding()
        }

//        val fluidState: FluidState = this.player.world.getFluidState(this.getBlockPos())

//        if (isTouchingWater() && this.shouldSwimInFluids() && !this.canWalkOnFluid(fluidState.fluid)) {
//            val e: Double = this.getY()
//            var f = if (isSprinting()) 0.9f else this.getBaseMovementSpeedMultiplier()
//            var g = 0.02f
//            var h = EnchantmentHelper.getDepthStrider(this).toFloat()
//            if (h > 3.0f) {
//                h = 3.0f
//            }
//            if (!onGround) {
//                h *= 0.5f
//            }
//            if (h > 0.0f) {
//                f += (0.54600006f - f) * h / 3.0f
//                g += (this.getMovementSpeed() - g) * h / 3.0f
//            }
//            if (hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
//                f = 0.96f
//            }
//            this.updateVelocity(g, movementInput)
//            this.move(MovementType.SELF, this.getVelocity())
//            var vec3d: Vec3d = this.getVelocity()
//            if (this.horizontalCollision && this.isClimbing()) {
//                vec3d = Vec3d(vec3d.x, 0.2, vec3d.z)
//            }
//            this.setVelocity(vec3d.multiply(f.toDouble(), 0.8, f.toDouble()))
//            val vec3d2: Vec3d = this.method_26317(d, bl, this.getVelocity())
//            this.setVelocity(vec3d2)
//            if (this.horizontalCollision && this.doesNotCollide(vec3d2.x, vec3d2.y + 0.6 - this.getY() + e, vec3d2.z)) {
//                this.setVelocity(vec3d2.x, 0.3, vec3d2.z)
//            }
//        } else if (isInLava() && this.shouldSwimInFluids() && !this.canWalkOnFluid(fluidState.fluid)) {
//            var f: Vec3d
//            val e: Double = this.getY()
//            this.updateVelocity(0.02f, movementInput)
//            this.move(MovementType.SELF, this.getVelocity())
//            if (getFluidHeight(FluidTags.LAVA) <= getSwimHeight()) {
//                this.setVelocity(this.getVelocity().multiply(0.5, 0.8, 0.5))
//                f = this.method_26317(d, bl, this.getVelocity())
//                this.setVelocity(f)
//            } else {
//                this.setVelocity(this.getVelocity().multiply(0.5))
//            }
//            if (!this.hasNoGravity()) {
//                this.setVelocity(this.getVelocity().add(0.0, -d / 4.0, 0.0))
//            }
//            f = this.getVelocity()
//            if (this.horizontalCollision && this.doesNotCollide(f.x, f.y + 0.6 - this.getY() + e, f.z)) {
//                this.setVelocity(f.x, 0.3, f.z)
//            }
//        } else
        if (this.isFallFlying()) {
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
            val e: BlockPos = this.getVelocityAffectingPos()
            val vec3d3: Float = this.player.world.getBlockState(e).block.slipperiness
            val f = if (onGround) vec3d3 * 0.91f else 0.91f
            val g: Vec3d = this.applyMovementInput(movementInput, vec3d3)

            //            if (hasStatusEffect(StatusEffects.LEVITATION)) {
//                h += (0.05 * (this.getStatusEffect(StatusEffects.LEVITATION).getAmplifier() + 1).toDouble() - g.y) * 0.2
//                this.onLanding()
//            } else if (!this.world.isClient || this.world.isChunkLoaded(e)) {
//                if (!this.hasNoGravity()) {
//                    h -= d
//                }
//            } else {
//                h = if (this.pos.y > this.world.getBottomY().toDouble()) -0.1 else 0.0
//            }
            velocity = Vec3d(g.x * f.toDouble(), (g.y - d) * 0.98, g.z * f.toDouble())
        }
    }

    private fun applyMovementInput(movementInput: Vec3d?, slipperiness: Float): Vec3d {
        this.updateVelocity(this.getMovementSpeed(slipperiness), movementInput)

        this.move(this.velocity)

        return this.velocity
    }

    private fun updateVelocity(speed: Float, movementInput: Vec3d?) {
        val vec3d = Entity.movementInputToVelocity(movementInput, speed, this.yaw)

        this.velocity += vec3d
    }

    private fun getMovementSpeed(slipperiness: Float): Float {
        return if (this.onGround) {
            getMovementSpeed() * (0.21600002f / (slipperiness * slipperiness * slipperiness))
        } else this.getAirStrafingSpeed()
    }

    private fun getAirStrafingSpeed(): Float {
        val speed = 0.02f

        if (this.input.sprinting) {
            return (speed + 0.005999999865889549).toFloat()
        }

        return speed
    }

    private fun getMovementSpeed(): Float = 0.10000000149011612.toFloat()

    private fun move(movement: Vec3d) {
        val adjustedMovement = this.adjustMovementForCollisions(movement)

        if (adjustedMovement.lengthSquared() > 1.0E-7) {
            this.pos += adjustedMovement
        }

        val xCollision = !MathHelper.approximatelyEquals(movement.x, adjustedMovement.x)
        val zCollision = !MathHelper.approximatelyEquals(movement.z, adjustedMovement.z)

        this.horizontalCollision = xCollision || zCollision
        this.verticalCollision = movement.y != adjustedMovement.y

        onGround = verticalCollision && movement.y < 0.0


        val vec3d2: Vec3d = this.velocity
//        if(onGround) {
//            this.velocity = Vec3d(vec3d2.x, vec3d2.y.coerceAtLeast(0.0), vec3d2.z)
//        }

        if (horizontalCollision || verticalCollision) {
            this.velocity = Vec3d(
                if (xCollision) 0.0 else vec3d2.x,
                if (onGround) 0.0 else vec3d2.y,
                if (zCollision) 0.0 else vec3d2.z
            )
        }

    }

    //

    private fun adjustMovementForCollisions(movement: Vec3d): Vec3d {
        val bl4: Boolean
        val box: Box = Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).offset(this.pos)

        val entityCollisionList = emptyList<VoxelShape>()

        val vec3d = if (movement.lengthSquared() == 0.0) movement else Entity.adjustMovementForCollisions(
            this.player,
            movement,
            box,
            this.player.world,
            entityCollisionList
        )
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

    private fun isFallFlying(): Boolean = false

    private fun onLanding() {
        this.fallDistance = 0.0f
    }

    fun jump() {
        this.velocity += Vec3d(0.0, this.getJumpVelocity().toDouble() + this.getJumpBoostVelocityModifier() - this.velocity.y, 0.0)

        if (this.isSprinting()) {
            val f: Float = this.yaw.toRadians()

            this.velocity += Vec3d((-MathHelper.sin(f) * 0.2f).toDouble(), 0.0, (MathHelper.cos(f) * 0.2f).toDouble())
        }

//        this.velocityDirty = true
    }

    private fun isSprinting(): Boolean = this.sprinting

    private fun getJumpBoostVelocityModifier(): Float = 0.0f

    private fun getJumpVelocity(): Float = 0.42f

    private fun swimUpward(water: TagKey<Fluid>?) {
        // TODO: Not yet implemented
    }

    private fun getSwimHeight(): Double = 0.0
    private fun isTouchingWater(): Boolean = false
    private fun isInLava(): Boolean = false
    private fun getFluidHeight(tags: TagKey<Fluid>): Double = 0.0

    private fun getRotationVector() = getRotationVector(this.pitch, this.yaw)

    fun getVelocityAffectingPos() = BlockPos.ofFloored(this.pos.x, this.player.box.minY - 0.5000001, this.pos.z)

    private fun getRotationVector(pitch: Float, yaw: Float): Vec3d {
        val f = pitch * (Math.PI.toFloat() / 180)
        val g = -yaw * (Math.PI.toFloat() / 180)

        val h = MathHelper.cos(g)
        val i = MathHelper.sin(g)
        val j = MathHelper.cos(f)
        val k = MathHelper.sin(f)

        return Vec3d((i * j).toDouble(), (-k).toDouble(), (h * j).toDouble())
    }

    private fun hasStatusEffect(effect: StatusEffect): Boolean {
        val instance = player.getStatusEffect(effect) ?: return false

        return instance.duration >= this.simulatedTicks
    }

    class SimulatedPlayerInput(
        directionalInput: DirectionalInput,
        jumping: Boolean,
        var sprinting: Boolean
    ) : Input() {
        var slowDown: Boolean = false

        init {
            this.pressingForward = directionalInput.forwards
            this.pressingBack = directionalInput.backwards
            this.pressingLeft = directionalInput.left
            this.pressingRight = directionalInput.right
            this.jumping = jumping
        }

        fun update() {
            if (this.pressingForward != this.pressingBack) {
                this.movementForward = if (this.pressingForward) 1.0f else -1.0f
            } else {
                this.movementForward = 0.0f
            }

            movementSideways = if (pressingLeft == pressingRight) 0.0f else if (pressingLeft) 1.0f else -1.0f

            if (slowDown) {
                movementSideways = (movementSideways.toDouble() * 0.3).toFloat()
                movementForward = (movementForward.toDouble() * 0.3).toFloat()
            }
        }

        override fun toString(): String {
            return "SimulatedPlayerInput(forwards={${this.pressingForward}}, backwards={${this.pressingBack}}, left={${this.pressingLeft}}, right={${this.pressingRight}}, jumping={${this.jumping}}, sprinting=$sprinting, slowDown=$slowDown)"
        }

        companion object {
            private const val MAX_WALKING_SPEED = 0.121

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
                    sprinting
                ).apply { this.slowDown = entity.isSneaking }
            }
        }

    }
}
