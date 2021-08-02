/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoArmor
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.item.*
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.enchantment.Enchantments
import net.minecraft.fluid.LavaFluid
import net.minecraft.fluid.WaterFluid
import net.minecraft.item.*
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.math.BlockPos

/**
 * InventoryCleaner module
 *
 * Automatically throws away useless items and sorts them.
 */

object ModuleInventoryCleaner : Module("InventoryCleaner", Category.PLAYER) {

    val inventoryConstraints = InventoryConstraintsConfigurable()

    init {
        tree(inventoryConstraints)
    }

    val maxBlocks by int("MaxBlocks", 512, 0..3000)
    val maxArrows by int("MaxArrows", 256, 0..3000)

    val usefulItems = items(
        "UsefulItems",
        mutableListOf(
            Items.WATER_BUCKET,
            Items.LAVA_BUCKET,
            Items.MILK_BUCKET,
            Items.FLINT_AND_STEEL,
            Items.ENDER_PEARL,
            Items.GOLDEN_APPLE,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.ARROW,
            Items.SPECTRAL_ARROW,
            Items.TIPPED_ARROW,
            Items.POTION,
            Items.LINGERING_POTION,
            Items.SPLASH_POTION,
            Items.TRIDENT,
            Items.TNT,
            Items.ELYTRA,
        )
    )

    val isGreedy by boolean("Greedy", true)

    val offHandItem by enumChoice("OffHandItem", ItemSortChoice.SHIELD, ItemSortChoice.values())
    val slotItem1 by enumChoice("SlotItem-1", ItemSortChoice.SWORD, ItemSortChoice.values())
    val slotItem2 by enumChoice("SlotItem-2", ItemSortChoice.BOW, ItemSortChoice.values())
    val slotItem3 by enumChoice("SlotItem-3", ItemSortChoice.PICKAXE, ItemSortChoice.values())
    val slotItem4 by enumChoice("SlotItem-4", ItemSortChoice.AXE, ItemSortChoice.values())
    val slotItem5 by enumChoice("SlotItem-5", ItemSortChoice.NONE, ItemSortChoice.values())
    val slotItem6 by enumChoice("SlotItem-6", ItemSortChoice.NONE, ItemSortChoice.values())
    val slotItem7 by enumChoice("SlotItem-7", ItemSortChoice.FOOD, ItemSortChoice.values())
    val slotItem8 by enumChoice("SlotItem-8", ItemSortChoice.BLOCK, ItemSortChoice.values())
    val slotItem9 by enumChoice("SlotItem-9", ItemSortChoice.BLOCK, ItemSortChoice.values())

    val repeatable = repeatable {
        if (player.currentScreenHandler.syncId != 0) {
            return@repeatable
        }

        if (ModuleAutoArmor.locked) {
            return@repeatable
        }

        val hotbarSlotMap = getHotbarSlotMap()

        val inventory = player.inventory

        val usefulItems = hashSetOf<Int>()
        val itemsUsedInHotbar = mutableSetOf<Int>()

        val items = mutableListOf<WeightedItem>()

        (0..40).forEach {
            categoriteItem(items, inventory.getStack(it), it)
        }

        val groupedByItemCategory = items.groupBy { it.category }

        for ((key, value) in groupedByItemCategory) {
            val maxCount = when {
                key.type.allowOnlyOne -> 1
                key.type == ItemType.BLOCK -> maxBlocks
                key.type == ItemType.ARROW -> maxArrows
                else -> Int.MAX_VALUE
            }

            val hotbarSlotsToFill = hotbarSlotMap[key]

            var requiredStackCount = hotbarSlotsToFill?.size

            if (requiredStackCount == null) {
                requiredStackCount = 0
            }

            var currentStackCount = 0
            var currentItemCount = 0

            for (weightedItem in value.sortedDescending()) {
                if (currentItemCount >= maxCount && currentStackCount >= requiredStackCount) {
                    break
                }

                usefulItems.add(weightedItem.slot)

                if (hotbarSlotsToFill != null && currentStackCount < hotbarSlotsToFill.size && weightedItem.slot !in itemsUsedInHotbar) {
                    val hotbarSlotToFill = hotbarSlotsToFill[currentStackCount]

                    if ((
                        isGreedy || hotbarSlotToFill.first.satisfactionCheck?.invoke(
                                inventory.getStack(
                                        hotbarSlotToFill.second
                                    )
                            ) != true
                        ) && weightedItem.slot != hotbarSlotToFill.second
                    ) {
                        if (executeAction(weightedItem.slot, hotbarSlotToFill.second, SlotActionType.SWAP)) {
                            wait(inventoryConstraints.delay.random())

                            return@repeatable
                        }

                    }

                    itemsUsedInHotbar.add(weightedItem.slot)
                }

                currentItemCount += weightedItem.itemStack.count
                currentStackCount++
            }
        }

        for (i in 0..40) {
            if (player.inventory.getStack(i).isNothing() || i in usefulItems) {
                continue
            }

            if (executeAction(i, 1, SlotActionType.THROW)) {
                wait(inventoryConstraints.delay.random())

                return@repeatable
            }
        }
    }

