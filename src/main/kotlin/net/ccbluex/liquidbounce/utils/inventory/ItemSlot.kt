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
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import java.util.*

/**
 * Represents an inventory slot (e.g. Hotbar Slot 0, OffHand, Chestslot 5, etc.)
 */
abstract class ItemSlot {
    abstract val itemStack: ItemStack
    abstract val slotType: ItemSlotType

    /**
     * Used for example for slot click packets
     */
    abstract fun getIdForServer(screen: GenericContainerScreen?): Int?

    fun getIdForServerWithCurrentScreen() = getIdForServer(mc.currentScreen as? GenericContainerScreen)

    abstract override fun hashCode(): Int

    abstract override fun equals(other: Any?): Boolean
}

/**
 * @param id the id this slot is identified by. Two virtual slots that have the same id are considered equal.
 */
class VirtualItemSlot(
    override val itemStack: ItemStack,
    override val slotType: ItemSlotType,
    val id: Int
): ItemSlot() {
    override fun getIdForServer(screen: GenericContainerScreen?): Int? {
        throw NotImplementedError()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VirtualItemSlot

        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }

}

class ContainerItemSlot(val slotInContainer: Int) : ItemSlot() {
    private val screen: GenericContainerScreen
        get() = mc.currentScreen as GenericContainerScreen
    override val itemStack: ItemStack
        get() = this.screen.screenHandler.slots[this.slotInContainer].stack

    override val slotType: ItemSlotType
        get() = ItemSlotType.CONTAINER

    override fun getIdForServer(screen: GenericContainerScreen?): Int = this.slotInContainer

    fun distance(itemSlot: ContainerItemSlot): Int {
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
}

private fun GenericContainerScreen.itemCount() = this.screenHandler.rows * 9

open class HotbarItemSlot(val hotbarSlot: Int) : ItemSlot() {

    override val itemStack: ItemStack
        get() = player.inventory.getStack(this.hotbarSlot)

    override val slotType: ItemSlotType
        get() = ItemSlotType.HOTBAR

    open val hotbarSlotForServer: Int = hotbarSlot

    /**
     * If the player is holding this slot (main hand stack)
     */
    val isSelected: Boolean
        get() = hotbarSlotForServer == player.inventory.selectedSlot

    open val useHand = Hand.MAIN_HAND

    override fun getIdForServer(screen: GenericContainerScreen?): Int? {
        return if (screen == null) 36 + hotbarSlot else screen.itemCount() + 27 + this.hotbarSlot
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
        return "HotbarItemSlot(hotbarSlot=$hotbarSlot, itemStack=$itemStack)"
    }

}

class InventoryItemSlot(private val inventorySlot: Int) : ItemSlot() {
    override val itemStack: ItemStack
        get() = player.inventory.getStack(9 + this.inventorySlot)

    override val slotType: ItemSlotType
        get() = ItemSlotType.INVENTORY

    override fun getIdForServer(screen: GenericContainerScreen?): Int {
        return if (screen == null) 9 + inventorySlot else screen.itemCount() + this.inventorySlot
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
}

class ArmorItemSlot(private val armorType: Int) : ItemSlot() {
    override val itemStack: ItemStack
        get() = player.inventory.armor[this.armorType]

    override val slotType: ItemSlotType
        get() = ItemSlotType.ARMOR

    override fun getIdForServer(screen: GenericContainerScreen?) = if (screen == null) 8 - this.armorType else null

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

object OffHandSlot : HotbarItemSlot(-1) {
    override val itemStack: ItemStack
        get() = player.offHandStack

    override val slotType: ItemSlotType
        get() = ItemSlotType.OFFHAND

    override val hotbarSlotForServer: Int = 40

    override val useHand = Hand.OFF_HAND

    override fun getIdForServer(screen: GenericContainerScreen?) = if (screen == null) 45 else null

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return this.javaClass.hashCode()
    }
}
