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
    fun categorizeItem(
        items: MutableList<WeightedItem>,
        stack: ItemStack,
        slotId: Int,
    ) {
        if (stack.isNothing()) {
            return
        }

        val item = stack.item

        items.add(
            when (item) {
                is ArmorItem -> WeightedArmorItem(stack, slotId)
                is SwordItem -> WeightedSwordItem(stack, slotId)
                is BowItem -> WeightedBowItem(stack, slotId)
                is CrossbowItem -> WeightedCrossbowItem(stack, slotId)
                is ArrowItem -> WeightedArrowItem(stack, slotId)
                is ToolItem -> {
                    items.add(WeightedSwordItem(stack, slotId))

                    WeightedToolItem(stack, slotId)
                }

                is FishingRodItem -> WeightedRodItem(stack, slotId)
                is ShieldItem -> WeightedShieldItem(stack, slotId)
                is BlockItem -> WeightedBlockItem(stack, slotId)
                is MilkBucketItem -> WeightedPrimitiveItem(stack, slotId, ItemCategory(ItemType.BUCKET, 2))
                is BucketItem -> {
                    when (item.fluid) {
                        is WaterFluid -> WeightedPrimitiveItem(stack, slotId, ItemCategory(ItemType.BUCKET, 0))
                        is LavaFluid -> WeightedPrimitiveItem(stack, slotId, ItemCategory(ItemType.BUCKET, 1))
                        else -> WeightedPrimitiveItem(stack, slotId, ItemCategory(ItemType.BUCKET, 3))
                    }
                }

                is EnderPearlItem -> WeightedPrimitiveItem(stack, slotId, ItemCategory(ItemType.PEARL, 0))
                Items.GOLDEN_APPLE -> {
                    items.add(WeightedFoodItem(stack, slotId))

                    WeightedPrimitiveItem(stack, slotId, ItemCategory(ItemType.GAPPLE, 0))
                }

                Items.ENCHANTED_GOLDEN_APPLE -> {
                    items.add(WeightedFoodItem(stack, slotId))

                    WeightedPrimitiveItem(
                        stack,
                        slotId,
                        ItemCategory(ItemType.GAPPLE, 0),
                        1,
                    )
                }

                else -> {
                    if (stack.isFood) {
                        WeightedFoodItem(stack, slotId)
                    } else {
                        WeightedItem(stack, slotId)
                    }
                }
            },
        )
    }
}