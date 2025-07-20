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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerVelocityStrafe
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.aiming.utils.setRotation
import net.ccbluex.liquidbounce.utils.aiming.utils.withFixedYaw
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.MODEL_STATE
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.RequestHandler
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * A rotation manager
 */
object RotationManager : EventListener {

    /**
     * Our final target rotation. This rotation is only used to define our current rotation.
     */
    private val rotationTarget
        get() = rotationTargetHandler.getActiveRequestValue()
    private var rotationTargetHandler = RequestHandler<RotationTarget>()

    val activeRotationTarget: RotationTarget?
        get() = rotationTarget ?: previousRotationTarget
    internal var previousRotationTarget: RotationTarget? = null

    /**
     * The rotation we want to aim at. This DOES NOT mean that the server already received this rotation.
     */
    var currentRotation: Rotation? = null
        set(value) {
            previousRotation = if (value == null) {
                null
            } else {
                field ?: mc.player?.rotation ?: Rotation.ZERO
            }

            field = value
        }

    // Used for rotation interpolation
    var previousRotation: Rotation? = null

    private val fakeLagging
        get() = PacketQueueManager.isLagging || ModuleBacktrack.isLagging()

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

    @Suppress("LongParameterList")
    fun setRotationTarget(
        rotation: Rotation,
        considerInventory: Boolean = true,
        configurable: RotationsConfigurable,
        priority: Priority,
        provider: ClientModule,
        whenReached: RestrictedSingleUseAction? = null
    ) {
        setRotationTarget(configurable.toRotationTarget(
            rotation, considerInventory = considerInventory, whenReached = whenReached
        ), priority, provider)
    }

    fun setRotationTarget(plan: RotationTarget, priority: Priority, provider: ClientModule) {
        if (!allowedToUpdate()) {
            return
        }

        rotationTargetHandler.request(
            RequestHandler.Request(
                if (plan.movementCorrection == MovementCorrection.CHANGE_LOOK) 1 else plan.ticksUntilReset,
                priority.priority,
                provider,
                plan
            )
        )
    }

    /**
     * Checks if the rotation is allowed to be updated
     */
    fun isRotatingAllowed(rotationTarget: RotationTarget): Boolean {
        if (!allowedToUpdate()) {
            return false
        }

        if (rotationTarget.considerInventory) {
            if (InventoryManager.isInventoryOpen || mc.currentScreen is GenericContainerScreen) {
                return false
            }
        }

        return true
    }

    /**
     * Update current rotation to a new rotation step
     */
    @Suppress("CognitiveComplexMethod", "NestedBlockDepth")
    fun update() {
        val activeRotationTarget = this.activeRotationTarget ?: return
        val playerRotation = player.rotation

        val rotationTarget = this.rotationTarget

        // Prevents any rotation changes when inventory is opened
        if (isRotatingAllowed(activeRotationTarget)) {
            val fromRotation = currentRotation ?: playerRotation
            val rotation = activeRotationTarget.towards(fromRotation, rotationTarget == null)
                // After generating the next rotation, we need to normalize it
                .normalize()

            val diff = rotation.angleTo(playerRotation)

            if (rotationTarget == null && (activeRotationTarget.movementCorrection == MovementCorrection.CHANGE_LOOK
                    || activeRotationTarget.processors.isEmpty()
                    || diff <= activeRotationTarget.resetThreshold)) {
                currentRotation?.let { currentRotation ->
                    player.yaw = player.withFixedYaw(currentRotation)
                    player.renderYaw = player.yaw
                    player.lastRenderYaw = player.yaw
                }

                currentRotation = null
                previousRotationTarget = null
            } else {
                if (activeRotationTarget.movementCorrection == MovementCorrection.CHANGE_LOOK) {
                    player.setRotation(rotation)
                }

                currentRotation = rotation
                previousRotationTarget = activeRotationTarget

                rotationTarget?.whenReached?.invoke()
            }
        }

        // Update reset ticks
        rotationTargetHandler.tick()
    }

    /**
     * Checks if it should update the server-side rotations
     */
    private fun allowedToUpdate() = !CombatManager.shouldPauseRotation

    fun rotationMatchesPreviousRotation(): Boolean {
        val player = mc.player ?: return false

        currentRotation?.let {
            return it == previousRotation
        }

        return player.rotation == player.lastRotation
    }

    @Suppress("unused")
    private val velocityHandler = handler<PlayerVelocityStrafe>(priority = MODEL_STATE) { event ->
        if (activeRotationTarget?.movementCorrection != MovementCorrection.OFF) {
            val rotation = currentRotation ?: return@handler

            event.velocity = Entity.movementInputToVelocity(
                event.movementInput,
                event.speed,
                rotation.yaw
            )
        }
    }

    @Suppress("unused")
    private val gameTickHandler = handler<GameTickEvent>(
        priority = FIRST_PRIORITY
    ) { event ->
        EventManager.callEvent(RotationUpdateEvent)
        update()
    }

    /**
     * Track rotation changes
     *
     * We cannot only rely on player.lastYaw and player.lastPitch because
     * sometimes we update the rotation off chain (e.g. on interactItem)
     * and the player.lastYaw and player.lastPitch are not updated.
     */
    @Suppress("unused")
    val packetHandler = handler<PacketEvent>(
        priority = EventPriorityConvention.READ_FINAL_STATE
    ) { event ->
        val rotation = when (val packet = event.packet) {
            is PlayerMoveC2SPacket -> {
                // If we are not changing the look, we don't need to update the rotation
                if (!packet.changeLook) {
                    return@handler
                }

                // We trust that we have sent a normalized rotation, if not, ... why?
                Rotation(packet.yaw, packet.pitch, isNormalized = true)
            }
            is PlayerPositionLookS2CPacket -> Rotation(packet.change.yaw, packet.change.pitch, isNormalized = true)
            is PlayerInteractItemC2SPacket -> Rotation(packet.yaw, packet.pitch, isNormalized = true)
            else -> return@handler
        }

        // This normally applies to Modules like Blink, BadWifi, etc.
        if (!event.isCancelled) {
            actualServerRotation = rotation
        }
        theoreticalServerRotation = rotation
    }

    override val running: Boolean
        get() = inGame

}
