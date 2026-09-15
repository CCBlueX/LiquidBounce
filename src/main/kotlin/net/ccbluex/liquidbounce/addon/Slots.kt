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
package net.ccbluex.liquidbounce.addon

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots as AllSlots
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.function.Predicate

/**
 * The player's items, and the silent hotbar every module shares: the server sees the selected slot,
 * the player does not, and it switches back after the given ticks.
 */
object Slots {

    @JvmStatic
    fun findHotbarSlot(item: Item): HotbarItemSlot? = AllSlots.Hotbar.findSlot(item)

    @JvmStatic
    fun findHotbarSlot(predicate: Predicate<ItemStack>): HotbarItemSlot? = AllSlots.Hotbar.findSlot(predicate)

    /** Hotbar first, then the inventory. */
    @JvmStatic
    fun findSlot(item: Item): ItemSlot? = AllSlots.HotbarAndInventory.findSlot(item)

    @JvmStatic
    fun findSlot(predicate: Predicate<ItemStack>): ItemSlot? = AllSlots.HotbarAndInventory.findSlot(predicate)

    /** The hotbar slot the server thinks is selected. */
    @JvmStatic
    val serverSideSlot: Int
        get() = SilentHotbar.serversideSlot

    /**
     * @return false when another module refused the switch
     */
    @JvmStatic
    @JvmOverloads
    fun selectSilently(owner: Any, slot: HotbarItemSlot, ticks: Int = 1): Boolean =
        SilentHotbar.selectSlotSilently(owner, slot, ticks)

    @JvmStatic
    fun resetSilentSlot(owner: Any) = SilentHotbar.resetSlot(owner)

    /**
     * Selects [slot] silently and uses the item in it, in the direction the server currently sees.
     */
    @JvmStatic
    @JvmOverloads
    fun useSlot(owner: EventListener, slot: HotbarItemSlot, ticks: Int = 1): InteractionResult =
        with(owner) { useHotbarSlotOrOffhand(slot, ticks) }

    @JvmStatic
    val isInventoryOpen: Boolean
        get() = InventoryManager.isInventoryOpen

    /** Any container screen, the inventory included. */
    @JvmStatic
    val isScreenOpen: Boolean
        get() = InventoryManager.isHandledScreenOpen

}
