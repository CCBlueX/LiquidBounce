/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.handler

/**
 * Manages things like [Scaffold]'s silent mode. Not thread safe, please only use this on the main-thread of minecraft
 */
object SilentHotbar : Listenable {

    private var hotbarState: SilentHotbarState? = null
    private var ticksSinceLastUpdate: Int = 0

    /**
     * When the minecraft's slot is overridden, this value will return the new slot.
     * Otherwise `null`
     */
    val enforcedSlot: Int?
        get() = this.hotbarState?.enforcedHotbarSlot

    /**
     * Returns the slot that interactions would take place with
     */
    val serversideSlot: Int
        get() = this.hotbarState?.enforcedHotbarSlot ?: mc.player!!.inventory.selectedSlot

    fun selectSlotSilently(requester: Any?, slot: Int, ticksUntilReset: Int = 20) {
        val allowOverride = this.hotbarState == null || ticksSinceLastUpdate > 1

        if (!allowOverride) {
            return
        }

        this.hotbarState = SilentHotbarState(slot, requester, ticksUntilReset)
        this.ticksSinceLastUpdate = 0
    }

    fun canGetSlot() = mc.player != null


    fun resetSlot(requester: Any?) {
        if (this.hotbarState?.requester == requester) {
            this.hotbarState = null
        }
    }

    val gametickHandler = handler<GameTickEvent> {
        val hotbarState = this.hotbarState ?: return@handler

        if (ticksSinceLastUpdate >= hotbarState.ticksUntilReset) {
            this.hotbarState = null
            return@handler
        }

        this.ticksSinceLastUpdate++
    }
}

private class SilentHotbarState(val enforcedHotbarSlot: Int, var requester: Any?, var ticksUntilReset: Int)
