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

/**
 * tested on mineblaze.net
 * made for intave version 14.8.4
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.util.hit.BlockHitResult

internal class NoSlowBlockIntave14(override val parent: ChoiceConfigurable<*>) : Choice("Intave14") {

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is PlayerInteractBlockC2SPacket -> {
                if (player.isUsingItem && player.moving) {
                    network.sendPacket(
                        PlayerInteractBlockC2SPacket(
                            packet.hand, BlockHitResult(
                                packet.blockHitResult.blockPos.toCenterPos(),
                                packet.blockHitResult.side,
                                packet.blockHitResult.blockPos,
                                packet.blockHitResult.isInsideBlock
                            ), packet.sequence
                        )
                    )
                }
            }

            is PlayerInteractItemC2SPacket -> {
                network.sendPacket(
                    PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                        player.blockPos,
                        player.horizontalFacing.opposite
                    )
                )
            }
        }
    }
}
