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
package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes

import net.ccbluex.fastutil.objectHashSetOf
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.world.entity.player.Player
import java.util.UUID

object HorizonAntiBotMode : AntiBotMode("Horizon") {

    private val botList = objectHashSetOf<UUID>()

    val packetHandler = handler<PacketEvent> {
        when (val packet = it.packet) {
            is ClientboundPlayerInfoUpdatePacket -> {
                if (ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER in packet.actions()) {
                    for (entry in packet.entries()) {
                        if (entry.gameMode != null) {
                            continue
                        }

                        botList.add(entry.profileId)
                    }
                }
            }

            is ClientboundPlayerInfoRemovePacket -> {
                packet.profileIds.forEach(botList::remove)
            }
        }
    }

    override fun isBot(entity: Player): Boolean {
        return botList.contains(entity.uuid)
    }

    override fun reset() {
        botList.clear()
    }
}
