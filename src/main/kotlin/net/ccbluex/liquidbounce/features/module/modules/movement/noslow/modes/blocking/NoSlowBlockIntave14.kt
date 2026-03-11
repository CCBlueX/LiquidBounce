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
package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.phys.BlockHitResult

/**
 * tested on mineblaze.net
 * made for intave version 14.8.4
 */

internal class NoSlowBlockIntave14(override val parent: ModeValueGroup<*>) : Mode("Intave14") {

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is ServerboundUseItemOnPacket -> {
                if (player.isUsingItem && player.moving) {
                    network.send(
                        ServerboundUseItemOnPacket(
                            packet.hand, BlockHitResult(
                                packet.hitResult.blockPos.center,
                                packet.hitResult.direction,
                                packet.hitResult.blockPos,
                                packet.hitResult.isInside
                            ), packet.sequence
                        )
                    )
                }
            }
        }
    }
}
