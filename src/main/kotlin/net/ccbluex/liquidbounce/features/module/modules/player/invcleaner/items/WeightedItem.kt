package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlotType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.item.ItemStack

open class WeightedItem(val itemSlot: ItemSlot) : Comparable<WeightedItem> {
    open val category: ItemCategory
        get() = ItemCategory(ItemType.NONE, 0)

    val itemStack: ItemStack
        get() = this.itemSlot.itemStack

    val isInHotbar: Boolean
        get() = this.itemSlot.slotType == ItemSlotType.HOTBAR || this.itemSlot.slotType == ItemSlotType.OFFHAND

    open fun isSignificantlyBetter(other: WeightedItem): Boolean {
        return false
    }

    override fun compareTo(other: WeightedItem): Int = compareByCondition(this, other, WeightedItem::isInHotbar)
}
