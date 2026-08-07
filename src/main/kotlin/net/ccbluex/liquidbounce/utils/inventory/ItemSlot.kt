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

import net.ccbluex.fastutil.asObjectList
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_15_2
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.item.ItemStackHolder
import net.ccbluex.liquidbounce.utils.item.PreferStackSize
import net.ccbluex.liquidbounce.utils.item.asHolderComparator
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.item.ItemStack
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

    fun getIdForServerWithCurrentScreen() = getIdForServer(mc.gui.screen() as? AbstractContainerScreen<*>)

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

    override val itemStack: ItemStack
        get() = (mc.gui.screen() as AbstractContainerScreen<*>).menu.slots[this.slotInContainer].item

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
        return this.javaClass.hashCode() * 31 + this.slotInContainer
    }

    override fun toString(): String = "ItemSlot/Container(slotInContainer=$slotInContainer)"
}

private fun AbstractContainerScreen<*>.itemCount() = this.menu.slots.size

enum class HotbarItemSlot(
    /**
     * Vanilla hotbar selection index, i.e. [Inventory.selected] / [Inventory.getSelectedSlot].
     *
     * Main-hand hotbar entries use `0..8`. Offhand has no such selected index in vanilla,
     * so [OFFHAND] uses `null`.
     */
    val hotbarIndex: Int?
) : ItemSlot {
    OFFHAND(null),
    SLOT_0(0),
    SLOT_1(1),
    SLOT_2(2),
    SLOT_3(3),
    SLOT_4(4),
    SLOT_5(5),
    SLOT_6(6),
    SLOT_7(7),
    SLOT_8(8);

    /**
     * Vanilla player-inventory index used by [Inventory.getItem].
     *
     * Main-hand hotbar stays `0..8`, while offhand maps to [Inventory.SLOT_OFFHAND] (`40`).
     */
    val inventorySlot: Int
        get() = hotbarIndex ?: Inventory.SLOT_OFFHAND

    override val itemStack: ItemStack
        get() = player.inventory.getItem(inventorySlot)

    /**
     * Whether this entry represents vanilla offhand instead of one of the nine selectable hotbar slots.
     */
    val isOffHand: Boolean
        get() = hotbarIndex == null

    val canBeSwapTarget: Boolean
        get() = !isOffHand || !isOlderThanOrEqual1_15_2

    /**
     * Vanilla [InteractionHand] corresponding to this slot when performing item use / interaction logic.
     */
    val useHand: InteractionHand
        get() = if (isOffHand) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND

    override val slotType: ItemSlot.Type
        get() = if (isOffHand) ItemSlot.Type.OFFHAND else ItemSlot.Type.HOTBAR

    /**
     * Vanilla slot id inside the player's own [InventoryMenu]:
     * hotbar uses `36..44`, offhand uses [InventoryMenu.SHIELD_SLOT].
     */
    private val playerInventoryMenuSlot: Int
        get() = if (hotbarIndex == null) InventoryMenu.SHIELD_SLOT else Inventory.INVENTORY_SIZE + hotbarIndex

    /**
     * If the player is holding this slot (main hand stack, or offhand stack)
     */
    val isSelected: Boolean
        get() = isOffHand || hotbarIndex == player.inventory.selectedSlot

    override fun getIdForServer(screen: AbstractContainerScreen<*>?): Int? {
        return when {
            isOffHand && isOlderThanOrEqual1_8 -> null
            screen == null -> playerInventoryMenuSlot
            hotbarIndex != null -> screen.itemCount() - Inventory.SELECTION_SIZE + hotbarIndex
            else -> null
        }
    }

    companion object {

        /**
         * Entries corresponding to vanilla selectable hotbar slots `0..8`, excluding offhand.
         */
        @JvmStatic
        val mainHandSlots: List<HotbarItemSlot> = entries.subList(1, 1 + Inventory.SELECTION_SIZE)

        @JvmStatic
        @JvmName("of")
        operator fun invoke(hotbarIndex: Int): HotbarItemSlot {
            return mainHandSlots.getOrNull(hotbarIndex) ?: error("Invalid hotbar index: $hotbarIndex")
        }

        /**
         * Distance order:
         * current hand -> offhand -> other slots
         */
        @JvmField
        val PREFER_NEARBY: Comparator<HotbarItemSlot> = Comparator.comparingInt {
            val selected = SilentHotbar.serversideSlot
            when (val hotbarIndex = it.hotbarIndex) {
                // Offhand
                null -> Int.MIN_VALUE + 1
                // Selected
                selected -> Int.MIN_VALUE
                // Other
                else -> abs(selected - hotbarIndex)
            }
        }
    }
}

class InventoryItemSlot private constructor(private val inventorySlot: Int) : ItemSlot {

    override val itemStack: ItemStack
        get() = player.inventory.getItem(Inventory.SELECTION_SIZE + this.inventorySlot)

    override val slotType: ItemSlot.Type
        get() = ItemSlot.Type.INVENTORY

    override fun getIdForServer(screen: AbstractContainerScreen<*>?): Int {
        return if (screen == null) {
            Inventory.SELECTION_SIZE + inventorySlot
        } else {
            screen.itemCount() - Inventory.INVENTORY_SIZE + this.inventorySlot
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InventoryItemSlot

        return inventorySlot == other.inventorySlot
    }

    override fun hashCode(): Int {
        return this.javaClass.hashCode() * 31 + inventorySlot
    }

    override fun toString(): String = "ItemSlot/Inventory(inventorySlot=$inventorySlot)"

    companion object {
        @JvmField
        val ALL: List<InventoryItemSlot> =
            Array(Inventory.INVENTORY_SIZE - Inventory.SELECTION_SIZE, ::InventoryItemSlot).asObjectList()

        @JvmStatic
        @JvmName("of")
        operator fun invoke(inventorySlot: Int): InventoryItemSlot {
            return ALL.getOrNull(inventorySlot) ?: error("Invalid inventory slot: $inventorySlot")
        }
    }
}

enum class ArmorItemSlot(@JvmField val equipmentSlot: EquipmentSlot) : ItemSlot {
    FEET(EquipmentSlot.FEET), // 0
    LEGS(EquipmentSlot.LEGS), // 1
    CHEST(EquipmentSlot.CHEST), // 2
    HEAD(EquipmentSlot.HEAD); // 3

    override val itemStack: ItemStack
        get() = player.getItemBySlot(equipmentSlot)

    override val slotType: ItemSlot.Type
        get() = ItemSlot.Type.ARMOR

    override fun getIdForServer(screen: AbstractContainerScreen<*>?) =
        if (screen == null) 8 - this.equipmentSlot.index else null

    companion object {
        @JvmStatic
        @JvmName("of")
        operator fun invoke(equipmentSlot: EquipmentSlot): ArmorItemSlot {
            require(equipmentSlot.type == EquipmentSlot.Type.HUMANOID_ARMOR) {
                "Slot type should be ${EquipmentSlot.Type.HUMANOID_ARMOR}"
            }
            return entries[equipmentSlot.index]
        }
    }
}
