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
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerVelocityStrafe
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.item.InventoryTracker
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.entity.Entity
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import org.apache.commons.lang3.RandomUtils
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Configurable to configure the dynamic rotation engine
 */
class RotationsConfigurable : Configurable("Rotations") {
    val turnSpeed by floatRange("TurnSpeed", 40f..60f, 0f..180f)
    val fixVelocity by boolean("FixVelocity", true)
    val resetThreshold by float("ResetThreshold", 2f, 1f..180f)
    val ticksUntilReset by int("TicksUntilReset", 5, 1..30)
    val silent by boolean("Silent", true)
}

/**
 * A rotation manager
 */
object RotationManager : Listenable {

    var targetRotation: Rotation? = null
    val serverRotation: Rotation
        get() = Rotation(mc.player?.lastYaw ?: 0f, mc.player?.lastPitch ?: 0f)

    // Current rotation
    var currentRotation: Rotation? = null
        set(value) {
            previousRotation = field ?: mc.player.rotation

            field = value
        }

    var ticksUntilReset: Int = 0
    var ignoreOpenInventory = false

    // Used for rotation interpolation
    var previousRotation: Rotation? = null

    // Active configurable
    var activeConfigurable: RotationsConfigurable? = null

    fun aimAt(vec: Vec3d, eyes: Vec3d, openInventory: Boolean = false, configurable: RotationsConfigurable) =
        aimAt(makeRotation(vec, eyes), openInventory, configurable)

    fun aimAt(rotation: Rotation, openInventory: Boolean = false, configurable: RotationsConfigurable) {
        if (!shouldUpdate()) {
            return
        }

        activeConfigurable = configurable
        targetRotation = rotation
        ticksUntilReset = configurable.ticksUntilReset
        ignoreOpenInventory = openInventory
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

        // Prevents any rotation changes, when inventory is opened
        val canRotate = (!InventoryTracker.isInventoryOpenServerSide &&
            mc.currentScreen !is GenericContainerScreen) || ignoreOpenInventory

        val configurable = activeConfigurable ?: return

        // Update rotations
        val speed = RandomUtils.nextFloat(
            configurable.turnSpeed.start,
            configurable.turnSpeed.endInclusive
        )

        val playerRotation = player.rotation

        if (ticksUntilReset == 0 || !shouldUpdate()) {
            if (rotationDifference(currentRotation ?: serverRotation, playerRotation) <
                configurable.resetThreshold || !configurable.silent) {
                ticksUntilReset = -1

                targetRotation = null
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

            if (canRotate) {
                limitAngleChange(currentRotation ?: serverRotation, playerRotation, speed).fixedSensitivity().let {
                    currentRotation = it

                    if (!configurable.silent) {
                        player.applyRotation(it)
                    }
                }
            }
            return
        }

        if (canRotate) {
            targetRotation?.let { targetRotation ->
                limitAngleChange(currentRotation ?: playerRotation, targetRotation, speed).fixedSensitivity().let {
                    currentRotation = it

                    if (!configurable.silent) {
                        player.applyRotation(it)
                    }
                }
            }
        }

        // Update reset ticks
        if (ticksUntilReset > 0) {
            ticksUntilReset--
        }
    }

    /**
     * Checks if it should update the server-side rotations
     */
    fun shouldUpdate() = !CombatManager.shouldPauseRotation()

    /**
     * Calculate difference between the server rotation and your rotation
     */
    fun rotationDifference(rotation: Rotation): Double {
        return rotationDifference(rotation, serverRotation)
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
     * Limit your rotations
     */
    fun limitAngleChange(currentRotation: Rotation, targetRotation: Rotation, speed: Float): Rotation {
        val yawDifference = angleDifference(targetRotation.yaw, currentRotation.yaw)
        val pitchDifference = angleDifference(targetRotation.pitch, currentRotation.pitch)

        val rotationDifference = hypot(yawDifference, pitchDifference)

        val straightLineYaw = abs(yawDifference / rotationDifference) * speed
        val straightLinePitch = abs(pitchDifference / rotationDifference) * speed

        return Rotation(
            currentRotation.yaw + yawDifference.coerceIn(-straightLineYaw, straightLineYaw),
            currentRotation.pitch + pitchDifference.coerceIn(-straightLinePitch, straightLinePitch)
        )
    }

    /**
     * Calculate difference between two angle points
     */
    fun angleDifference(a: Float, b: Float) = MathHelper.wrapDegrees(a - b)

    val velocityHandler = handler<PlayerVelocityStrafe> { event ->
        if (activeConfigurable?.fixVelocity == true) {
            event.velocity = fixVelocity(event.velocity, event.movementInput, event.speed)
        }
    }

    val tickHandler = handler<GameTickEvent> {
        if (targetRotation == null || mc.isPaused) {
            return@handler
        }

        update()
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
            get() = LeastDifferencePreference(RotationManager.currentRotation ?: mc.player.rotation)

        fun leastDifferenceToLastPoint(eyes: Vec3d, point: Vec3d): LeastDifferencePreference {
            return LeastDifferencePreference(RotationManager.makeRotation(vec = point, eyes = eyes), point)
        }
    }

}
