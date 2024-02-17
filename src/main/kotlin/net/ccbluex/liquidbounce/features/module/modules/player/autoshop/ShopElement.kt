package net.ccbluex.liquidbounce.features.module.modules.player.autoshop

import net.minecraft.item.Item

class ShopElement (
    val item: Item,
    val minAmount: Int,
    val amountPerClick: Int = 1,
    val categorySlot: Int,
    val itemSlot: Int,
    val price: List<Map<Item, Int>>,
    val itemsToCheckBeforeBuying: List<Pair<Item, Int>>?
) {
    override fun toString(): String {
        return "item: $item  minAmount: $minAmount amountPerClick: $amountPerClick categorySlot: $categorySlot itemSlot: $itemSlot"
    }
}
