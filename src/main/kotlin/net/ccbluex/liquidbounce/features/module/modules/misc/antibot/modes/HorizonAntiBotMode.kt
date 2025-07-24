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
package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket
import java.util.*

object HorizonAntiBotMode : Choice("Horizon"), ModuleAntiBot.IAntiBotMode {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleAntiBot.modes

    private val botList = hashSetOf<UUID>()

    val packetHandler = handler<PacketEvent> {
        when (val packet = it.packet) {
            is PlayerListS2CPacket -> {
                if (packet.actions.first() == PlayerListS2CPacket.Action.ADD_PLAYER) {
                    for (entry in packet.entries) {
                        if (entry.gameMode != null) {
                            continue
                        }

                        botList.add(entry.profileId)
                    }
                }
            }

            is PlayerRemoveS2CPacket -> {
                packet.profileIds.forEach(botList::remove)
            }
        }
    }

    override fun isBot(entity: PlayerEntity): Boolean {
        return botList.contains(entity.uuid)
    }

    override fun reset() {
        botList.clear()
    }
}
