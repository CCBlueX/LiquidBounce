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
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import kotlin.math.*

/**
 * GoldSrc/HL1 movement.
 *
 * Implements Half‑Life 1 movement 1:1 inspired by the goldsqource mod:
 * - Full horizontal control via HL velocity buffer (cancels vanilla input accel).
 * - Ground friction + ground acceleration (HL1 stopSpeed/accel/friction).
 * - Air acceleration with multi‑step yaw interpolation (classic A/D bhops).
 * - HL water movement: acceleration + friction with simple buoyancy damping.
 * - HL ladders: normal‑based climbing, into‑ladder pull, and jump‑off impulse.
 * - Ledgegrab/glidestep: downward collision probe to stick to ground.
 * - Optional buffered jump (matches goldsqource toggle) for consistent hop chaining.
 */
object ModuleGoldsrcMovement : ClientModule("GoldSrcMovement", Category.FUN) {

    // Unit conversions and constants (ported from goldsqource)
    private const val TO_QUAKE = 40.0     // convert MC per-tick displacement to ups/s
    private const val FROM_QUAKE = 1.0 / 40.0
    private const val TICKRATE = 20.0
    private const val FRAMETIME = 1.0 / TICKRATE
    private const val FAKE_FRAMETIME = 1.0 / 100.0 // simulate ~100 tickrate in airaccel
    private const val MAX_CLIMB_SPEED = 200.0
    private const val CLIMB_JUMPOFF_SPEED = 270.0

    // Speed scaling to calibrate MC base move speed (~0.1) to ~270 ups HL1 feel
    private const val QUAKE_MOVEMENT_SPEED_MULTIPLIER = 3.375
    private const val QUAKE_SNEAKING_SPEED_MULTIPLIER = 0.65

    // Config
    private val friction by float("Friction", 4.0f, 0.0f..10.0f)
    private val stopSpeed by float("StopSpeed", 75.0f, 0.0f..200.0f, "ups")
    private val acceleration by float("Acceleration", 10.0f, 0.0f..20.0f)
    private val airAcceleration by float("AirAcceleration", 10.0f, 0.0f..20.0f)
    private val maxAAccPerTick by float("MaxAirAccelPerTick", 0.0f, 0.0f..400.0f, "ups")
    private val hardCapSpeed by float("HardCapSpeed", 0.0f, 0.0f..1000.0f, "ups")

    private val waterAcceleration by float("WaterAcceleration", 10.0f, 0.0f..20.0f)
    private val waterFriction by float("WaterFriction", 1.0f, 0.0f..5.0f)
    private val waterSpeedScale by float("WaterSpeedScale", 0.5f, 0.0f..2.0f)

    // HL1-quality of life: if enabled, holding jump will buffer so you can chain hops more consistently
    private val bufferedJump by boolean("BufferedJump", false)

    // State
    private var previousYaw: Float = 0.0f
    private var jumping: Boolean = false
    private var jumped: Boolean = false
    private var baseVelocities = mutableListOf<Pair<Double, Double>>()
    private var hlVelX: Double = 0.0
    private var hlVelZ: Double = 0.0

    private fun getBaseSpeedCurrent(): Double {
        var result = player.movementSpeed.toDouble()
        result *= if (!player.isSneaking && !player.isInSneakingPose && !player.isInSwimmingPose) {
            QUAKE_MOVEMENT_SPEED_MULTIPLIER
        } else {
            QUAKE_SNEAKING_SPEED_MULTIPLIER
        }

        if (player.isUsingItem) result *= 0.2
        return result
    }

    private fun getBaseSpeedMax(): Double {
        return player.movementSpeed.toDouble() * QUAKE_MOVEMENT_SPEED_MULTIPLIER
    }

    private fun horizontalSpeed(): Double {
        return sqrt(hlVelX * hlVelX + hlVelZ * hlVelZ)
    }

    private fun movementDirection(sidemove: Double, forwardmove: Double): Pair<Double, Double> {
        val preSpeed = sidemove * sidemove + forwardmove * forwardmove
        if (preSpeed <= 0.0) return 0.0 to 0.0

        var speed = sqrt(preSpeed)
        speed = if (speed <= 0.0) 1.0 else 1.0 / speed

        val sm = sidemove * speed
        val fm = forwardmove * speed
        val f1 = sin(player.yaw * Math.PI / 180.0)
        val f2 = cos(player.yaw * Math.PI / 180.0)
        val wishX = sm * f2 - fm * f1
        val wishZ = fm * f2 + sm * f1
        return wishX to wishZ
    }

