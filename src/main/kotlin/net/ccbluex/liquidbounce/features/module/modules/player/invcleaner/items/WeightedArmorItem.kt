package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.utils.item.ArmorComparator
import net.ccbluex.liquidbounce.utils.item.ArmorPiece

class WeightedArmorItem(itemSlot: ItemSlot) : WeightedItem(itemSlot) {
    private val armorPiece = ArmorPiece(itemSlot)

    override val category: ItemCategory
        get() = ItemCategory(ItemType.ARMOR, armorPiece.entitySlotId)

    override fun compareTo(other: WeightedItem): Int =
        ArmorComparator.compare(this.armorPiece, (other as WeightedArmorItem).armorPiece)
}
