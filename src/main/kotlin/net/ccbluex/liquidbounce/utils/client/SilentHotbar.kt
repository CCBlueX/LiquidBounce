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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.additions.realSelectedSlot
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.SelectHotbarSlotSilentlyEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.minecraft.world.entity.player.Inventory
import org.jetbrains.annotations.Range

/**
 * Manages things like [ModuleScaffold]'s silent mode.
 * Not thread safe, please only use this on the main-thread of minecraft
 */
object SilentHotbar : EventListener {

    private var hotbarState: SilentHotbarState? = null
    private var ticksSinceLastUpdate: Int = 0

    /**
     * Returns the slot that interactions would take place with
     */
    val serversideSlot: Int
        get() = hotbarState?.enforcedHotbarSlot ?: mc.player?.inventory?.realSelectedSlot ?: 0

    val clientsideSlot: Int
        get() = hotbarState?.clientsideSlot ?: mc.player?.inventory?.realSelectedSlot ?: 0

    /**
     * Silently selects a main-hand hotbar slot for duration of [ticksUntilReset].
     * Offhand is ignored because it is not selected through held-item changes.
     *
     * @return `true` when the slot is selected or no selection is required, `false` when the request is cancelled
     */
    fun selectSlotSilently(requester: Any?, slot: HotbarItemSlot, ticksUntilReset: Int): Boolean =
        slot.hotbarIndex?.let { selectSlotSilently(requester, it, ticksUntilReset) } ?: true

    /**
     * @see net.minecraft.world.entity.player.Inventory.isHotbarSlot
     */
    fun selectSlotSilently(
        requester: Any?,
        slot: @Range(from = 0, to = Inventory.SELECTION_SIZE - 1L) Int,
        ticksUntilReset: Int,
    ): Boolean {
        require(Inventory.isHotbarSlot(slot)) { "Invalid hotbar slot: $slot" }

        val event = EventManager.callEvent(SelectHotbarSlotSilentlyEvent(requester, slot))
        if (event.isCancelled) {
            return false
        }

        hotbarState = SilentHotbarState(slot, requester, ticksUntilReset, clientsideSlot)
        ticksSinceLastUpdate = 0
        return true
    }

    fun resetSlot(requester: Any?) {
        if (hotbarState?.requester === requester) {
            hotbarState = null
        }
    }

    fun isSlotModified() = hotbarState != null

    /**
     * Returns if the slot is currently getting modified by a given requester
     */
    fun isSlotModifiedBy(requester: Any?) = hotbarState?.requester === requester

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        hotbarState = null
        ticksSinceLastUpdate = 0
    }

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
    val requester: Any?,
    val ticksUntilReset: Int,
    val clientsideSlot: Int
)
