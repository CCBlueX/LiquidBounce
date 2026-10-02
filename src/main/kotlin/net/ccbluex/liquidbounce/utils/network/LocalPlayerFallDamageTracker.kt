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
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.minecraft.tags.DamageTypeTags

/**
 * Tracks whether the local player's current hurt cycle was caused by fall damage.
 *
 * The damage source travels with every successful hit
 * ([net.minecraft.server.level.ServerLevel#broadcastDamageEvent]), and the client applies the very same
 * hurt cycle from it - [net.minecraft.world.entity.LivingEntity#handleDamageEvent] sets `hurtTime`
 * to [HURT_TICKS]. Classifying by that packet needs no pairing with a following knockback:
 * [net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket] cannot tell the two apart,
 * since `fall` is part of [net.minecraft.tags.DamageTypeTags.NO_KNOCKBACK] and therefore arrives with
 * the unchanged delta movement, while hostile entities may also set the sync flag without any damage.
 *
 * @see net.minecraft.world.entity.LivingEntity#hurtServer
 */
object LocalPlayerFallDamageTracker : EventListener {

    private const val HURT_TICKS = 10

    private var fallDamageTicks = 0

    val isCurrentFallDamage: Boolean
        get() = fallDamageTicks > 0

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(READ_FINAL_STATE) { event ->
        if (event.origin != TransferOrigin.INCOMING || !event.original || event.isCancelled) {
            return@handler
        }

        val packet = event.packet
        if (packet.isLocalPlayerDamage()) {
            fallDamageTicks = if (packet.sourceType.`is`(DamageTypeTags.IS_FALL)) HURT_TICKS else 0
        }
    }

    @Suppress("unused")
    private val gameTickHandler = handler<GameTickEvent> {
        if (fallDamageTicks > 0) {
            fallDamageTicks--
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        fallDamageTicks = 0
    }

}
