/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.player.cheststealer

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap
import net.ccbluex.fastutil.objectHashSetOf
import net.ccbluex.fastutil.swap
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureChestAura
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureSilentScreen
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanGenerator
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.InventoryCleanupPlan
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategorization
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeaponItemFacet
import net.ccbluex.liquidbounce.utils.inventory.CheckScreenHandlerTypeValueGroup
import net.ccbluex.liquidbounce.utils.inventory.CheckScreenTitleValueGroup
import net.ccbluex.liquidbounce.utils.inventory.ContainerItemSlot
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.InventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findItemsInContainer
import net.ccbluex.liquidbounce.utils.inventory.mergeableCapacityFor
import net.ccbluex.liquidbounce.utils.inventory.findNonEmptySlotsInInventory
import net.ccbluex.liquidbounce.utils.item.isMergeable
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.item.ItemStack
import kotlin.math.ceil
import net.ccbluex.liquidbounce.utils.kotlin.random

/**
 * ChestStealer module
 *
 * Automatically steals all items from a chest.
 */

object ModuleChestStealer : ClientModule("ChestStealer", ModuleCategories.PLAYER) {

    private val inventoryConstrains = tree(InventoryConstraints())
    private val autoClose by boolean("AutoClose", true)

    private val selectionMode = choices("SelectionMode", Distance, arrayOf(Distance, Index, Random, InvCleanerPriority))

    private val itemMoveMode by enumChoice("MoveMode", ItemMoveMode.QUICK_MOVE)
    private val quickSwaps by boolean("QuickSwaps", true)

    private val onFull by enumChoice("OnFull", OnFull.THROW)

    private enum class OnFull(override val tag: String) : Tagged {
        NONE("None"),
        THROW("Throw"),
//        PUT_BACK("PutBack"), TODO: Fix this
    }

    private val checkScreenHandlerType = tree(CheckScreenHandlerTypeValueGroup(this))
    private val checkScreenTitle = tree(CheckScreenTitleValueGroup(this))

    init {
        tree(FeatureChestAura)
        tree(FeatureSilentScreen)
    }

    @Suppress("unused")
    private val scheduleInventoryAction = handler<ScheduleInventoryActionEvent> { event ->
        // Check if we are in a chest screen
        val screen = getChestScreen() ?: return@handler

        val cleanupPlan = createCleanupPlan(screen)
        // Quick swap items in hotbar (i.e. swords), some servers hate them
        if (quickSwaps && performQuickSwaps(event, cleanupPlan, screen)) {
            return@handler
        }

        val itemsToCollect = cleanupPlan.usefulItems.filterIsInstanceTo(ArrayList<ContainerItemSlot>())

        val stillRequiredSpace = getStillRequiredSpace(cleanupPlan, itemsToCollect.size)
        selectionMode.activeMode.process(itemsToCollect)

        val targetBlacklist = objectHashSetOf<ItemSlot>()

        for (slot in itemsToCollect) {
            val moveActions = Slots.HotbarAndInventory.findPossiblePickActions(screen, slot, targetBlacklist)

            if (moveActions != null) {
                event.schedule(
                    inventoryConstrains, moveActions,
                    /**
                     * we prioritize item based on how important it is
                     * for example we should prioritize armor over apples
                     */
                    ItemCategorization.Default.getItemFacets(slot).maxOf { it.category.type.allocationPriority }
                )
            } else if (stillRequiredSpace > 0) {
                // Throw useless items
                event.schedule(
                    inventoryConstrains,
                    throwItem(cleanupPlan, screen, targetBlacklist) ?: break
                )
            }
        }

        // Check if stealing the chest was completed
        if (autoClose && itemsToCollect.isEmpty()) {
            event.schedule(inventoryConstrains, InventoryAction.CloseScreen(screen))
        }
    }

