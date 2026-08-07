package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet

interface ItemAmountConstraintProvider {
    fun getConstraints(item: ItemFacet): ArrayList<ItemConstraintInfo>

    /**
     * Returns the priority of the given item category.
     * Categories with values are processed first.
     *
     * This is useful when it comes to finding the minimal number of items required to fulfill the constraints.
     * For example, if the constraints were `egg -> 64, egg, snowball -> 32`, it would be important to process the eggs
     * first so that no snowballs are kept when having > 32 eggs.
     */
    fun getAllocationPriority(itemGroup: ItemCategory): Int

    /**
     * Filters out not applying default configurations.
     *
     * See [ItemConstraintInfo.default] for further information on that.
     */
    fun getApplyingConstraints(item: ItemFacet): ArrayList<ItemConstraintInfo> {
        val constraints = getConstraints(item)

        if (constraints.any { !it.default }) {
            constraints.removeIf { it.default }
        }

        return constraints
    }
}
