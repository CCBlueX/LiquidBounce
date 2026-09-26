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

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.event.until
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

/**
 * Bypassing GrimAC Anti Cheat (8/3/2025, Loyisa Server)
 * Minecraft Version 1.9+
 *
 * @author XeContrast
 */
internal object NoFallGrim2371 : NoFallMode("Grim2371-1.9+") {

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (player.onGround() || player.fallDistance < 2.5) {
            return@tickHandler
        }

        until<PlayerNetworkMovementTickEvent> { event ->
            if (!player.onGround() || event.state != EventState.PRE) {
                return@until false
            }

            event.cancelEvent()
            network.send(ServerboundMovePlayerPacket.StatusOnly(true, player.horizontalCollision))
            true
        }

        until<MovementInputEvent> { event ->
            event.jump = true
            player.onGround()
        }

        tickUntil { player.onGround() }
    }

}
