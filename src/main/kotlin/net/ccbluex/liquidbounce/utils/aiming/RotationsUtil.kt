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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerVelocityStrafe
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.fakelag.FakeLag
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleBacktrack
import net.ccbluex.liquidbounce.utils.aiming.angleSmooth.*
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.RequestHandler
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Configurable to configure the dynamic rotation engine
 */
open class RotationsConfigurable(
    owner: Listenable,
    fixVelocity: Boolean = true,
    changeLook: Boolean = false,
    combatSpecific: Boolean = false
) : Configurable("Rotations") {

    var angleSmooth = choices<AngleSmoothMode>(owner, "AngleSmooth", { it.choices[0] }, {
        arrayOf(
            LinearAngleSmoothMode(it),
            BezierAngleSmoothMode(it),
            SigmoidAngleSmoothMode(it),
            ConditionalLinearAngleSmoothMode(it)
        )
    })

    private var attention = Attention(owner).takeIf { combatSpecific }?.also { tree(it) }

    var fixVelocity by boolean("FixVelocity", fixVelocity)
    val resetThreshold by float("ResetThreshold", 2f, 1f..180f)
    private val ticksUntilReset by int("TicksUntilReset", 5, 1..30, "ticks")
    private val changeLook by boolean("ChangeLook", changeLook)

    fun toAimPlan(rotation: Rotation, vec: Vec3d? = null, entity: Entity? = null,
                  considerInventory: Boolean = false) = AimPlan(
        rotation,
        vec,
        entity,
        angleSmooth.activeChoice,
        attention,
        ticksUntilReset,
        resetThreshold,
        considerInventory,
        fixVelocity,
        changeLook,
    )

    fun toAimPlan(rotation: Rotation, vec: Vec3d? = null, entity: Entity? = null,
                  considerInventory: Boolean = false, changeLook: Boolean) =
        AimPlan(
            rotation,
            vec,
            entity,
            angleSmooth.activeChoice,
            attention,
            ticksUntilReset,
            resetThreshold,
            considerInventory,
            fixVelocity,
            changeLook
        )

    /**
     * How long it takes to rotate to a rotation in ticks
     *
     * Calculates the difference from the server rotation to the target rotation and divides it by the
     * minimum turn speed (to make sure we are always there in time)
     *
     * @param rotation The rotation to rotate to
     * @return The amount of ticks it takes to rotate to the rotation
     */
    fun howLongToReach(rotation: Rotation) = angleSmooth.activeChoice
        .howLongToReach(RotationManager.actualServerRotation, rotation)

}

/**
 * A rotation manager
 */
object RotationManager : Listenable {

    private var previousAimPlan: AimPlan? = null

    /**
     * Our final target rotation. This rotation is only used to define our current rotation.
     */
    private val aimPlan
        get() = aimPlanHandler.getActiveRequestValue()

    private var aimPlanHandler = RequestHandler<AimPlan>()

    /**
     * The rotation we want to aim at. This DOES NOT mean that the server already received this rotation.
     */
    var currentRotation: Rotation? = null
        set(value) {
            previousRotation = field ?: mc.player?.rotation ?: Rotation.ZERO

            field = value
        }

    // Used for rotation interpolation
    var previousRotation: Rotation? = null

    private val fakeLagging
        get() = FakeLag.isLagging || ModuleBacktrack.isLagging()

    val serverRotation: Rotation
        get() = if (fakeLagging) theoreticalServerRotation else actualServerRotation

    /**
     * The rotation that was already sent to the server and is currently active.
     * The value is not being written by the packets, but we gather the Rotation from the last yaw and pitch variables
     * from our player instance handled by the sendMovementPackets() function.
     */
    var actualServerRotation = Rotation.ZERO
        private set

    private var theoreticalServerRotation = Rotation.ZERO

    val storedAimPlan: AimPlan?
        get() = aimPlan ?: previousAimPlan

    /**
     * Inverts yaw (-180 to 180)
     */
    fun invertYaw(yaw: Float): Float {
        return (yaw + 180) % 360
    }