    /**
     * Gets the clicks from mergeable or empty slots, or null if impossible to pick
     */
    @Suppress("CognitiveComplexMethod")
    private fun Iterable<ItemSlot>.findPossiblePickActions(
        screen: AbstractContainerScreen<*>,
        from: ItemSlot,
        targetBlacklist: MutableSet<ItemSlot>? = null,
    ): List<InventoryAction.Click>? {
        val fromStack = from.itemStack
        val remaining = mergeableCapacityFor(fromStack, blacklist = targetBlacklist)

        // Impossible to pick any item into inventory
        if (remaining == 0) return null

        targetBlacklist?.add(from)
        return when (itemMoveMode) {
            ItemMoveMode.QUICK_MOVE -> listOf(InventoryAction.Click.performQuickMove(screen, from))

            ItemMoveMode.DRAG_AND_DROP -> {
                // Never empty
                val targets = filterTo(ArrayDeque()) {
                    (targetBlacklist == null || it !in targetBlacklist) &&
                        (it.itemStack.isEmpty || it.itemStack.isMergeable(fromStack))
                }

                /* The remaining count after merged with [fromStack]. Negative -> fromStack has remaining */
                fun mergedRemaining(target: ItemStack) = fromStack.maxStackSize - fromStack.count - target.count

                buildList {
                    // Pick up
                    this += InventoryAction.Click.performPickup(screen, from)

                    val possibleSinglePut = targets.firstOrNull { mergedRemaining(it.itemStack) >= 0 }
                    if (possibleSinglePut != null) {
                        this += InventoryAction.Click.performPickup(screen, possibleSinglePut)
                        targetBlacklist?.add(possibleSinglePut)
                    } else {
                        // Now all `mergedRemaining` result of [targets] are negative
                        // Minimize click count
                        targets.sortBy { mergedRemaining(it.itemStack) }
                        var count = fromStack.count
                        while (count >= 0) {
                            val target = targets.removeFirstOrNull() ?: break
                            count += mergedRemaining(target.itemStack)
                            this += InventoryAction.Click.performPickup(screen, target)
                            targetBlacklist?.add(target)
                        }
                    }

                    if (remaining < fromStack.count) {
                        // Unable to take all, put remaining items back
                        this += InventoryAction.Click.performPickup(screen, from)
                    }
                }
            }
        }
    }

    /**
     * @return if we should wait
     */
    private fun throwItem(
        cleanupPlan: InventoryCleanupPlan,
        screen: AbstractContainerScreen<*>,
        targetBlacklist: MutableSet<ItemSlot>,
    ): List<InventoryAction>? {
        val itemsInInv = findNonEmptySlotsInInventory()
        val itemToThrowOut = cleanupPlan.findItemsToThrowOut(itemsInInv)
            .firstOrNull { it.getIdForServer(screen) != null } ?: return null

        return when (onFull) {
            OnFull.NONE -> null
//            OnFull.PUT_BACK -> screen.getSlotsInContainer()
//                .findPossiblePickActions(screen, itemToThrowOut, targetBlacklist)
            OnFull.THROW -> {
                targetBlacklist.add(itemToThrowOut)
                listOf(InventoryAction.Click.performThrow(screen, itemToThrowOut))
            }
        }
    }

    /**
     * @param slotsToCollect amount of items we need to take
     */
    private fun getStillRequiredSpace(
        cleanupPlan: InventoryCleanupPlan,
        slotsToCollect: Int,
    ): Int {
        val freeSlotsInInv = Slots.HotbarAndInventory.count { it.itemStack.isEmpty }

        val spaceGainedThroughMerge = cleanupPlan.mergeableItems.entries.sumOf { (id, slots) ->
            val slotsInChest = slots.count { it.slotType == ItemSlot.Type.CONTAINER }
            val totalCount = slots.sumOf { it.itemStack.count }

            val mergedStackCount = ceil(totalCount.toDouble() / id.item.defaultMaxStackSize.toDouble()).toInt()

            (slots.size - mergedStackCount).coerceAtMost(slotsInChest)
        }

        return (slotsToCollect - freeSlotsInInv - spaceGainedThroughMerge).coerceAtLeast(0)
    }