    fun getUsefulItems(handledScreen: GenericContainerScreen): List<Int> {
        if (!enabled) {
            return handledScreen.screenHandler.slots.filter { !it.stack.isNothing() && it.inventory === handledScreen.screenHandler.inventory }
                .map { it.id }
        }

        val hotbarSlotMap = getHotbarSlotMap()

        val inventory = player.inventory

        val usefulItems = hashSetOf<Int>()

        val items = mutableListOf<WeightedItem>()

        (0..40).forEach {
            categoriteItem(items, inventory.getStack(it), it)
        }

        handledScreen.screenHandler.slots.forEach {
            if (it.inventory === handledScreen.screenHandler.inventory) {
                categoriteItem(items, it.stack, it.id + 41)
            }
        }

        val groupedByItemCategory = items.groupBy { it.category }

        for ((key, value) in groupedByItemCategory) {
            val maxCount = when {
                key.type.allowOnlyOne -> 1
                key.type == ItemType.BLOCK -> maxBlocks
                key.type == ItemType.ARROW -> maxArrows
                else -> Int.MAX_VALUE
            }

            val hotbarSlotsToFill = hotbarSlotMap[key]

            var requiredStackCount = hotbarSlotsToFill?.size

            if (requiredStackCount == null) {
                requiredStackCount = 0
            }

            var currentStackCount = 0
            var currentItemCount = 0

            for (weightedItem in value.sortedDescending()) {
                if (currentItemCount >= maxCount && currentStackCount >= requiredStackCount) {
                    break
                }

                usefulItems.add(weightedItem.slot)

                currentItemCount += weightedItem.itemStack.count
                currentStackCount++
            }
        }

        return usefulItems.filter { it >= 41 }.map { it - 41 }
    }

    private fun getHotbarSlotMap() = arrayOf(
        Pair(offHandItem, 40),
        Pair(slotItem1, 0),
        Pair(slotItem2, 1),
        Pair(slotItem3, 2),
        Pair(slotItem4, 3),
        Pair(slotItem5, 4),
        Pair(slotItem6, 5),
        Pair(slotItem7, 6),
        Pair(slotItem8, 7),
        Pair(slotItem9, 8),
    ).groupBy { it.first.category }

    private fun executeAction(item: Int, clickData: Int, slotActionType: SlotActionType): Boolean {
        val slot = convertClientSlotToServerSlot(item)
        val isInInventoryScreen = mc.currentScreen is InventoryScreen

        if (!(inventoryConstraints.noMove && player.moving) && (!inventoryConstraints.invOpen || isInInventoryScreen)) {
            val openInventory = inventoryConstraints.simulateInventory && !isInInventoryScreen

            if (openInventory) {
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.OPEN_INVENTORY))
            }

            interaction.clickSlot(0, slot, clickData, slotActionType, player)

            if (openInventory) {
                network.sendPacket(CloseHandledScreenC2SPacket(0))
            }

