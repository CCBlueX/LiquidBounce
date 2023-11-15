package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeightedArmorItem
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeightedArrowItem
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeightedBlockItem
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeightedBowItem
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeightedCrossbowItem
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeightedFoodItem
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeightedItem
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeightedPrimitiveItem
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeightedRodItem
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeightedShieldItem
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeightedSwordItem
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeightedToolItem
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.fluid.LavaFluid
import net.minecraft.fluid.WaterFluid
import net.minecraft.item.ArmorItem
import net.minecraft.item.ArrowItem
import net.minecraft.item.BlockItem
import net.minecraft.item.BowItem
import net.minecraft.item.BucketItem
import net.minecraft.item.CrossbowItem
import net.minecraft.item.EnderPearlItem
import net.minecraft.item.FishingRodItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.MilkBucketItem
import net.minecraft.item.ShieldItem
import net.minecraft.item.SwordItem
import net.minecraft.item.ToolItem

val PREFER_ITEMS_IN_HOTBAR: (o1: WeightedItem, o2: WeightedItem) -> Int =
    { o1, o2 -> compareByCondition(o1, o2, WeightedItem::isInHotbar) }
val STABILIZE_COMPARISON: (o1: WeightedItem, o2: WeightedItem) -> Int =
    { o1, o2 -> o1.itemStack.hashCode().compareTo(o2.itemStack.hashCode()) }

data class ItemCategory(val type: ItemType, val subtype: Int)

enum class ItemType(val allowOnlyOne: Boolean) {
    ARMOR(true),
    SWORD(true),
    BOW(true),
    CROSSBOW(true),
    ARROW(true),
    TOOL(true),
    ROD(true),
    SHIELD(true),
    FOOD(false),
    BUCKET(false),
    PEARL(false),
    GAPPLE(false),
    BLOCK(false),
    NONE(false),
}

enum class ItemSortChoice(
    override val choiceName: String,
    val category: ItemCategory?,
    val satisfactionCheck: ((ItemStack) -> Boolean)? = null,
) : NamedChoice {
    SWORD("Sword", ItemCategory(ItemType.SWORD, 0)),
    BOW("Bow", ItemCategory(ItemType.BOW, 0)),
    CROSSBOW("Crossbow", ItemCategory(ItemType.CROSSBOW, 0)),
    AXE("Axe", ItemCategory(ItemType.TOOL, 0)),
    PICKAXE("Pickaxe", ItemCategory(ItemType.TOOL, 1)),
    ROD("Rod", ItemCategory(ItemType.ROD, 0)),
    SHIELD("Shield", ItemCategory(ItemType.SHIELD, 0)),
    WATER("Water", ItemCategory(ItemType.BUCKET, 0)),
    LAVA("Lava", ItemCategory(ItemType.BUCKET, 1)),
    MILK("Milk", ItemCategory(ItemType.BUCKET, 2)),
    PEARL("Pearl", ItemCategory(ItemType.PEARL, 0), { it.item == Items.ENDER_PEARL }),
    GAPPLE(
        "Gapple",
        ItemCategory(ItemType.GAPPLE, 0),
        { it.item == Items.GOLDEN_APPLE || it.item == Items.ENCHANTED_GOLDEN_APPLE }),
    FOOD("Food", ItemCategory(ItemType.FOOD, 0), { it.item.foodComponent != null }),
    BLOCK("Block", ItemCategory(ItemType.BLOCK, 0), { it.item is BlockItem }),
    IGNORE("Ignore", null),
    NONE("None", null),
}

object ItemCategorization {

    /**
     * Returns a list of facets an item represents. For example an axe is an axe, but also a sword:
     * - (SANDSTONE_BLOCK, 64) => `[Block(SANDSTONE_BLOCK, 64)]`
     * - (DIAMOND_AXE, 1) => `[Axe(DIAMOND_AXE, 1), Tool(DIAMOND_AXE, 1)]`
     */
    @Suppress("CyclomaticComplexMethod")
    fun getItemFacets(slot: ItemSlot): Array<WeightedItem> {
        if (slot.itemStack.isNothing()) {
            return emptyArray()
        }

        return when (val item = slot.itemStack.item) {
            is ArmorItem -> arrayOf(WeightedArmorItem(slot))
            is SwordItem -> arrayOf(WeightedSwordItem(slot))
            is BowItem -> arrayOf(WeightedBowItem(slot))
            is CrossbowItem -> arrayOf(WeightedCrossbowItem(slot))
            is ArrowItem -> arrayOf(WeightedArrowItem(slot))
            is ToolItem -> {
                arrayOf(
                    WeightedSwordItem(slot),
                    WeightedToolItem(slot)
                )
            }
            is FishingRodItem -> arrayOf(WeightedRodItem(slot))
            is ShieldItem -> arrayOf(WeightedShieldItem(slot))
            is BlockItem -> arrayOf(WeightedBlockItem(slot))
            is MilkBucketItem -> arrayOf(WeightedPrimitiveItem(slot, ItemCategory(ItemType.BUCKET, 2)))
            is BucketItem -> {
                when (item.fluid) {
                    is WaterFluid -> arrayOf(WeightedPrimitiveItem(slot, ItemCategory(ItemType.BUCKET, 0)))
                    is LavaFluid -> arrayOf(WeightedPrimitiveItem(slot, ItemCategory(ItemType.BUCKET, 1)))
                    else -> arrayOf(WeightedPrimitiveItem(slot, ItemCategory(ItemType.BUCKET, 3)))
                }
            }
            is EnderPearlItem -> arrayOf(WeightedPrimitiveItem(slot, ItemCategory(ItemType.PEARL, 0)))
            Items.GOLDEN_APPLE -> {
                arrayOf(
                    WeightedFoodItem(slot),
                    WeightedPrimitiveItem(slot, ItemCategory(ItemType.GAPPLE, 0))
                )
            }
            Items.ENCHANTED_GOLDEN_APPLE -> {
                arrayOf(
                    WeightedFoodItem(slot),
                    WeightedPrimitiveItem(slot, ItemCategory(ItemType.GAPPLE, 0), 1)
                )
            }
            else -> {
                if (slot.itemStack.isFood) {
                    arrayOf(WeightedFoodItem(slot))
                } else {
                    arrayOf(WeightedItem(slot))
                }
            }
        }
    }
}
