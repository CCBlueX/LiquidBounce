@file:Suppress("WildcardImport")

package net.ccbluex.liquidbounce.features.inventoryPreset

import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot

/**
 * Represents an inventory preset configuration defining item groups for specific slots and stack limitations.
 *
 * This preset maintains a strict relationship between array indices and inventory slots:
 * - The [items] array is guaranteed to contain exactly 10 elements.
 * - Index 0 always represents the [HotbarItemSlot.OFFHAND]
 * - Indices 1-9 correspond to hotbar slots 0-8 respectively (index -1 adjustment)
 *
 * @property itemLimitRules Array of stack limitation groups applying to the entire inventory
 * @param items Initial item group configuration (must contain exactly 10 elements).
 *             Each array position maps to:
 *             - [HotbarItemSlot.OFFHAND] for index 0
 *             - [HotbarItemSlot] (0-8) for indices 1-9
 *
 * @throws IllegalArgumentException if item array size isn't exactly 10 during initialization
 */
@Suppress("MagicNumber")
class InventoryPreset(
    items: Array<List<FrontendSlotPreference>> = Array(10) { listOf() },
    val itemLimitRules: List<FrontendItemLimitRules> = emptyList()
) {
    val items: Map<HotbarItemSlot, List<FrontendSlotPreference>>

    init {
        // Required because the frontend would break if there weren't exactly 10 entries...
        require(items.size == 10)

        require(items.flatMap { it }.find { it == FrontendSlotPreference.AnySlotPreference } == null) {
            "For an item to be Any, the list must be empty."
        }

        items.forEach { preferences ->
            val ignoreCount = preferences.count { it == FrontendSlotPreference.IgnoreSlotPreference }
            require(ignoreCount == 0 || (ignoreCount == 1 && preferences.size == 1)) {
                "If you use IgnoreSlotPreference, it must be the ONLY element in the list"
            }
        }

        val itemMap = items
            .mapIndexed { index, item -> getSlotForIndex(index) to item }
            .associate { it }

        this.items = itemMap
    }

    private fun getSlotForIndex(idx: Int): HotbarItemSlot {
        return when (idx) {
            0 -> HotbarItemSlot.OFFHAND
            else -> HotbarItemSlot(idx - 1)
        }
    }

    fun itemRulesToArray(): Array<List<FrontendSlotPreference>> {
        return Array(10) {
            val preferences = items[getSlotForIndex(it)]

            if (preferences.isNullOrEmpty()) {
                return@Array listOf()
            }

            preferences
        }
    }
}