    private fun applyFriction() {
        val speed = horizontalSpeed()
        if (speed <= 0.0) return

        val speedUps = speed * TO_QUAKE * TICKRATE
        val control = if (speedUps < stopSpeed.toDouble()) stopSpeed.toDouble() else speedUps
        val dropUps = control * friction.toDouble() * FRAMETIME
        val newSpeedUps = max(0.0, speedUps - dropUps)
        if (newSpeedUps != speedUps) {
            val ratio = newSpeedUps / speedUps
            hlVelX *= ratio
            hlVelZ *= ratio
        }
    }

    private fun accelerate(wishspeed: Double, wishX: Double, wishZ: Double, accel: Double) {
        val currentSpeed = hlVelX * wishX + hlVelZ * wishZ
        val addSpeed = wishspeed - currentSpeed
        if (addSpeed <= 0.0) return

        var accelSpeed = accel * wishspeed * FRAMETIME
        if (accelSpeed > addSpeed) accelSpeed = addSpeed

        hlVelX += accelSpeed * wishX
        hlVelZ += accelSpeed * wishZ
    }

    private fun airAccelerate(wishspeedInitial: Double, wishX: Double, wishZ: Double, accel: Double) {
        val maxAirAccelerationUps = maxAAccPerTick.toDouble()
        val wishspeedUps = wishspeedInitial * TO_QUAKE * TICKRATE
        val wishspeed = if (maxAirAccelerationUps > 0.0) min(wishspeedUps, maxAirAccelerationUps) else wishspeedUps

        val currentSpeed = (hlVelX * TO_QUAKE * TICKRATE) * wishX + (hlVelZ * TO_QUAKE * TICKRATE) * wishZ
        val addSpeed = wishspeed - currentSpeed
        if (addSpeed <= 0.0) return

        var accelSpeed = accel * wishspeed * FAKE_FRAMETIME
        if (accelSpeed > addSpeed) accelSpeed = addSpeed

        val newUpsX = (hlVelX * TO_QUAKE * TICKRATE) + accelSpeed * wishX
        val newUpsZ = (hlVelZ * TO_QUAKE * TICKRATE) + accelSpeed * wishZ
        hlVelX = newUpsX * FROM_QUAKE * FRAMETIME
        hlVelZ = newUpsZ * FROM_QUAKE * FRAMETIME
    }

    private fun waterAccelerate(wishspeed: Double, wishX: Double, wishZ: Double, accel: Double) {
        val currentSpeed = hlVelX * wishX + hlVelZ * wishZ
        val addSpeed = wishspeed - currentSpeed
        if (addSpeed <= 0.0) return

        var accelSpeed = accel * wishspeed * FRAMETIME
        if (accelSpeed > addSpeed) accelSpeed = addSpeed

        hlVelX += accelSpeed * wishX
        hlVelZ += accelSpeed * wishZ
    }

    private fun applyWaterFriction() {
        val speed = horizontalSpeed()
        if (speed <= 0.0) return
        val drop = speed * waterFriction.toDouble() * FRAMETIME
        val newSpeed = max(0.0, speed - drop)
        if (newSpeed != speed) {
            val ratio = newSpeed / speed
            hlVelX *= ratio
            hlVelZ *= ratio
        }
    }

    private fun applyHardCap() {
        val hardCap = hardCapSpeed.toDouble() * FROM_QUAKE * FRAMETIME
        val speed = horizontalSpeed()
        if (hardCap != 0.0 && speed > hardCap) {
            val m = hardCap / speed
            hlVelX *= m
            hlVelZ *= m
        }
    }

    // Clear base velocities each tick like goldsqource
    private val tickHandler = handler<GameTickEvent> {
        if (!inGame) return@handler
        if (baseVelocities.isNotEmpty()) baseVelocities.clear()
        // Replicate buffered jump handling from goldsqource client
        jumping = player.input.playerInput.jump
        if (bufferedJump) {
            if (!jumping) {
                jumped = false
            } else if (jumped) {
                // this is so that you don't airaccelerate on the ground
                jumping = false
            }
        } else {
            // Vanilla HL1 behavior (no explicit buffering): don't gate jump state
            jumped = false
        }
    }

    // Track jump completion
    private val afterJumpHandler = handler<PlayerAfterJumpEvent> { _ -> jumped = true }

