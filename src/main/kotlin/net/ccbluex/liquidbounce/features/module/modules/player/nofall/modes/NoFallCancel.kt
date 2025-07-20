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

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

internal object NoFallCancel : Choice("Cancel") {

    private val fallDistance = choices("FallDistance", Smart,
        arrayOf(Smart, Constant))
    private val resetFallDistance by boolean("ResetFallDistance", true)
    private val cancelSetback by boolean("CancelSetbackPacket", false)

    private var isFalling = false

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.isCancelled) {
            return@handler
        }

        when (val packet = event.packet) {
            is PlayerPositionLookS2CPacket -> {
                val change = packet.change

                if (cancelSetback) {
                    event.cancelEvent()
                }

                val pos = change.position()
                network.sendPacket(
                    PlayerMoveC2SPacket.Full(
                        pos.x,
                        pos.y,
                        pos.z,
                        change.yaw,
                        change.pitch,
                        true,
                        player.horizontalCollision
                    )
                )
                isFalling = false
            }

            is PlayerMoveC2SPacket -> {
                if (player.fallDistance >= fallDistance.activeChoice.value) {
                    isFalling = true

                    event.cancelEvent()
                    if (resetFallDistance) {
                        player.onLanding()
                    }
                }
            }
        }
    }

    private abstract class DistanceMode(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = fallDistance

        abstract val value: Float
    }

    private object Smart : DistanceMode("Smart") {
        override val value: Float
            get() = player.getAttributeValue(EntityAttributes.SAFE_FALL_DISTANCE).toFloat()
    }

    private object Constant : DistanceMode("Constant") {
        override val value by float("Value", 1.7f, 0f..5f)
    }

}
