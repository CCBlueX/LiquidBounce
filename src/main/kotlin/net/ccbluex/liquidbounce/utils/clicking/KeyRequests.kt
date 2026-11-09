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
package net.ccbluex.liquidbounce.utils.clicking

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.KeyMapping

/**
 * Clickers on the same key would add up their CPS, and so would the user's own presses. One clicker holds
 * a key at a time: a higher [Priority] takes it over, otherwise the holder keeps it while it keeps asking.
 * Meanwhile the user's presses on that key are dropped.
 */
internal object KeyRequests : EventListener {

    private class Hold(var clicker: Clicker<*>, var priority: Int, var until: Long)

    private val holds = Reference2ObjectOpenHashMap<KeyMapping, Hold>()
    private var tick = 0L

    /**
     * Asks for the key of [clicker] for the next [ticks] ticks and returns whether it holds the key now.
     */
    fun request(clicker: Clicker<*>, priority: Priority, ticks: Int): Boolean {
        val hold = holds[clicker.keyBinding]
        if (hold == null) {
            holds[clicker.keyBinding] = Hold(clicker, priority.priority, tick + ticks)
            return true
        }

        if (hold.clicker !== clicker && hold.isActive && hold.priority >= priority.priority) {
            return false
        }

        hold.clicker = clicker
        hold.priority = priority.priority
        hold.until = tick + ticks
        return true
    }

    private val Hold.isActive get() = until > tick && clicker.running

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) {
        tick++

        for ((key, hold) in holds) {
            if (hold.isActive) {
                key.clickCount = 0
            }
        }
    }

}
