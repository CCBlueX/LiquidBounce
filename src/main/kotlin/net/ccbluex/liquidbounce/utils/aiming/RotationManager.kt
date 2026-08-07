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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MouseRotationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerVelocityStrafe
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.aiming.utils.setRotation
import net.ccbluex.liquidbounce.utils.aiming.utils.withFixedYaw
import net.ccbluex.liquidbounce.utils.client.RestrictedSingleUseAction
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.MODEL_STATE
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.client.RequestHandler
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Relative

/**
 * A rotation manager
 */
object RotationManager : EventListener {

    /**
     * Our final target rotation. This rotation is only used to define our current rotation.
     */
    private val rotationTarget
        get() = rotationTargetHandler.getActiveRequestValue()
    private val rotationTargetHandler = RequestHandler<RotationTarget>()

    @Suppress("unused")
    private val lifecycleListener = object : EventListener {
        private val worldChangeHandler = handler<WorldChangeEvent> { reset() }
    }

    val activeRotationTarget: RotationTarget?
        get() = rotationTarget ?: previousRotationTarget
    internal var previousRotationTarget: RotationTarget? = null
        private set

    /**
     * The rotation we want to aim at. This DOES NOT mean that the server already received this rotation.
     */
    var currentRotation: Rotation? = null
        private set(value) {
            previousRotation = if (value == null) {
                null
            } else {
                field ?: mc.player?.rotation ?: Rotation.ZERO
            }

            field = value
        }

    // Used for rotation interpolation
    var playerRotation: Rotation? = null
        private set
    var previousRotation: Rotation? = null
        private set

    private val fakeLagging
        get() = BlinkManager.isLagging || ModuleBacktrack.isLagging()

    private val freezing
        get() = ModuleFreeze.running

    val serverRotation: Rotation
        get() = if (fakeLagging || freezing) theoreticalServerRotation else actualServerRotation

    /**
     * The rotation that was already sent to the server and is currently active.
     * The value is not being written by the packets, but we gather the Rotation from the last yaw and pitch variables
     * from our player instance handled by the sendMovementPackets() function.
     */
    var actualServerRotation = Rotation.ZERO
        private set

    private var theoreticalServerRotation = Rotation.ZERO

    private fun reset() {
        rotationTargetHandler.clear()
        previousRotationTarget = null
        currentRotation = null
        playerRotation = null
        previousRotation = null
        actualServerRotation = Rotation.ZERO
        theoreticalServerRotation = Rotation.ZERO
    }

    /**
     * Applies the rotation normalization performed by the vanilla server for rotation-bearing
     * movement and item-use packets.
     *
     * @see net.minecraft.server.network.ServerGamePacketListenerImpl.handleMovePlayer
     * @see net.minecraft.server.network.ServerGamePacketListenerImpl.handleUseItem
     */
    private fun serverboundRotation(yaw: Float, pitch: Float) = Rotation(
        yaw = Mth.wrapDegrees(yaw),
        pitch = Mth.wrapDegrees(pitch).coerceIn(-90f, 90f),
        isNormalized = true
    )

    /**
     * Resolves the relative rotation flags used by vanilla player position and rotation updates.
     *
     * @see net.minecraft.world.entity.PositionMoveRotation.calculateAbsolute
     * @see net.minecraft.world.entity.Entity.forceSetRotation
     */
    private fun clientboundRotation(
        yaw: Float,
        pitch: Float,
        relativeYaw: Boolean,
        relativePitch: Boolean
    ) = Rotation(
        yaw = yaw + if (relativeYaw) actualServerRotation.yaw else 0f,
        pitch = (pitch + if (relativePitch) actualServerRotation.pitch else 0f).coerceIn(-90f, 90f),
        isNormalized = true
    )

