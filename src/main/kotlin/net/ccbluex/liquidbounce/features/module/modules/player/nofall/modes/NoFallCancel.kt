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
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

internal object NoFallCancel : NoFallMode("Cancel") {

    private val fallDistance = modes("FallDistance", Smart,
        arrayOf(Smart, Constant))
    private val resetFallDistance by boolean("ResetFallDistance", true)
    private val cancelSetback by boolean("CancelSetbackPacket", false)

    private var isFalling = false

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.isCancelled) {
            return@handler
        }

        when (val packet = event.packet) {
            is ClientboundPlayerPositionPacket -> {
                val change = packet.change

                if (cancelSetback) {
                    event.cancelEvent()
                }

                val pos = change.position()
                network.send(
                    ServerboundMovePlayerPacket.PosRot(
                        pos.x,
                        pos.y,
                        pos.z,
                        change.yRot,
                        change.xRot,
                        true,
                        player.horizontalCollision
                    )
                )
                isFalling = false
            }

            is ServerboundMovePlayerPacket -> {
                if (player.fallDistance >= fallDistance.activeMode.value) {
                    isFalling = true

                    event.cancelEvent()
                    if (resetFallDistance) {
                        player.resetFallDistance()
                    }
                }
            }
        }
    }

    private abstract class DistanceMode(name: String) : Mode(name) {
        override val parent: ModeValueGroup<*>
            get() = fallDistance

        abstract val value: Float
    }

    private object Smart : DistanceMode("Smart") {
        override val value: Float
            get() = playerSafeFallDistance.toFloat()
    }

    private object Constant : DistanceMode("Constant") {
        override val value by float("Value", 1.7f, 0f..5f)
    }

}
