package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.utils.item.isInHotbar
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.item.ItemStack

open class WeightedItem(val itemStack: ItemStack, val slot: Int) : Comparable<WeightedItem> {
    open val category: ItemCategory
        get() = ItemCategory(ItemType.NONE, 0)

    val isInHotbar: Boolean
        get() = isInHotbar(slot)

    open fun isSignificantlyBetter(other: WeightedItem): Boolean {
        return false
    }

    override fun compareTo(other: WeightedItem): Int = compareByCondition(this, other, WeightedItem::isInHotbar)
}
