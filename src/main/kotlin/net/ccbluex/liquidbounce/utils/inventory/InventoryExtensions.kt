/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
 *
 *
 */
package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.SlotActionType

val ScreenHandler.isPlayerInventory: Boolean
    get() = this.syncId == 0

val isInInventoryScreen
    get() = mc.currentScreen is InventoryScreen

val canCloseMainInventory
    get() = !isInInventoryScreen && mc.player?.currentScreenHandler?.isPlayerInventory == true
        && InventoryManager.isInventoryOpenServerSide

private val currentlyOpenedScreen
    get() = mc.currentScreen as? GenericContainerScreen

val GenericContainerScreen?.syncId
    get() = this?.screenHandler?.syncId ?: 0

fun ClientPlayerInteractionManager.performSwapToHotbar(
    slot: ItemSlot,
    target: HotbarItemSlot,
    screen: GenericContainerScreen? = currentlyOpenedScreen
): Boolean {
    val slotId = slot.getIdForServer(screen) ?: return false

    this.clickSlot(screen.syncId, slotId, target.hotbarSlotForServer, SlotActionType.SWAP, player)

    return true
}

fun ClientPlayerInteractionManager.performQuickMove(
    slot: ItemSlot,
    screen: GenericContainerScreen? = currentlyOpenedScreen
): Boolean {
    val slotId = slot.getIdForServer(screen) ?: return false

    this.clickSlot(screen.syncId, slotId, 0, SlotActionType.QUICK_MOVE, player)

    return true
}
fun ClientPlayerInteractionManager.performThrow(
    slot: ItemSlot,
    screen: GenericContainerScreen? = currentlyOpenedScreen
): Boolean {
    val slotId = slot.getIdForServer(screen) ?: return false

    this.clickSlot(screen.syncId, slotId, 1, SlotActionType.THROW, player)

    return true
}
