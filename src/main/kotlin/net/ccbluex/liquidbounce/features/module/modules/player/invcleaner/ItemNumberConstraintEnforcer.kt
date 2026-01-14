package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet

/**
 * This class serves two functions:
 * - Keeps track of the current state of the fulfilment of the item number limits.
 * - Decides whether an item is useful or not.
 */
class ItemNumberConstraintEnforcer(private val template: CleanupPlanTemplate) {
    private val currentLimit = HashMap<ItemNumberContraintGroup, Int>()

    /**
     * Decides whether the given item facet is useful.
     * The decision is made based on the items that have been added via [addItem]
     */
    fun getSatisfactionStatus(item: ItemFacet): SatisfactionStatus {
        val constraints = this.template.itemAmountConstraintProvider.getApplyingConstraints(item)

        constraints.sortBy { it.group.priority }

        for (constraintInfo in constraints) {
            val currentCount = this.currentLimit[constraintInfo.group] ?: 0

            if (currentCount > constraintInfo.group.acceptableRange.last) {
                return SatisfactionStatus.OVERSATURATED
            } else if (currentCount < constraintInfo.group.acceptableRange.first) {
                return SatisfactionStatus.NOT_SATISFIED
            }
        }

        return SatisfactionStatus.SATISFIED
    }

    /**
     * Called when an item is kept in the inventory.
     */
    fun addItem(item: ItemFacet) {
        val constraints = this.template.itemAmountConstraintProvider.getApplyingConstraints(item)

        for (constraintInfo in constraints) {
            val current = this.currentLimit.getOrDefault(constraintInfo.group, 0)

            this.currentLimit[constraintInfo.group] = current + constraintInfo.amountAddedByItem
        }
    }

    enum class SatisfactionStatus {
        /**
         * Keep the item
         */
        NOT_SATISFIED,

        /**
         * The item is not needed - except for filling slots.
         */
        SATISFIED,

        /**
         * The item shouldn't be kept - even if there are still slots to fill.
         */
        OVERSATURATED,
    }
}
