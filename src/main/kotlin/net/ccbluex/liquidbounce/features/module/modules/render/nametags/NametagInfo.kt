package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import net.ccbluex.liquidbounce.utils.item.isNothing
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack

class NametagInfo(
    /**
     * The text to render as nametag
     */
    val text: String,
    /**
     * The items that should be rendered above the name tag
     */
    val items: List<ItemStack?>,
) {
    companion object {
        fun createForEntity(entity: Entity): NametagInfo {
            val text = NametagTextFormatter(entity).format()
            val items = createItemList(entity)

            return NametagInfo(text, items)
        }

        /**
         * Creates a list of items that should be rendered above the name tag. Currently, it is the item in main hand,
         * the item in off-hand (as long as it exists) and the armor items.
         */
        private fun createItemList(entity: Entity): List<ItemStack?> {
            val itemIterator = entity.handItems.iterator()

            val firstHandItem = itemIterator.next()
            val secondHandItem = itemIterator.next()

            val armorItems = entity.armorItems

            val heldItems =
                if (secondHandItem.isNothing()) {
                    listOf(firstHandItem)
                } else {
                    listOf(firstHandItem, secondHandItem)
                }

            return heldItems + armorItems
        }
    }
}
