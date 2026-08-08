package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.GenericItemType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemFunction
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.ItemStackHolder
import net.minecraft.world.item.ItemStack

open class ItemFacet(val itemSlot: ItemSlot) : Comparable<ItemFacet>, ItemStackHolder by itemSlot {
    open val category: ItemCategory
        get() = ItemCategory(GenericItemType.ANY_ITEM, itemSlot.itemStack.item)

    open val providedItemFunctions: List<ProvidedFunction>
        get() = emptyList()

    val isInHotbar: Boolean
        get() = this.itemSlot.slotType == ItemSlot.Type.HOTBAR || this.itemSlot.slotType == ItemSlot.Type.OFFHAND

    /**
     * Should this item be kept, even if it is not allocated to any slot?
     */
    open fun shouldKeep(): Boolean = false

    override fun compareTo(other: ItemFacet): Int = compareValuesBy<ItemFacet>(this, other, ItemFacet::isInHotbar)

    /**
     * Example:
     * - Bow -> (BOW_LIKE, 1)
     * - Porkchop -> (FOOD, <amount of hunger points it regenerates>)
     *
     * @param amount The amount of the function this item gives.
     */
    data class ProvidedFunction(val type: ItemFunction, val amount: Int)
}