            return true
        }

        return false
    }

    private fun categoriteItem(
        items: MutableList<WeightedItem>,
        stack: ItemStack,
        slotId: Int
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
                        1
                    )
                }
                else -> {
                    if (stack.isFood) {
                        WeightedFoodItem(stack, slotId)
                    } else {
                        WeightedItem(stack, slotId)
                    }
                }
            }
        )
    }
}

val HOTBAR_PREDICATE: (o1: WeightedItem, o2: WeightedItem) -> Int =
    { o1, o2 -> compareByCondition(o1, o2, WeightedItem::isInHotbar) }
val IDENTITY_PREDICATE: (o1: WeightedItem, o2: WeightedItem) -> Int =
    { o1, o2 -> o1.itemStack.hashCode().compareTo(o2.itemStack.hashCode()) }

open class WeightedItem(val itemStack: ItemStack, val slot: Int) : Comparable<WeightedItem> {
    open val category: ItemCategory
        get() = ItemCategory(ItemType.NONE, 0)

    val isInHotbar: Boolean
        get() = isInHotbar(slot)

    open fun isSignificantlyBetter(other: WeightedItem): Boolean {
        return false
    }

    override fun compareTo(other: WeightedItem): Int = compareByCondition(this, other, WeightedItem::isInHotbar)
}

class WeightedPrimitiveItem(itemStack: ItemStack, slot: Int, override val category: ItemCategory, val worth: Int = 0) :
    WeightedItem(itemStack, slot) {
    companion object {
        private val COMPARATOR = ComparatorChain<WeightedPrimitiveItem>(
            { o1, o2 -> o1.worth.compareTo(o2.worth) },
            { o1, o2 -> o1.itemStack.count.compareTo(o2.itemStack.count) },
            HOTBAR_PREDICATE,
            IDENTITY_PREDICATE
        )
    }

    override fun compareTo(other: WeightedItem): Int = COMPARATOR.compare(this, other as WeightedPrimitiveItem)
}

class WeightedArmorItem(itemStack: ItemStack, slot: Int) : WeightedItem(itemStack, slot) {
    private val armorPiece = ArmorPiece(itemStack, slot)

    override val category: ItemCategory
        get() = ItemCategory(ItemType.ARMOR, armorPiece.entitySlotId)

    override fun compareTo(other: WeightedItem): Int =
        ArmorComparator.compare(this.armorPiece, (other as WeightedArmorItem).armorPiece)
}

class WeightedSwordItem(itemStack: ItemStack, slot: Int) : WeightedItem(itemStack, slot) {
    companion object {
        val DAMAGE_ESTIMATOR = EnchantmentValueEstimator(
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SHARPNESS, 0.5f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SMITE, 2.0f * 0.05f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.BANE_OF_ARTHROPODS, 2.0f * 0.05f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.KNOCKBACK, 0.75f),
        )
        val SECONDARY_VALUE_ESTIMATOR = EnchantmentValueEstimator(
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.LOOTING, 0.05f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.05f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.VANISHING_CURSE, -0.1f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SWEEPING, 0.2f),
        )
        private val COMPARATOR = ComparatorChain<WeightedSwordItem>(
            { o1, o2 ->
                (
                    // TODO: Attack Speed
                    o1.itemStack.item.attackDamage * (1.0f + DAMAGE_ESTIMATOR.estimateValue(o1.itemStack)) + o1.itemStack.getEnchantment(
                        Enchantments.FIRE_ASPECT
                    ) * 4.0f * 0.625f * 0.9f
                    ).compareTo(
                    o2.itemStack.item.attackDamage * (
                        1.0f + DAMAGE_ESTIMATOR.estimateValue(
                            o2.itemStack
                        ) + o2.itemStack.getEnchantment(Enchantments.FIRE_ASPECT) * 4.0f * 0.625f * 0.9f
                        )
                )
            },
            { o1, o2 ->
                SECONDARY_VALUE_ESTIMATOR.estimateValue(o1.itemStack)
                    .compareTo(SECONDARY_VALUE_ESTIMATOR.estimateValue(o2.itemStack))
            },
            { o1, o2 -> compareByCondition(o1, o2) { it.itemStack.item is SwordItem } },
            { o1, o2 -> o1.itemStack.item.enchantability },
            HOTBAR_PREDICATE,
            IDENTITY_PREDICATE
        )
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.SWORD, 0)

    override fun compareTo(other: WeightedItem): Int {
        return COMPARATOR.compare(this, other as WeightedSwordItem)
    }
}

