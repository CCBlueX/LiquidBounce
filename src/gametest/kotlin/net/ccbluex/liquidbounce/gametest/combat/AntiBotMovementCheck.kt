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
package net.ccbluex.liquidbounce.gametest.combat

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes.CustomAntiBotMode
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.VecDelta
import java.util.UUID

private const val MOVES = 3

/**
 * AntiBot: a player counts as a bot until enough movement updates arrived.
 */
fun CombatScene.checkAntiBotMovement() {
    startCustomAntiBot {
        CustomAntiBotMode.set("Movement/RequiredUpdates", MOVES.toString())
        CustomAntiBotMode.set("Movement/Enabled", "true")
    }
    val player = spawnFakePlayer(UUID.randomUUID(), FAKE_PLAYER_ID, "Moving")
    check(isBot(player)) { "AntiBot: a player without movement updates counts as real" }
    context.onClient {
        repeat(MOVES) {
            EventManager.callEvent(
                PacketEvent(TransferOrigin.INCOMING, ClientboundMoveEntityPacket.Pos(player, VecDelta.ZERO, true))
            )
        }
    }
    context.waitTick()
    check(!isBot(player)) { "AntiBot: still a bot after $MOVES movement updates" }
    stopAntiBot(player)
}
