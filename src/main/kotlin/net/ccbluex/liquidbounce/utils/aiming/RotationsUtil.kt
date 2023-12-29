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

package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerVelocityStrafe
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleBacktrack
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleBadWifi
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.item.InventoryTracker
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Configurable to configure the dynamic rotation engine
 */
class RotationsConfigurable(
    turnSpeed: ClosedFloatingPointRange<Float> = 180f..180f,
) : Configurable("Rotations") {

    val turnSpeed by floatRange("TurnSpeed", turnSpeed, 0f..180f)
    val smoothMode by enumChoice("SmoothMode", SmootherMode.RELATIVE, SmootherMode.values())
    var fixVelocity by boolean("FixVelocity", true)
    val resetThreshold by float("ResetThreshold", 2f, 1f..180f)
    val ticksUntilReset by int("TicksUntilReset", 5, 1..30)
    val silent by boolean("Silent", true)

    fun toAimPlan(rotation: Rotation, considerInventory: Boolean = false) =
        AimPlan(
            rotation,
            smoothMode,
            turnSpeed,
            ticksUntilReset,
            resetThreshold,
            considerInventory,
            fixVelocity,
            !silent
        )

    fun toAimPlan(rotation: Rotation, considerInventory: Boolean = false, silent: Boolean) =
        AimPlan(
            rotation,
            smoothMode,
            turnSpeed,
            ticksUntilReset,
            resetThreshold,
            considerInventory,
            fixVelocity,
            !silent
        )

}

/**
 * A rotation manager
 */
object RotationManager : Listenable {

    /**
     * Our final target rotation. This rotation is only used to define our current rotation.
     */
    var aimPlan: AimPlan? = null

    /**
     * The rotation we want to aim at. This DOES NOT mean that the server already received this rotation.
     */
    var currentRotation: Rotation? = null
        set(value) {
            previousRotation = field ?: mc.player.rotation

            field = value
        }

    // Used for rotation interpolation
    var previousRotation: Rotation? = null

    private val fakeLagging
        get() = ModuleBadWifi.isLagging() || ModuleBacktrack.isLagging() || ModuleBlink.isLagging()

    val serverRotation: Rotation
        get() = if (fakeLagging) theoreticalServerRotation else actualServerRotation

    /**
     * The rotation that was already sent to the server and is currently active.
     * The value is not being written by the packets, but we gather the Rotation from the last yaw and pitch variables
     * from our player instance handled by the sendMovementPackets() function.
     */
    var actualServerRotation = Rotation.ZERO
        private set

    var theoreticalServerRotation = Rotation.ZERO
        private set

    fun aimAt(rotation: Rotation, considerInventory: Boolean = true, configurable: RotationsConfigurable) =
        aimAt(configurable.toAimPlan(rotation, considerInventory))

    fun aimAt(plan: AimPlan) {
        if (!allowedToUpdate()) {
            return
        }

        aimPlan = plan
    }

