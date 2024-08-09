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

package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket

internal class NoSlowSharedIntave14(override val parent: ChoiceConfigurable<*>) : Choice("Intave14") {

    @Suppress("unused")
    private val onNetworkTick = handler<PlayerNetworkMovementTickEvent> { event ->
        if (player.isUsingItem && event.state == EventState.PRE) {
            if (player.moving) {
                if (player.itemUseTimeLeft == 0 || player.itemUseTime == 1) {
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
}