    /**
     * @return true if a quick swap transaction was scheduled and the plan should be regenerated next tick
     */
    @Suppress("CognitiveComplexMethod")
    private fun performQuickSwaps(
        event: ScheduleInventoryActionEvent,
        cleanupPlan: InventoryCleanupPlan,
        screen: AbstractContainerScreen<*>
    ): Boolean {
        cleanupPlan.swaps.forEach { hotbarSwap ->
            // We only care about swaps from the chest to the hotbar
            if (hotbarSwap.from.slotType != ItemSlot.Type.CONTAINER) {
                return@forEach
            }

            val hotbarSlot = hotbarSwap.to as? HotbarItemSlot ?: return@forEach
            if (!hotbarSlot.canBeSwapTarget) {
                return@forEach
            }

            val actions = when {
                // Target slot is empty, swap
                hotbarSlot.itemStack.isEmpty -> listOf(
                    InventoryAction.Click.performSwap(screen, hotbarSwap.from, hotbarSlot)
                )

                // Target slot item is useful, swap it with another empty slot
                hotbarSlot in cleanupPlan.usefulItems ->
                    Slots.Inventory.firstOrNull { it.itemStack.isEmpty }?.let { emptyInventorySlot ->
                        listOf(
                            InventoryAction.Click.performSwap(screen, emptyInventorySlot, hotbarSlot),
                            InventoryAction.Click.performSwap(screen, hotbarSwap.from, hotbarSlot),
                        )
                    } ?: return@forEach

                // Target slot item is useless, throw and swap
                else -> if (hotbarSlot.isOffHand) {
                    // Throwing offhand item inside container looks not legit
                    listOf(InventoryAction.Click.performSwap(screen, hotbarSwap.from, hotbarSlot))
                } else {
                    listOf(
                        InventoryAction.Click.performThrow(screen, hotbarSlot),
                        InventoryAction.Click.performSwap(screen, hotbarSwap.from, hotbarSlot),
                    )
                }
            }

            if (actions.any { it.slot.getIdForServer(screen) == null }) {
                return@forEach
            }

            event.schedule(
                inventoryConstrains,
                actions,
                /**
                 * we prioritize item based on how important it is
                 * for example we should prioritize armor over apples
                 */
                hotbarSwap.priority
            )

            return true
        }

        return false
    }

    /**
     * Either asks [ModuleInventoryCleaner] what to do or just takes everything.
     */
    private fun createCleanupPlan(screen: AbstractContainerScreen<*>): InventoryCleanupPlan {
        val cleanupPlan = if (!ModuleInventoryCleaner.running) {
            val usefulItems = screen.findItemsInContainer()

            InventoryCleanupPlan(HashSet(usefulItems), mutableListOf(), hashMapOf())
        } else {
            val availableItems = findNonEmptySlotsInInventory() + screen.findItemsInContainer()

            CleanupPlanGenerator(ModuleInventoryCleaner.cleanupTemplateFromSettings, availableItems).generatePlan()
        }

        return cleanupPlan
    }

    private fun sortByDistance(slots: MutableList<ContainerItemSlot>, factorRange: ClosedFloatingPointRange<Float>) {
        val n = slots.size
        if (n <= 2) return

        val hasRandom = factorRange.start != factorRange.endInclusive
        val randomFactors = if (hasRandom) Int2DoubleOpenHashMap(n) else null

        if (randomFactors != null) {
            for (slot in slots) {
                randomFactors.put(slot.slotInContainer, factorRange.random().toDouble())
            }
        }

        for (i in 0..<n - 1) {
            var bestIdx = i + 1
            var bestDist = Double.MAX_VALUE

            val current = slots[i]

            for (j in i + 1..<n) {
                val candidate = slots[j]
                val baseDist = current.distance(candidate).toDouble()
                val randomizedDist = if (randomFactors == null) {
                    baseDist
                } else {
                    baseDist * randomFactors.get(candidate.slotInContainer)
                }

                if (randomizedDist < bestDist) {
                    bestDist = randomizedDist
                    bestIdx = j
                }
            }

            slots.swap(i + 1, bestIdx)
        }
    }