    fun makeRotation(vec: Vec3d, eyes: Vec3d): Rotation {
        val diffX = vec.x - eyes.x
        val diffY = vec.y - eyes.y
        val diffZ = vec.z - eyes.z

        return Rotation(
            MathHelper.wrapDegrees(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
            MathHelper.wrapDegrees((-Math.toDegrees(atan2(diffY, sqrt(diffX * diffX + diffZ * diffZ)))).toFloat())
        )
    }

    val gcd: Double
        get() {
            val f = mc.options.mouseSensitivity.value * 0.6F.toDouble() + 0.2F.toDouble()
            return f * f * f * 8.0 * 0.15F
        }

    /**
     * Update current rotation to new rotation step
     */
    fun update() {
        val player = mc.player ?: return
        val aimPlan = aimPlan ?: return

        // Prevents any rotation changes when inventory is opened
        val allowedRotation = ((!InventoryTracker.isInventoryOpenServerSide &&
            mc.currentScreen !is GenericContainerScreen) || !aimPlan.considerInventory) && allowedToUpdate()

        val playerRotation = player.rotation

        if (aimPlan.ticksLeft == 0) {
            val differenceFromCurrentToPlayer = rotationDifference(currentRotation ?: serverRotation, playerRotation)

            if (differenceFromCurrentToPlayer < aimPlan.resetThreshold || aimPlan.applyClientSide) {
                this.aimPlan = null

                currentRotation?.let { rotation ->
                    player.let { player ->
                        player.yaw = rotation.yaw + angleDifference(player.yaw, rotation.yaw)
                        player.renderYaw = player.yaw
                        player.lastRenderYaw = player.yaw
                    }
                }
                currentRotation = null
                return
            }
        }

        if (allowedRotation) {
            aimPlan.nextRotation(currentRotation ?: playerRotation).fixedSensitivity().let {
                currentRotation = it

                if (aimPlan.applyClientSide) {
                    player.applyRotation(it)
                }
            }
        }

        // Update reset ticks
        if (aimPlan.ticksLeft > 0) {
            aimPlan.ticksLeft--
        }
    }

    /**
     * Checks if it should update the server-side rotations
     */
    private fun allowedToUpdate() =
        !CombatManager.shouldPauseRotation()

    fun rotationMatchesPreviousRotation(): Boolean {
        val player = mc.player ?: return false

        currentRotation?.let {
            return it == previousRotation
        }

        return player.rotation == player.lastRotation
    }

    /**
     * Calculate difference between two rotations
     */
    fun rotationDifference(a: Rotation, b: Rotation) =
        hypot(angleDifference(a.yaw, b.yaw).toDouble(), (a.pitch - b.pitch).toDouble())

    /**
     * Calculate difference between an entity and your rotation
     */
    fun rotationDifference(entity: Entity): Double {
        val player = mc.player ?: return 0.0
        val eyes = player.eyes

        return rotationDifference(makeRotation(entity.box.center, eyes), player.rotation).coerceAtMost(180.0)
    }

    /**
     * Calculate difference between two angle points
     */
    fun angleDifference(a: Float, b: Float) =
        MathHelper.wrapDegrees(a - b)

    val velocityHandler = handler<PlayerVelocityStrafe> { event ->
        if (aimPlan?.applyVelocityFix == true) {
            event.velocity = fixVelocity(event.velocity, event.movementInput, event.speed)
        }
    }

    /**
     * Updates at movement tick, so we can update the rotation before the movement runs and the client sends the packet
     * to the server.
     */
    val tickHandler = handler<MovementInputEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        val player = mc.player ?: return@handler

        val input = SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(event.directionalInput)

        input.sneaking = event.sneaking
        input.jumping = event.jumping

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(input)

        simulatedPlayer.tick()

        val oldPos = player.pos
        player.setPosition(simulatedPlayer.pos)

        EventManager.callEvent(SimulatedTickEvent())
        update()

        player.setPosition(oldPos)
    }

    /**
     * Track rotation changes
     *
     * We cannot only rely on player.lastYaw and player.lastPitch because
     * sometimes we update the rotation off chain (e.g. on interactItem)
     * and the player.lastYaw and player.lastPitch are not updated.
     */
    val packetHandler = handler<PacketEvent>(priority = -1000) {
        val packet = it.packet

        val rotation = if (packet is PlayerMoveC2SPacket && packet.changeLook) {
             Rotation(packet.yaw, packet.pitch)
        } else if (packet is PlayerPositionLookS2CPacket) {
            Rotation(packet.yaw, packet.pitch)
        } else {
            return@handler
        }

        // This normally applies to Modules like Blink, BadWifi, etc.
        if (!it.isCancelled) {
            actualServerRotation = rotation
        }

        theoreticalServerRotation = rotation
    }

    /**
     * Fix velocity
     */
    private fun fixVelocity(currVelocity: Vec3d, movementInput: Vec3d, speed: Float): Vec3d {
        currentRotation?.let { rotation ->
            val yaw = rotation.yaw
            val d = movementInput.lengthSquared()

            return if (d < 1.0E-7) {
                Vec3d.ZERO
            } else {
                val vec3d = (if (d > 1.0) movementInput.normalize() else movementInput).multiply(speed.toDouble())

                val f = MathHelper.sin(yaw * 0.017453292f)
                val g = MathHelper.cos(yaw * 0.017453292f)

                Vec3d(
                    vec3d.x * g.toDouble() - vec3d.z * f.toDouble(),
                    vec3d.y,
                    vec3d.z * g.toDouble() + vec3d.x * f.toDouble()
                )
            }
        }

        return currVelocity
    }

}

class LeastDifferencePreference(
    private val baseRotation: Rotation,
    private val basePoint: Vec3d? = null,
) : RotationPreference {
    override fun getPreferredSpot(eyesPos: Vec3d, range: Double): Vec3d {
        if (this.basePoint != null) {
            return this.basePoint
        }

        return eyesPos + this.baseRotation.rotationVec * range
    }

    override fun compare(o1: Rotation, o2: Rotation): Int {
        val rotationDifferenceO1 = RotationManager.rotationDifference(baseRotation, o1)
        val rotationDifferenceO2 = RotationManager.rotationDifference(baseRotation, o2)

        return rotationDifferenceO1.compareTo(rotationDifferenceO2)
    }

    companion object {
        val LEAST_DISTANCE_TO_CURRENT_ROTATION: LeastDifferencePreference
            get() = LeastDifferencePreference(RotationManager.actualServerRotation)

        fun leastDifferenceToLastPoint(eyes: Vec3d, point: Vec3d): LeastDifferencePreference {
            return LeastDifferencePreference(RotationManager.makeRotation(vec = point, eyes = eyes), point)
        }
    }

}
