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
package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlotType
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import java.util.*
import kotlin.math.abs

/**
 * Represents an inventory slot (e.g. Hotbar Slot 0, OffHand, Chestslot 5, etc.)
 */
sealed interface ItemSlot {
    val itemStack: ItemStack
    val slotType: ItemSlotType

    /**
     * Used for example for slot click packets
     */
    fun getIdForServer(screen: HandledScreen<*>?): Int?

    fun getIdForServerWithCurrentScreen() = getIdForServer(mc.currentScreen as? HandledScreen<*>)

    override fun hashCode(): Int

    override fun equals(other: Any?): Boolean
}

/**
 * @param id the id this slot is identified by. Two virtual slots that have the same id are considered equal.
 */
class VirtualItemSlot(
    override val itemStack: ItemStack,
    override val slotType: ItemSlotType,
    val id: Int
) : ItemSlot {
    override fun getIdForServer(screen: HandledScreen<*>?): Nothing =
        throw UnsupportedOperationException("VirtualItemSlot does not have a server id")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VirtualItemSlot

        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }

    override fun toString(): String = "ItemSlot/Virtual(id=$id, itemStack=$itemStack, slotType=$slotType)"

}

class ContainerItemSlot(val slotInContainer: Int) : ItemSlot {
    private val screen: HandledScreen<*>
        get() = mc.currentScreen as HandledScreen<*>
    override val itemStack: ItemStack
        get() = this.screen.screenHandler.slots[this.slotInContainer].stack

    override val slotType: ItemSlotType
        get() = ItemSlotType.CONTAINER

    override fun getIdForServer(screen: HandledScreen<*>?): Int = this.slotInContainer

    fun distance(itemSlot: ContainerItemSlot): Int {
        // TODO: only for 9xN types
        val slotId = this.slotInContainer
        val otherId = itemSlot.slotInContainer

        val rowA = slotId / 9
        val colA = slotId % 9

        val rowB = otherId / 9
        val colB = otherId % 9

        return (colA - colB) * (colA - colB) + (rowA - rowB) * (rowA - rowB)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ContainerItemSlot

        return slotInContainer == other.slotInContainer
    }

    override fun hashCode(): Int {
        return Objects.hash(this.javaClass, slotInContainer)
    }

    override fun toString(): String = "ItemSlot/Container(slotInContainer=$slotInContainer)"
}

private fun HandledScreen<*>.itemCount() = this.screenHandler.slots.size

open class HotbarItemSlot(val hotbarSlot: Int) : ItemSlot {

    override val itemStack: ItemStack
        get() = player.inventory.getStack(this.hotbarSlot)

    override val slotType: ItemSlotType
        get() = ItemSlotType.HOTBAR

    open val hotbarSlotForServer: Int = hotbarSlot

    /**
     * If the player is holding this slot (main hand stack, or offhand stack)
     */
    open val isSelected: Boolean
        get() = hotbarSlotForServer == player.inventory.selectedSlot

    open val useHand = Hand.MAIN_HAND

    override fun getIdForServer(screen: HandledScreen<*>?): Int? {
        return if (screen == null) 36 + hotbarSlot else screen.itemCount() - 9 + this.hotbarSlot
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HotbarItemSlot

        return hotbarSlot == other.hotbarSlot
    }

    override fun hashCode(): Int {
        return Objects.hash(this.javaClass, hotbarSlot)
    }

    override fun toString(): String {
        return "ItemSlot/Hotbar(hotbarSlot=$hotbarSlot, itemStack=$itemStack)"
    }

    companion object {

        /**
         * Distance order:
         * current hand -> offhand -> other slots
         */
        @JvmField
        val PREFER_NEARBY: Comparator<HotbarItemSlot> = Comparator.comparingInt<HotbarItemSlot> {
            when {
                it is OffHandSlot -> Int.MIN_VALUE + 1
                it.hotbarSlotForServer == SilentHotbar.serversideSlot -> Int.MIN_VALUE
                else -> abs(SilentHotbar.serversideSlot - it.hotbarSlotForServer)
            }
        }
    }

}

class InventoryItemSlot(private val inventorySlot: Int) : ItemSlot {
    override val itemStack: ItemStack
        get() = player.inventory.getStack(9 + this.inventorySlot)

    override val slotType: ItemSlotType
        get() = ItemSlotType.INVENTORY

    override fun getIdForServer(screen: HandledScreen<*>?): Int {
        return if (screen == null) 9 + inventorySlot else screen.itemCount() - 36 + this.inventorySlot
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InventoryItemSlot

        return inventorySlot == other.inventorySlot
    }

    override fun hashCode(): Int {
        return Objects.hash(this.javaClass, inventorySlot)
    }

    override fun toString(): String = "ItemSlot/Inventory(inventorySlot=$inventorySlot)"
}

class ArmorItemSlot(private val armorType: Int) : ItemSlot {
    override val itemStack: ItemStack
        get() = player.inventory.armor[this.armorType]

    override val slotType: ItemSlotType
        get() = ItemSlotType.ARMOR

    override fun getIdForServer(screen: HandledScreen<*>?) = if (screen == null) 8 - this.armorType else null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArmorItemSlot

        return armorType == other.armorType
    }

    override fun hashCode(): Int {
        return Objects.hash(this.javaClass, this.armorType)
    }
}

data object OffHandSlot : HotbarItemSlot(-1) {
    override val itemStack: ItemStack
        get() = player.offHandStack

    override val slotType: ItemSlotType
        get() = ItemSlotType.OFFHAND

    override val hotbarSlotForServer: Int = 40

    /**
     * OffHand is always "selected"
     */
    override val isSelected: Boolean
        get() = true

    override val useHand = Hand.OFF_HAND

    override fun getIdForServer(screen: HandledScreen<*>?) = if (screen == null) 45 else null
}