class WeightedBowItem(itemStack: ItemStack, slot: Int) : WeightedItem(itemStack, slot) {
    companion object {
        val VALUE_ESTIMATOR = EnchantmentValueEstimator(
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.POWER, 0.25f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.PUNCH, 0.33f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.FLAME, 4.0f * 0.9f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.INFINITY, 4.0f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.1f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.VANISHING_CURSE, -0.1f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.MENDING, -0.2f),
        )
        private val COMPARATOR = ComparatorChain<WeightedBowItem>(
            { o1, o2 ->
                (VALUE_ESTIMATOR.estimateValue(o1.itemStack)).compareTo(
                    VALUE_ESTIMATOR.estimateValue(o2.itemStack)
                )
            },
            HOTBAR_PREDICATE,
            IDENTITY_PREDICATE
        )
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.BOW, 0)

    override fun compareTo(other: WeightedItem): Int {
        return COMPARATOR.compare(this, other as WeightedBowItem)
    }
}

class WeightedCrossbowItem(itemStack: ItemStack, slot: Int) : WeightedItem(itemStack, slot) {
    companion object {
        val VALUE_ESTIMATOR = EnchantmentValueEstimator(
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.QUICK_CHARGE, 0.2f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.MULTISHOT, 1.5f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.PIERCING, 1.0f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.MENDING, 0.2f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.1f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.VANISHING_CURSE, -0.25f),
        )
        private val COMPARATOR = ComparatorChain<WeightedCrossbowItem>(
            { o1, o2 ->
                (VALUE_ESTIMATOR.estimateValue(o1.itemStack)).compareTo(
                    VALUE_ESTIMATOR.estimateValue(o2.itemStack)
                )
            },
            HOTBAR_PREDICATE,
            IDENTITY_PREDICATE
        )
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.CROSSBOW, 0)

    override fun compareTo(other: WeightedItem): Int {
        return COMPARATOR.compare(this, other as WeightedCrossbowItem)
    }
}

class WeightedArrowItem(itemStack: ItemStack, slot: Int) : WeightedItem(itemStack, slot) {
    companion object {
        private val COMPARATOR = ComparatorChain<WeightedArrowItem>(
            { o1, o2 ->
                o1.itemStack.count.compareTo(o2.itemStack.count)
            },
            HOTBAR_PREDICATE,
            IDENTITY_PREDICATE
        )
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.ARROW, 0)

    override fun compareTo(other: WeightedItem): Int {
        return COMPARATOR.compare(this, other as WeightedArrowItem)
    }
}

class WeightedToolItem(itemStack: ItemStack, slot: Int) : WeightedItem(itemStack, slot) {
    companion object {
        val VALUE_ESTIMATOR = EnchantmentValueEstimator(
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SILK_TOUCH, 1.0f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.2f),
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.FORTUNE, 0.33f),
        )
        private val COMPARATOR = ComparatorChain<WeightedToolItem>(
            { o1, o2 ->
                (o1.itemStack.item as ToolItem).material.miningLevel.compareTo((o2.itemStack.item as ToolItem).material.miningLevel)
            },
            { o1, o2 ->
                VALUE_ESTIMATOR.estimateValue(o1.itemStack).compareTo(VALUE_ESTIMATOR.estimateValue(o2.itemStack))
            },
            HOTBAR_PREDICATE,
            IDENTITY_PREDICATE
        )
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.TOOL, (this.itemStack.item as ToolItem).type)

    override fun compareTo(other: WeightedItem): Int {
        return COMPARATOR.compare(this, other as WeightedToolItem)
    }
}