    private fun invCleanerPriorityFor(slot: ItemSlot): Int {
        val categoryType = primaryItemType(slot)
        return when (categoryType) {
            ItemType.ARMOR -> 5
            ItemType.SWORD -> 4
            ItemType.WEAPON, ItemType.SPEAR, ItemType.MACE -> 3
            ItemType.PEARL -> 2
            ItemType.TOOL -> 1
            else -> 0
        }
    }

    private fun primaryItemType(slot: ItemSlot): ItemType {
        val facets = ItemCategorization.Default.getItemFacets(slot)
        val nonWeaponFacets = facets.filterNot { it is WeaponItemFacet }
        val facetsToCheck = if (nonWeaponFacets.isEmpty()) facets else nonWeaponFacets

        return facetsToCheck
            .maxBy { it.category.type.allocationPriority }
            .category
            .type
    }

    /**
     * Mode pattern (as requested in review): each selection mode owns its own settings, so the
     * DistanceRandomFactor only shows for the modes that actually sort by distance and is hidden for the
     * others — no doNotIncludeWhen hacks.
     */
    abstract class SelectionMode(name: String) : Mode(name) {
        override val parent: ModeValueGroup<SelectionMode> get() = selectionMode

        abstract fun process(slots: MutableList<ContainerItemSlot>)
    }

    /** Steal nearest-first (greedy nearest-neighbour), optionally randomized by [distanceRandomFactor]. */
    object Distance : SelectionMode("Distance") {
        val distanceRandomFactor by floatRange("DistanceRandomFactor", 1.0f..1.0f, 0.0f..5.0f, "factor")

        override fun process(slots: MutableList<ContainerItemSlot>) = sortByDistance(slots, distanceRandomFactor)
    }

    /** Steal in raw container-slot order. */
    object Index : SelectionMode("Index") {
        private val comparator: Comparator<ContainerItemSlot> = Comparator.comparingInt { it.slotInContainer }

        override fun process(slots: MutableList<ContainerItemSlot>) = slots.sortWith(comparator)
    }

    /** Steal in a fully random order. */
    object Random : SelectionMode("Random") {
        override fun process(slots: MutableList<ContainerItemSlot>) = slots.shuffle()
    }

    /**
     * Steal highest-priority items first (armor > sword > weapon > pearl > tool), and WITHIN each
     * priority group take them nearest-first, optionally randomized by [distanceRandomFactor].
     */
    object InvCleanerPriority : SelectionMode("InvCleanerPriority") {
        val distanceRandomFactor by floatRange("DistanceRandomFactor", 1.0f..1.0f, 0.0f..5.0f, "factor")

        override fun process(slots: MutableList<ContainerItemSlot>) {
            if (slots.size <= 2) return

            val grouped = slots.groupBy { invCleanerPriorityFor(it) }
            val sortedPriorities = grouped.keys.sortedDescending()

            slots.clear()
            for (priority in sortedPriorities) {
                val groupSlots = grouped.getValue(priority).toMutableList()
                sortByDistance(groupSlots, distanceRandomFactor)
                slots.addAll(groupSlots)
            }
        }
    }

    /**
     * @return the chest screen if it is open and the title matches the chest title
     */
    private fun getChestScreen(): AbstractContainerScreen<*>? {
        return mc.screen?.takeIf { it.canBeStolen() } as AbstractContainerScreen<*>?
    }

    fun Screen.canBeStolen(): Boolean {
        return running && this is AbstractContainerScreen<*> && this !is InventoryScreen &&
            checkScreenHandlerType.isValid(this) && checkScreenTitle.isValid(this)
    }

    private enum class ItemMoveMode(override val tag: String) : Tagged {
        QUICK_MOVE("QuickMove"),
        DRAG_AND_DROP("DragAndDrop"),
    }

}
