package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType

/**
 * Specialization of weapon type. Used in order to allow the user to specify that they want a sword and not an axe
 * or something.
 */
class SwordItemFacet(itemSlot: ItemSlot) : WeaponItemFacet(itemSlot) {
    override val category: ItemCategory
        get() = ItemCategory(ItemType.SWORD, 0)
}
