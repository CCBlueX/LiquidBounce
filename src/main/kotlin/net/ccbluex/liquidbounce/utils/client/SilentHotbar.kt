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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.SelectHotbarSlotSilentlyEvent
import net.ccbluex.liquidbounce.event.handler

/**
 * Manages things like [Scaffold]'s silent mode. Not thread safe, please only use this on the main-thread of minecraft
 */
object SilentHotbar : EventListener {

    private var hotbarState: SilentHotbarState? = null
    private var ticksSinceLastUpdate: Int = 0

    /**
     * Returns the slot that interactions would take place with
     */
    val serversideSlot: Int
        get() = hotbarState?.enforcedHotbarSlot ?: mc.player?.inventory?.selectedSlot ?: 0

    val clientsideSlot: Int
        get() = hotbarState?.clientsideSlot ?: mc.player?.inventory?.selectedSlot ?: 0

    fun selectSlotSilently(requester: Any?, slot: Int, ticksUntilReset: Int = 20) {
        val event = EventManager.callEvent(SelectHotbarSlotSilentlyEvent(requester, slot))
        if (event.isCancelled) {
            return
        }

        hotbarState = SilentHotbarState(slot, requester, ticksUntilReset, clientsideSlot)
        ticksSinceLastUpdate = 0
    }

    fun resetSlot(requester: Any?) {
        if (hotbarState?.requester == requester) {
            hotbarState = null
        }
    }

    fun isSlotModified() = hotbarState != null

    /**
     * Returns if the slot is currently getting modified by a given requester
     */
    fun isSlotModifiedBy(requester: Any?) = hotbarState?.requester == requester

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = 1001) {
        val hotbarState = hotbarState ?: return@handler

        if (ticksSinceLastUpdate >= hotbarState.ticksUntilReset) {
            this.hotbarState = null
            return@handler
        }

        ticksSinceLastUpdate++
    }
}

private class SilentHotbarState(
    val enforcedHotbarSlot: Int,
    var requester: Any?,
    var ticksUntilReset: Int,
    var clientsideSlot: Int
)
