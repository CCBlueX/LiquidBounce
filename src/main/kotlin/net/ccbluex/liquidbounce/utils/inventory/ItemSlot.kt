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
package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.item.ItemStackHolder
import net.ccbluex.liquidbounce.utils.item.PreferStackSize
import net.ccbluex.liquidbounce.utils.item.asHolderComparator
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import java.util.Objects
import kotlin.math.abs

/**
 * Represents an inventory slot (e.g. Hotbar Slot 0, OffHand, Chestslot 5, etc.)
 */
sealed interface ItemSlot : ItemStackHolder {
    override val itemStack: ItemStack
    val slotType: Type

    /**
     * Used for example for slot click packets
     */
    fun getIdForServer(screen: AbstractContainerScreen<*>?): Int?

    fun getIdForServerWithCurrentScreen() = getIdForServer(mc.screen as? AbstractContainerScreen<*>)

    override fun hashCode(): Int

    override fun equals(other: Any?): Boolean

    companion object {

        /**
         * Distance order:
         * current hand -> offhand -> other hotbar slots -> other slots
         */
        @JvmField
        val PREFER_NEARBY: Comparator<ItemSlot> = Comparator { left, right ->
            val leftIsHotbar = left is HotbarItemSlot
            val rightIsHotbar = right is HotbarItemSlot
            when {
                leftIsHotbar && rightIsHotbar -> HotbarItemSlot.PREFER_NEARBY.compare(left, right)
                leftIsHotbar -> -1
                rightIsHotbar -> 1
                else -> 0
            }
        }

        @JvmField
        val PREFER_FEWER_ITEM: Comparator<in ItemSlot> = PreferStackSize.PREFER_FEWER.asHolderComparator()

        @JvmField
        val PREFER_MORE_ITEM: Comparator<in ItemSlot> = PreferStackSize.PREFER_MORE.asHolderComparator()
    }

    enum class Type {
        HOTBAR,
        OFFHAND,
        ARMOR,
        INVENTORY,

        /**
         * e.g. chests
         */
        CONTAINER,
    }
}

/**
 * @param id the id this slot is identified by. Two virtual slots that have the same id are considered equal.
 */
class VirtualItemSlot(
    override val itemStack: ItemStack,
    override val slotType: ItemSlot.Type,
    val id: Int
) : ItemSlot {
    override fun getIdForServer(screen: AbstractContainerScreen<*>?): Nothing =
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
    private val screen: AbstractContainerScreen<*>
        get() = mc.screen as AbstractContainerScreen<*>
    override val itemStack: ItemStack
        get() = this.screen.menu.slots[this.slotInContainer].item

    override val slotType: ItemSlot.Type
        get() = ItemSlot.Type.CONTAINER

    override fun getIdForServer(screen: AbstractContainerScreen<*>?): Int = this.slotInContainer

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

private fun AbstractContainerScreen<*>.itemCount() = this.menu.slots.size

open class HotbarItemSlot(val hotbarSlot: Int) : ItemSlot {

    override val itemStack: ItemStack
        get() = player.inventory.getItem(this.hotbarSlot)

    override val slotType: ItemSlot.Type
        get() = ItemSlot.Type.HOTBAR

    open val hotbarSlotForServer: Int = hotbarSlot

    /**
     * If the player is holding this slot (main hand stack, or offhand stack)
     */
    open val isSelected: Boolean
        get() = hotbarSlotForServer == player.inventory.selectedSlot

    open val useHand get() = InteractionHand.MAIN_HAND

    override fun getIdForServer(screen: AbstractContainerScreen<*>?): Int? {
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
        get() = player.inventory.getItem(9 + this.inventorySlot)

    override val slotType: ItemSlot.Type
        get() = ItemSlot.Type.INVENTORY

    override fun getIdForServer(screen: AbstractContainerScreen<*>?): Int {
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

class ArmorItemSlot(private val equipmentSlot: EquipmentSlot) : ItemSlot {
    override val itemStack: ItemStack
        get() = player.getItemBySlot(equipmentSlot)

    override val slotType: ItemSlot.Type
        get() = ItemSlot.Type.ARMOR

    override fun getIdForServer(screen: AbstractContainerScreen<*>?) =
        if (screen == null) 8 - this.equipmentSlot.index else null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArmorItemSlot

        return equipmentSlot == other.equipmentSlot
    }

    override fun hashCode(): Int {
        return Objects.hash(this.javaClass, this.equipmentSlot)
    }
}

data object OffHandSlot : HotbarItemSlot(-1) {
    override val itemStack: ItemStack
        get() = player.offhandItem

    override val slotType: ItemSlot.Type
        get() = ItemSlot.Type.OFFHAND

    override val hotbarSlotForServer: Int = 40

    /**
     * OffHand is always "selected"
     */
    override val isSelected: Boolean
        get() = true

    override val useHand get() = InteractionHand.OFF_HAND

    override fun getIdForServer(screen: AbstractContainerScreen<*>?) = if (screen == null) 45 else null
}