    fun aimAt(
        vecRotation: VecRotation,
        entity: Entity? = null,
        considerInventory: Boolean = true,
        configurable: RotationsConfigurable,
        priority: Priority,
        provider: Module
    ) {
        val (rotation, vec) = vecRotation
        aimAt(configurable.toAimPlan(rotation, vec, entity, considerInventory = considerInventory), priority, provider)
    }

    fun aimAt(
        rotation: Rotation,
        considerInventory: Boolean = true,
        configurable: RotationsConfigurable,
        priority: Priority,
        provider: Module
    ) {
        aimAt(configurable.toAimPlan(rotation, considerInventory = considerInventory), priority, provider)
    }

    fun aimAt(plan: AimPlan, priority: Priority, provider: Module) {
        if (!allowedToUpdate()) {
            return
        }

        aimPlanHandler.request(
            RequestHandler.Request(
                if (plan.changeLook) 1 else plan.ticksUntilReset,
                priority.priority,
                provider,
                plan
            )
        )
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
     * Update current rotation to a new rotation step
     */
    @Suppress("CognitiveComplexMethod", "NestedBlockDepth")
    fun update() {
        val player = mc.player ?: return
        val aimPlan = aimPlan
        val storedAimPlan = this.storedAimPlan ?: return

        val playerRotation = player.rotation

        if (aimPlan == null) {
            val differenceFromCurrentToPlayer = rotationDifference(serverRotation, playerRotation)

            if (differenceFromCurrentToPlayer < storedAimPlan.resetThreshold || storedAimPlan.changeLook) {
                currentRotation?.let { (yaw, _) ->
                    player.let { player ->
                        player.yaw = yaw + angleDifference(player.yaw, yaw)
                        player.renderYaw = player.yaw
                        player.lastRenderYaw = player.yaw
                    }
                }
                currentRotation = null
                previousAimPlan = null
                return
            }
        } else {
            if (aimPlan.entity != null && aimPlan.entity != previousAimPlan?.entity) {
                aimPlan.attention?.onNewTarget()
            }
        }

        // Prevents any rotation changes when inventory is opened
        val allowedRotation = ((!InventoryManager.isInventoryOpenServerSide &&
            mc.currentScreen !is GenericContainerScreen) || !storedAimPlan.considerInventory) && allowedToUpdate()

        if (allowedRotation) {
            storedAimPlan.nextRotation(currentRotation ?: playerRotation, aimPlan == null)
                    .fixedSensitivity().let {
                currentRotation = it
                previousAimPlan = storedAimPlan

                if (storedAimPlan.changeLook) {
                    player.applyRotation(it)
                }
            }
        }
        // Update reset ticks
        aimPlanHandler.tick()
    }

    /**
     * Checks if it should update the server-side rotations
     */
    private fun allowedToUpdate() = !CombatManager.shouldPauseRotation()

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
        hypot(abs(angleDifference(a.yaw, b.yaw).toDouble()), abs((a.pitch - b.pitch).toDouble()))

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
    fun angleDifference(a: Float, b: Float) = MathHelper.wrapDegrees(a - b)

    @Suppress("unused")
    val velocityHandler = handler<PlayerVelocityStrafe> { event ->
        if (storedAimPlan?.applyVelocityFix == true) {
            event.velocity = fixVelocity(event.velocity, event.movementInput, event.speed)
        }
    }

    /**
     * Updates at movement tick, so we can update the rotation before the movement runs and the client sends the packet
     * to the server.
     */
    val tickHandler = handler<MovementInputEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        val input = SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(event.directionalInput)

        input.sneaking = event.sneaking
        input.jumping = event.jumping

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(input)
        simulatedPlayer.tick()

        val oldPos = player.pos
        player.setPosition(simulatedPlayer.pos)

        EventManager.callEvent(SimulatedTickEvent(event, simulatedPlayer))
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
        } else if (packet is PlayerInteractItemC2SPacket) {
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