class WeightedRodItem(itemStack: ItemStack, slot: Int) : WeightedItem(itemStack, slot) {
    companion object {
        val VALUE_ESTIMATOR = EnchantmentValueEstimator(
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.4f),
        )
        private val COMPARATOR = ComparatorChain<WeightedRodItem>(
            { o1, o2 ->
                VALUE_ESTIMATOR.estimateValue(o1.itemStack).compareTo(VALUE_ESTIMATOR.estimateValue(o2.itemStack))
            },
            HOTBAR_PREDICATE,
            IDENTITY_PREDICATE
        )
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.ROD, 0)

    override fun compareTo(other: WeightedItem): Int {
        return COMPARATOR.compare(this, other as WeightedRodItem)
    }
}

class WeightedShieldItem(itemStack: ItemStack, slot: Int) : WeightedItem(itemStack, slot) {
    companion object {
        val VALUE_ESTIMATOR = EnchantmentValueEstimator(
            EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.4f),
        )
        private val COMPARATOR = ComparatorChain<WeightedShieldItem>(
            { o1, o2 ->
                VALUE_ESTIMATOR.estimateValue(o1.itemStack).compareTo(VALUE_ESTIMATOR.estimateValue(o2.itemStack))
            },
            HOTBAR_PREDICATE,
            IDENTITY_PREDICATE
        )
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.SHIELD, 0)

    override fun compareTo(other: WeightedItem): Int {
        return COMPARATOR.compare(this, other as WeightedShieldItem)
    }
}

class WeightedFoodItem(itemStack: ItemStack, slot: Int) : WeightedItem(itemStack, slot) {
    companion object {
        private val COMPARATOR = ComparatorChain<WeightedFoodItem>(
            { o1, o2 -> compareByCondition(o1, o2) { it.itemStack.item == Items.ENCHANTED_GOLDEN_APPLE } },
            { o1, o2 -> compareByCondition(o1, o2) { it.itemStack.item == Items.GOLDEN_APPLE } },
            { o1, o2 -> o1.itemStack.item.foodComponent!!.hunger.compareTo(o2.itemStack.item.foodComponent!!.hunger) },
            { o1, o2 -> o1.itemStack.item.foodComponent!!.saturationModifier.compareTo(o2.itemStack.item.foodComponent!!.saturationModifier) },
            { o1, o2 ->
                o1.itemStack.count.compareTo(o2.itemStack.count)
            },
            HOTBAR_PREDICATE,
            IDENTITY_PREDICATE
        )
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.FOOD, 0)

    override fun compareTo(other: WeightedItem): Int {
        return COMPARATOR.compare(this, other as WeightedFoodItem)
    }
}

class WeightedBlockItem(itemStack: ItemStack, slot: Int) : WeightedItem(itemStack, slot) {
    companion object {
        private val COMPARATOR = ComparatorChain<WeightedBlockItem>(
            { o1, o2 ->
                compareByCondition(
                    o1,
                    o2
                ) { (it.itemStack.item as BlockItem).block.defaultState.material.isSolid }
            },
            { o1, o2 ->
                compareByCondition(
                    o1,
                    o2
                ) { (it.itemStack.item as BlockItem).block.defaultState.isFullCube(mc.world, BlockPos(0, 0, 0)) }
            },
            { o1, o2 -> o1.itemStack.count.compareTo(o2.itemStack.count) },
            HOTBAR_PREDICATE,
            IDENTITY_PREDICATE
        )
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.BLOCK, 0)

    override fun compareTo(other: WeightedItem): Int {
        return COMPARATOR.compare(this, other as WeightedBlockItem)
    }
}

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
    NONE(false)
}

enum class ItemSortChoice(
    override val choiceName: String,
    val category: ItemCategory?,
    val satisfactionCheck: ((ItemStack) -> Boolean)? = null
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
        { it.item == Items.GOLDEN_APPLE || it.item == Items.ENCHANTED_GOLDEN_APPLE }
    ),
    FOOD("Food", ItemCategory(ItemType.FOOD, 0), { it.item.foodComponent != null }),
    BLOCK("Block", ItemCategory(ItemType.BLOCK, 0), { it.item is BlockItem }),
    IGNORE("Ignore", null),
    NONE("None", null)
}
