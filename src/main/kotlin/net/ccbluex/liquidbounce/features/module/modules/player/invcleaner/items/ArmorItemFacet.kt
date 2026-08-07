package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.GenericItemType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.armor.ArmorComparator
import net.ccbluex.liquidbounce.utils.item.armor.ArmorPiece

/**
 * @param stacksToKeep armor items which should be kept since they might be strong in future situations
 */
class ArmorItemFacet(
    itemSlot: ItemSlot,
    private val stacksToKeep: List<ItemSlot>,
    private val armorComparator: ArmorComparator
) : ItemFacet(itemSlot) {
    private val armorPiece = ArmorPiece(itemSlot)

    override val category: ItemCategory
        get() = ItemCategory(GenericItemType.ARMOR, armorPiece.entitySlotId)

    override fun shouldKeep(): Boolean {
        return this.stacksToKeep.contains(this.itemSlot)
    }

    override fun compareTo(other: ItemFacet): Int {
        return armorComparator.compare(this.armorPiece, (other as ArmorItemFacet).armorPiece)
    }
}
