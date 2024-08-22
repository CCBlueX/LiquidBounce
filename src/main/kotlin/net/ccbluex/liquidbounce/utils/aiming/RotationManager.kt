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

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerVelocityStrafe
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.data.AngleLine
import net.ccbluex.liquidbounce.utils.aiming.tracking.RotationTracker
import net.ccbluex.liquidbounce.utils.aiming.utils.angleDifference
import net.ccbluex.liquidbounce.utils.aiming.utils.applyRotation
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.RequestHandler
import net.minecraft.entity.Entity
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

/**
 * A rotation manager
 */
object RotationManager : Listenable {

    private var rotationTrackerHandler = RequestHandler<RotationTracker>()

    private val rotationTracker
        get() = rotationTrackerHandler.getActiveRequestValue()
    private var previousRotationTracker: RotationTracker? = null

    val activeRotationTracker: RotationTracker?
        get() = rotationTracker ?: previousRotationTracker

    init {
        RotationObserver
    }

    fun aimAt(tracker: RotationTracker, priority: Priority, provider: Module) {
        if (!isAllowedToUpdate()) {
            return
        }

        rotationTrackerHandler.request(
            RequestHandler.Request(
                if (tracker.engine.movementCorrectionMode.changeLook) 1 else tracker.engine.ticksUntilReset,
                priority.priority,
                provider,
                tracker
            )
        )
    }

    /**
     * Update current rotation to a new rotation step
     */
    @Suppress("CognitiveComplexMethod", "NestedBlockDepth")
    fun update() {
        val tracker = rotationTracker
        val activeTracker = this.activeRotationTracker ?: return
        val playerOrientation = player.orientation

        if (tracker == null) {
            val differenceFromCurrentToPlayer = playerOrientation.differenceTo(RotationObserver.serverOrientation)

            // todo: implement a smart unhook
            if (differenceFromCurrentToPlayer < 2f || activeTracker.engine.movementCorrectionMode == RotationEngine.MovementCorrectionMode.CHANGE_LOOK) {
                RotationObserver.currentOrientation?.let { (yaw, _) ->
                    player.let { player ->
                        player.yaw = yaw + angleDifference(player.yaw, yaw)
                        player.renderYaw = player.yaw
                        player.lastRenderYaw = player.yaw
                    }
                }
                RotationObserver.currentOrientation = null
                previousRotationTracker = null
                return
            }
        } else {
            // todo: slow start implementation, which I should remove and replace with a timing observer
//            val enemyChange = tracker.entity != null && tracker.entity != previousRotationTracker?.entity &&
//                tracker.slowStart?.onEnemyChange == true
//            val triggerNoChange = triggerNoDifference && tracker.slowStart?.onZeroRotationDifference == true
//
//            if (triggerNoChange || enemyChange) {
//                tracker.slowStart?.onTrigger()
//            }
        }

        // Prevents any rotation changes when inventory is opened
//        val allowedRotation = ((!InventoryManager.isInventoryOpenServerSide &&
//            mc.currentScreen !is GenericContainerScreen) || !activeTracker.considerInventory) && isAllowedToUpdate()

        activeTracker.nextRotation(RotationObserver.currentOrientation ?: playerOrientation, tracker == null)
            .fixedSensitivity().let {
                RotationObserver.currentOrientation = it
                previousRotationTracker = activeTracker

                if (activeTracker.engine.movementCorrectionMode == RotationEngine.MovementCorrectionMode.CHANGE_LOOK) {
                    player.applyRotation(it)
                }
            }

//        if (allowedRotation) {

//        }
        // Update reset ticks
        rotationTrackerHandler.tick()
    }

    /**
     * Checks if it should update the server-side rotations
     */
    private fun isAllowedToUpdate() = !CombatManager.shouldPauseRotation()

    /**
     * Calculate difference between an entity and your rotation
     */
    fun rotationDifference(entity: Entity) = AngleLine(toPoint = entity.box.center)
        .differenceTo(player.orientation)

    @Suppress("unused")
    val velocityHandler = handler<PlayerVelocityStrafe> { event ->
        if (activeRotationTracker?.engine?.movementCorrectionMode == RotationEngine.MovementCorrectionMode.SILENT) {
            event.velocity = adjustVelocity(event.velocity, event.movementInput, event.speed)
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
     * Adjusts the velocity based on the current rotation
     */
    private fun adjustVelocity(currVelocity: Vec3d, movementInput: Vec3d, speed: Float): Vec3d {
        RotationObserver.currentOrientation?.let { rotation ->
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
