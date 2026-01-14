package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.utils.inventory.ItemSlot

class CleanupPlanTemplate(
    /**
     * Contains requests for each slot (e.g. Slot 1 -> SWORD, Slot 8 -> BLOCK, etc.)
     */
    val slotContentMap: Map<ItemSlot, CleanupPlanSlotContent>,
    /**
     * A function which provides constraint groups for each item category and the number which the item counts against
     * the given constraint. More info on how constraints work at [ItemNumberContraintGroup].
     */
    val itemAmountConstraintProvider: ItemAmountConstraintProvider,
    /**
     * See [CleanupPlanRestrictions]
     */
    val restrictions: CleanupPlanRestrictions,
) {

    class CleanupPlanSlotContent(
        /**
         * Content wishes for the target slot.
         *
         * ## Example:
         * - Configuration for the slot: `[(sword), (snowball, egg), (apple)]`
         * - Available items: `[1x sword, 3x snowball, 16x egg, 64x apple]`
         *
         * Behaviour:
         * 1. The slot would be filled in
         * 2. If the sword wasn't available, the next wish is considered.
         * So it searches for the best snowball or egg in the list.
         * Since 16 eggs are better than 3 snowballs, it will prefer those.
         * 3. If the eggs weren't available or the snowballs were more, it would fill the slot with the snowball stack.
         * 4. If no eggs and snowballs are available either, the apples would be filled in.
         */
        val slotContentPreferences: List<SlotContentPreference>,
        val priority: Int,
    )

    data class SlotContentPreference(
        val itemType: GenericItemType,
        val subtypes: Set<Any> = setOf(Unit),
    )

    /**
     * Contains all information about what the inv cleaner is *not allowed* to do.
     */
    class CleanupPlanRestrictions(
        private val slotRestrictionMap: Map<ItemSlot, RestrictionType>,
    ) {

        fun getRestrictionFor(slot: ItemSlot): RestrictionType {
            return this.slotRestrictionMap.getOrDefault(slot, RestrictionType.NONE)
        }

        fun getSlotsWithAtLeast(type: RestrictionType): List<ItemSlot> {
            return this.slotRestrictionMap.entries
                .filter { it.value >= type }
                .map { it.key }
        }

        enum class RestrictionType {
            NONE,

            /**
             * Forbids the inventory cleaner from replacing the item in that slot with another item according to
             * the current template.
             * The inventory cleaner may still decide that the current content of the slot is useless and throw it out.
             *
             * Used for preventing the replacement of items that
             * [net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand] placed in the offhand.
             */
            FORBID_REPLACING,

            /**
             * Prevents the invcleaner from touching those slots at all.
             *
             * This used to be user-configurable for specific hotbar slots.
             * Currently, this is used to prevent inv cleaner from tampering with the armor slots.
             */
            FORBID_TAMPERING
        }
    }
}
