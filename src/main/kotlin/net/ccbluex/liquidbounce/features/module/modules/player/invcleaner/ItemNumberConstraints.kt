package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

/**
 * Defines an item constraint group.
 *
 * For example if we had two constraints:
 * - `BLOCKS` -> `128..`
 * - `TNT` -> `..64`
 *
 * Imagine a situation where the player has 125 TNT:
 * - If the TNT was processed first it would be thrown out since the TNT limit says that we have too much TNT.
 * - If the BLOCKS constraint was processed first, the TNT would be kept since the BLOCKS constraint is not yet
 * satisfied.
 */
abstract class ItemNumberContraintGroup(
    /**
     * The range of desired item amounts (which might be raw item counts, food saturation, etc.):
     * - The lower limit defines the desired amount of items (=> any more items *might* be thrown out)
     * - The upper limit defines the maximum amount of items (=> any more items *will* be thrown out)
     */
    val acceptableRange: IntRange,
    /**
     * The priority of this constraint group. Lower values are processed first.
     * It Affects the order in which items are processed.
     */
    val priority: Int,
) {
    abstract override fun hashCode(): Int
    abstract override fun equals(other: Any?): Boolean
}

class ItemCategoryConstraintGroup(
    acceptableRange: IntRange,
    priority: Int,
    val category: ItemCategory,
) : ItemNumberContraintGroup(acceptableRange, priority) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemCategoryConstraintGroup

        return category == other.category
    }

    override fun hashCode(): Int {
        return category.hashCode()
    }
}

class ItemFunctionCategoryConstraintGroup(
    acceptableRange: IntRange,
    priority: Int,
    val function: ItemFunction,
) : ItemNumberContraintGroup(acceptableRange, priority) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemFunctionCategoryConstraintGroup

        return function == other.function
    }

    override fun hashCode(): Int {
        return function.hashCode()
    }
}

class ItemConstraintInfo(
    val group: ItemNumberContraintGroup,
    val amountAddedByItem: Int
)
