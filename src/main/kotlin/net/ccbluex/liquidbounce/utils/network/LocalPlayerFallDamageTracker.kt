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
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.world.damagesource.DamageTypes
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tracks whether the local player's current knockback/hurt cycle was caused by vanilla fall damage.
 *
 * Vanilla sends the local player's fall [net.minecraft.network.protocol.game.ClientboundDamageEventPacket]
 * before the matching `hurtMarked`-driven [ClientboundSetEntityMotionPacket]. We only confirm fall damage
 * after seeing that following motion packet.
 *
 * @see net.minecraft.world.entity.LivingEntity#hurt
 * @see net.minecraft.server.level.ServerLevel#broadcastDamageEvent
 * @see net.minecraft.server.level.ServerEntity#sendChanges
 */
object LocalPlayerFallDamageTracker : EventListener {

    @Volatile
    private var state = State.NONE

    private val activeTicks = AtomicInteger()
    private val pendingTicks = AtomicInteger()

    val isCurrentFallDamage: Boolean
        get() = state == State.CONFIRMED_FALL_DAMAGE && activeTicks.get() > 0

    private fun reset() {
        state = State.NONE
        activeTicks.set(0)
        pendingTicks.set(0)
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(READ_FINAL_STATE) { event ->
        if (event.origin != TransferOrigin.INCOMING || !event.original || event.isCancelled) {
            return@handler
        }

        val packet = event.packet
        when {
            packet.isLocalPlayerDamage() -> {
                activeTicks.set(0)
                pendingTicks.set(0)
                state = if (packet.sourceType.`is`(DamageTypes.FALL)) State.AWAITING_FALL_DAMAGE_MOTION else State.NONE
            }

            packet is ClientboundSetEntityMotionPacket && packet.id == mc.player?.id -> {
                if (state == State.AWAITING_FALL_DAMAGE_MOTION) {
                    state = State.CONFIRMED_FALL_DAMAGE
                    activeTicks.set(DAMAGE_WINDOW_TICKS)
                }
            }
        }
    }

    @Suppress("unused")
    private val gameTickHandler = handler<GameTickEvent> {
        when (state) {
            State.AWAITING_FALL_DAMAGE_MOTION -> {
                if (pendingTicks.incrementAndGet() > FOLLOWING_MOTION_TIMEOUT_TICKS) {
                    reset()
                }
            }

            State.CONFIRMED_FALL_DAMAGE -> {
                val remainingTicks = activeTicks.updateAndGet { ticks ->
                    if (ticks > 0) ticks - 1 else 0
                }
                if (remainingTicks <= 0) {
                    reset()
                }
            }

            State.NONE -> {}
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        reset()
    }

    private enum class State {
        NONE,
        AWAITING_FALL_DAMAGE_MOTION,
        CONFIRMED_FALL_DAMAGE,
    }

    private const val FOLLOWING_MOTION_TIMEOUT_TICKS = 1
    private const val DAMAGE_WINDOW_TICKS = 10

}