    // Collect base velocities from updateVelocity hook
    private val velocityStrafeHandler = handler<PlayerVelocityStrafe> { event ->
        if (!running) return@handler
        if (player.abilities.flying || player.hasVehicle()) return@handler
        // Cancel vanilla input-based acceleration so HL logic fully controls horizontal velocity
        event.velocity = Vec3d(0.0, event.velocity.y, 0.0)

        if (player.isTouchingWater || player.isInLava || player.isClimbing) return@handler
        val sidemove = event.movementInput.x
        val forwardmove = event.movementInput.z
        val wishdir = movementDirection(sidemove, forwardmove)
        val wishspeed = event.speed.toDouble() * 2.15
        baseVelocities.add(wishdir.first * wishspeed to wishdir.second * wishspeed)

        // Seed HL velocity state from current player velocity when starting movement collection
        if (hlVelX == 0.0 && hlVelZ == 0.0) {
            hlVelX = player.velocity.x
            hlVelZ = player.velocity.z
        }
    }

    // Accurately apply GoldSrc movement inside the movement step
    private val moveHandler = handler<PlayerMoveEvent>(priority = EventPriorityConvention.MODEL_STATE) { event ->
        if (!running || event.type != net.minecraft.entity.MovementType.SELF) return@handler
        if (player.abilities.flying || player.isGliding || player.hasVehicle()) return@handler

        val sidemove = player.input.movementSideways.toDouble()
        val forwardmove = player.input.movementForward.toDouble()
        val wishdir = movementDirection(sidemove, forwardmove)
        val wishspeed = if (sidemove != 0.0 || forwardmove != 0.0) getBaseSpeedCurrent() else 0.0

        // Water movement
        if (player.isTouchingWater && !player.abilities.flying) {
            val waterWish = if (wishspeed > 0.0) getBaseSpeedCurrent() * waterSpeedScale.toDouble() else 0.0
            waterAccelerate(waterWish, wishdir.first, wishdir.second, waterAcceleration.toDouble())
            applyWaterFriction()
            // Simple vertical damping to mimic buoyancy
            val yNew = player.velocity.y * 0.8 - 0.005
            player.velocity = Vec3d(hlVelX, yNew, hlVelZ)
            event.movement = player.velocity
            previousYaw = player.yaw
            return@handler
        }

        // Ladder movement (HL1-like)
        if (player.isClimbing) {
            val climbingPos = player.climbingPos.orElse(null)
            val blockState = player.world.getBlockState(climbingPos)
            if (!blockState.isAir && blockState.block is net.minecraft.block.LadderBlock) {
                val facing = blockState.get(net.minecraft.block.LadderBlock.FACING)
                val ladderNormal = Vec3d(
                    facing.offsetX.toDouble(),
                    facing.offsetY.toDouble(),
                    facing.offsetZ.toDouble()
                )

                var speed = 200.0 * FROM_QUAKE * FRAMETIME
                speed = min(speed, getBaseSpeedMax())

                if (jumping) {
                    val jumpOff = Vec3d(
                        ladderNormal.x * CLIMB_JUMPOFF_SPEED * FROM_QUAKE * FRAMETIME,
                        ladderNormal.y * CLIMB_JUMPOFF_SPEED * FROM_QUAKE * FRAMETIME,
                        ladderNormal.z * CLIMB_JUMPOFF_SPEED * FROM_QUAKE * FRAMETIME
                    )
                    hlVelX = jumpOff.x
                    hlVelZ = jumpOff.z
                    player.velocity = jumpOff
                    event.movement = jumpOff
                } else {
                    if (forwardmove != 0.0 || sidemove != 0.0) {
                        val viewYaw = player.yaw.toDouble()
                        val viewPitch = player.pitch.toDouble()
                        val vecForwardLeft = anglesToVectors(viewPitch, viewYaw)
                        var velocity = vecForwardLeft.first.multiply(forwardmove * speed)
                        velocity = velocity.add(vecForwardLeft.second.multiply(sidemove * speed))

                        var tmp = Vec3d(0.0, 1.0, 0.0)
                        var perp = tmp.crossProduct(ladderNormal).normalize()
                        val normal = velocity.dotProduct(ladderNormal)
                        val cross = ladderNormal.multiply(normal)
                        var lateral = velocity.subtract(cross)
                        tmp = ladderNormal.crossProduct(perp)
                        val wishdirVec = Vec3d(wishdir.first, 0.0, wishdir.second)
                        val movingAwayFromLadder = ladderNormal.dotProduct(wishdirVec) > 0
                        if (!player.isOnGround || !movingAwayFromLadder) {
                            lateral = lateral.add(ladderNormal.multiply(-MAX_CLIMB_SPEED * FROM_QUAKE * FRAMETIME))
                        }
                        val ladderMove = lateral.add(tmp.multiply(-normal))
                        hlVelX = ladderMove.x
                        hlVelZ = ladderMove.z
                        if (player.isOnGround && movingAwayFromLadder) {
                            val add = ladderNormal.multiply(MAX_CLIMB_SPEED * FROM_QUAKE * FRAMETIME)
                            hlVelX += add.x
                            hlVelZ += add.z
                        }
                        player.velocity = Vec3d(hlVelX, ladderMove.y, hlVelZ)
                        event.movement = player.velocity
                    } else {
                        hlVelX = 0.0
                        hlVelZ = 0.0
                        player.velocity = Vec3d.ZERO
                        event.movement = player.velocity
                    }
                }
                previousYaw = player.yaw
                return@handler
            }
        }

        // Ground vs Air
        val onGroundForReal = player.isOnGround && !jumping
        if (onGroundForReal) {
            applyFriction()
            if (wishspeed != 0.0) {
                accelerate(wishspeed, wishdir.first, wishdir.second, acceleration.toDouble())
            }

            if (baseVelocities.isNotEmpty()) {
                var x = hlVelX
                var z = hlVelZ
                val speedMod = if (getBaseSpeedMax() != 0.0) wishspeed / getBaseSpeedMax() else 0.0
                for (base in baseVelocities) {
                    x += base.first * speedMod
                    z += base.second * speedMod
                }
                hlVelX = x
                hlVelZ = z
            }
        } else {
            val realYaw = player.yaw
            var savedYaw = player.yaw
            val d = savedYaw - previousYaw
            if (d > 180f) savedYaw -= 360f else if (d < -180f) savedYaw += 360f
            for (i in 1..5) {
                val t = i / 5.0
                player.yaw = lerp(previousYaw.toDouble(), savedYaw.toDouble(), t).toFloat()
                val wishdirAir = movementDirection(sidemove, forwardmove)
                airAccelerate(wishspeed, wishdirAir.first, wishdirAir.second, airAcceleration.toDouble())
            }
            player.yaw = realYaw
        }

        applyHardCap()
        player.velocity = Vec3d(hlVelX, player.velocity.y, hlVelZ)
        event.movement = Vec3d(hlVelX, event.movement.y, hlVelZ)
        previousYaw = player.yaw
    }

