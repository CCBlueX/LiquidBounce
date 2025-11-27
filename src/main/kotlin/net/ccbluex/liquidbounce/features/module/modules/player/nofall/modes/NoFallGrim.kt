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

package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerVelocityStrafe
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler

import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

internal object NoFallGrim : Choice("Grim") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    private var waitLagPacketTicks = 0
    private var shouldHandleFall = false
    private var shouldJump = false
    private var isLagged = false

    val jumping: Boolean
        get() = waitLagPacketTicks > 0 || shouldJump || isLagged

    override fun enable() {
        reset()
    }

    private fun reset() {
        waitLagPacketTicks = 0
        shouldHandleFall = false
        shouldJump = false
        isLagged = false
    }

    private val canWork
        get() = !player.isInFluid
            && !player.isHoldingOntoLadder
            && !player.isClimbing
            && !player.abilities.flying
            && !InventoryManager.isInventoryOpen
            && mc.currentScreen !is GenericContainerScreen

    private val shouldCancelJump
        get() = (shouldHandleFall || shouldJump) && canWork

    @Suppress("unused")
    private val movementInputEventHandler = handler<MovementInputEvent> { event ->
        if (shouldCancelJump) {
            event.jump = false
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (isLagged && shouldHandleFall) {
            shouldJump = true
            shouldHandleFall = false
            isLagged = false
        }

        if (shouldCancelJump) {
            mc.options.jumpKey.isPressed = false
        }

        if (waitLagPacketTicks > 0) {
            waitLagPacketTicks--
            if (waitLagPacketTicks == 0 || !canWork) {
                reset()
            }
        }
    }

    @Suppress("unused")
    private val playerVelocityStrafeHandler = handler<PlayerVelocityStrafe> {
        if (player.isOnGround && shouldJump) {
            player.jump()
            shouldJump = false
        }
    }

    @Suppress("unused")
    private val playerNetworkMovementTickEventHandler = handler<PlayerNetworkMovementTickEvent> { event ->
        if (event.state != EventState.PRE) {
            return@handler
        }

        if (!canWork) {
            if (shouldHandleFall || jumping) {
                reset()
            }
            return@handler
        }

        if (!event.ground
            && player.fallDistance >= player.getAttributeValue(
                EntityAttributes.SAFE_FALL_DISTANCE
            ).toFloat()
            && !shouldHandleFall
        ) {
            shouldHandleFall = true
            waitLagPacketTicks = 0
            isLagged = false
        }

        if (shouldHandleFall && player.fallDistance < 3.0f) {
            event.ground = false
            if (waitLagPacketTicks == 0) {
                network.sendPacket(
                    PlayerMoveC2SPacket.PositionAndOnGround(
                        player.x - (616..3473).random(),
                        player.y,
                        player.z - (616..3473).random(),
                        false,
                        player.horizontalCollision
                    )
                )
                waitLagPacketTicks = 20
            }
        }
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        if (!shouldHandleFall || waitLagPacketTicks == 0 || !canWork) return@handler

        when (val packet = event.packet) {
            is PlayerMoveC2SPacket -> event.cancelEvent()

            is PlayerPositionLookS2CPacket -> {
                isLagged = true
                waitLagPacketTicks = 0
            }
        }
    }

}