    @Suppress("LongParameterList")
    fun setRotationTarget(
        rotation: Rotation,
        considerInventory: Boolean = true,
        valueGroup: RotationsValueGroup,
        priority: Priority,
        provider: ClientModule,
        whenReached: RestrictedSingleUseAction? = null
    ) {
        setRotationTarget(valueGroup.toRotationTarget(
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
            if (InventoryManager.isInventoryOpen || mc.gui.screen() is ContainerScreen) {
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
        val playerRotation = player.rotation.also { this.playerRotation = it }
        val activeRotationTarget = this.activeRotationTarget ?: return
        val rotationTarget = this.rotationTarget

        // Prevents any rotation changes when inventory is opened
        if (isRotatingAllowed(activeRotationTarget)) {
            val fromRotation = currentRotation ?: playerRotation
            val rotation = activeRotationTarget.towards(fromRotation, rotationTarget == null)
                // After generating the next rotation, we need to normalize it
                .normalize()

            val diff = rotation.rotationDeltaLengthTo(playerRotation)

            if (rotationTarget == null && (activeRotationTarget.movementCorrection == MovementCorrection.CHANGE_LOOK
                    || activeRotationTarget.processors.isEmpty()
                    || diff <= activeRotationTarget.resetThreshold)) {
                currentRotation?.let { currentRotation ->
                    player.yRot = player.withFixedYaw(currentRotation)
                    player.yBob = player.yRot
                    player.yBobO = player.yRot
                }

                currentRotation = null
                previousRotationTarget = null
            } else {
                currentRotation = rotation
                previousRotationTarget = activeRotationTarget

                rotationTarget?.whenReached?.invoke()
            }
        }

        // Update reset ticks
        rotationTargetHandler.tick()
    }

    fun applyChangeLookRotation(partialTicks: Float) {
        val activeRotationTarget = this.activeRotationTarget ?: return

        if (isRotatingAllowed(activeRotationTarget)
            && activeRotationTarget.movementCorrection == MovementCorrection.CHANGE_LOOK) {
            val playerRotation = playerRotation ?: return
            val currentRotation = currentRotation ?: return

            val interpolated = playerRotation.interpolateTo(currentRotation, partialTicks)
            player.setRotation(interpolated)
        }
    }

    @Suppress("unused")
    private val mouseMovement = handler<MouseRotationEvent> { event ->
        val activeRotationTarget = this.activeRotationTarget ?: return@handler
        if (!isRotatingAllowed(activeRotationTarget) ||
            activeRotationTarget.movementCorrection != MovementCorrection.CHANGE_LOOK) {
            return@handler
        }

        fun adjustRotation(rotation: Rotation): Rotation =
            RotationUtil.applyMouseTurnDelta(rotation, event.cursorDeltaX, event.cursorDeltaY)

        playerRotation?.let { rotation ->
            playerRotation = adjustRotation(rotation)
        }

        currentRotation?.let { rotation ->
            currentRotation = adjustRotation(rotation)
        }
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

            event.velocity = Entity.getInputVector(
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
            is ServerboundMovePlayerPacket -> {
                // If we are not changing the look, we don't need to update the rotation
                if (!packet.hasRot) {
                    return@handler
                }

                serverboundRotation(packet.yRot, packet.xRot)
            }
            is ClientboundPlayerPositionPacket -> clientboundRotation(
                packet.change.yRot,
                packet.change.xRot,
                Relative.Y_ROT in packet.relatives,
                Relative.X_ROT in packet.relatives,
            )
            is ClientboundPlayerRotationPacket -> clientboundRotation(
                packet.yRot,
                packet.xRot,
                packet.relativeY,
                packet.relativeX,
            )
            is ServerboundUseItemPacket -> serverboundRotation(packet.yRot, packet.xRot)
            else -> return@handler
        }

        // Incoming corrections already changed the server-side player before they were sent. For
        // outgoing packets, only packets that pass the event pipeline can affect the server.
        if (event.origin == TransferOrigin.INCOMING || !event.isCancelled) {
            actualServerRotation = rotation
        }
        theoreticalServerRotation = rotation
    }

    override val running: Boolean
        get() = inGame

}