    // We intentionally infer jumping state directly from input to match goldsqource timing

    // Ledgegrab/glidestep approximation using collision adjust hook
    private val stepSuccessHandler = handler<PlayerStepSuccessEvent> { event ->
        if (!running) return@handler
        if (player.isOnGround) return@handler
        if (player.isClimbing || player.isTouchingWater) return@handler
        // 200 ups threshold and non-negative vertical velocity
        if (player.velocity.y * TICKRATE * TO_QUAKE >= 200.0 || player.velocity.y < 0.0) return@handler

        val list = player.world.getEntityCollisions(null, player.boundingBox.stretch(event.movementVec))
        val down = -(4.0 * FROM_QUAKE)
        val downAdjust =
            Entity.adjustMovementForCollisions(null, Vec3d(0.0, down, 0.0), player.boundingBox, player.world, list)
        if (downAdjust.y > down) {
            event.adjustedVec = Vec3d(event.adjustedVec.x, event.adjustedVec.y + downAdjust.y, event.adjustedVec.z)
            player.velocity = Vec3d(player.velocity.x, 0.0, player.velocity.z)
        }
    }

    private fun lerp(a: Double, b: Double, t: Double): Double = (1.0 - t) * a + b * t

    // View vectors
    private fun anglesToVectors(pitch: Double, yaw: Double): Pair<Vec3d, Vec3d> {
        val radPitch = Math.toRadians(pitch)
        val radYaw = Math.toRadians(yaw)
        val cosPitch = cos(radPitch)
        val sinPitch = sin(radPitch)
        val cosYaw = cos(radYaw)
        val sinYaw = sin(radYaw)
        val forwards = Vec3d(cosPitch * -sinYaw, -sinPitch, cosPitch * cosYaw)
        val left = Vec3d(cosYaw, 0.0, sinYaw)
        return forwards to left
    }
}
