package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.EnchantmentValueEstimator
import net.ccbluex.liquidbounce.utils.item.isAxe
import net.ccbluex.liquidbounce.utils.item.isHoe
import net.ccbluex.liquidbounce.utils.item.isPickaxe
import net.ccbluex.liquidbounce.utils.item.isShovel
import net.ccbluex.liquidbounce.utils.item.toolComponent
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.world.item.enchantment.Enchantments

class MiningToolItemFacet(itemSlot: ItemSlot) : ItemFacet(itemSlot) {
    companion object {
        val VALUE_ESTIMATOR =
            EnchantmentValueEstimator(
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SILK_TOUCH, 1.0f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.2f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.FORTUNE, 0.33f),
            )
        private val COMPARATOR =
            ComparatorChain<MiningToolItemFacet>(
                compareBy {
                    val toolComponent = it.itemStack.toolComponent ?: return@compareBy 0f
                    toolComponent.rules.firstOrNull { rule ->
                        rule.correctForDrops.orElse(false)
                    }?.speed?.orElse(null) ?: toolComponent.defaultMiningSpeed
                },
                compareBy { VALUE_ESTIMATOR.estimateValue(it.itemStack) },
                PREFER_BETTER_DURABILITY,
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )
    }

    private val subtype = ItemToolType.guessType(itemSlot.itemStack)

    override val category: ItemCategory
        get() = ItemCategory(GenericItemType.TOOL, subtype)

    override fun compareTo(other: ItemFacet): Int {
        return COMPARATOR.compare(this, other as MiningToolItemFacet)
    }

    enum class ItemToolType {
        AXE,
        PICKAXE,
        SHOVEL,
        HOE;

        companion object {
            fun guessType(stack: net.minecraft.world.item.ItemStack) = when {
                stack.isPickaxe -> PICKAXE
                stack.isAxe -> AXE
                stack.isShovel -> SHOVEL
                stack.isHoe -> HOE
                else -> error("Unknown tool item ${stack.item}.")
            }
        }
    }
}
