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

package net.ccbluex.liquidbounce.utils.network

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.world.damagesource.DamageTypes

/**
 * Tracks whether the local player's current client-side hurt window comes from vanilla fall damage.
 *
 * The server sends [net.minecraft.network.protocol.game.ClientboundDamageEventPacket] for
 * [DamageTypes.FALL] before the later `hurtMarked`-driven motion sync, while the client keeps the
 * currently active damage window in `hurtTime`.
 *
 * @see net.minecraft.world.entity.LivingEntity#hurt
 * @see net.minecraft.server.level.ServerLevel#broadcastDamageEvent
 * @see net.minecraft.world.entity.LivingEntity#handleDamageEvent
 * @see net.minecraft.server.level.ServerEntity#sendChanges
 */
object LocalPlayerFallDamageTracker : EventListener {

    @Volatile
    private var currentDamageState = DamageState.NONE

    val isCurrentFallDamage: Boolean
        get() = currentDamageState == DamageState.FALL && (mc.player?.hurtTime ?: 0) > 0

    private fun reset() {
        currentDamageState = DamageState.NONE
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.origin != TransferOrigin.INCOMING) {
            return@handler
        }

        val packet = event.packet
        when {
            packet.isLocalPlayerDamage() -> {
                currentDamageState =
                    if (packet.sourceType.`is`(DamageTypes.FALL)) DamageState.FALL else DamageState.OTHER
            }
        }
    }

    @Suppress("unused")
    private val gameTickHandler = handler<GameTickEvent> {
        if ((mc.player?.hurtTime ?: 0) <= 0) {
            reset()
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        reset()
    }

    private enum class DamageState {
        NONE,
        FALL,
        OTHER,
    }

}
