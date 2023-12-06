/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoArmor
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.CoroutineUtils.waitUntil
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isFullBlock
import net.ccbluex.liquidbounce.utils.extensions.shuffled
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.inventory.ArmorComparator.getBestArmorSet
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.canClickInventory
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.hasScheduledInLastLoop
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.isFirstInventoryClick
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.toHotbarIndex
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomDelay
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockContainer
import net.minecraft.block.BlockFalling
import net.minecraft.block.BlockWorkbench
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.item.EntityItem
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.*
import net.minecraft.potion.Potion

object InventoryCleaner: Module("InventoryCleaner", ModuleCategory.PLAYER) {
	private val drop by BoolValue("Drop", true, subjective = true)
	val sort by BoolValue("Sort", true, subjective = true)

	private val maxDelay: Int by object : IntegerValue("MaxDelay", 50, 0..500) {
		override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minDelay)
	}
	private val minDelay by object : IntegerValue("MinDelay", 50, 0..500) {
		override fun isSupported() = maxDelay > 0

		override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxDelay)
	}
	private val minItemAge by IntegerValue("MinItemAge", 0, 0..2000)

	private val limitStackCounts by BoolValue("LimitStackCounts", true, subjective = true)
		private val maxBlockStacks by IntegerValue("MaxBlockStacks", 5, 0..36, subjective = true) { limitStackCounts }
		private val maxFoodStacks by IntegerValue("MaxFoodStacks", 5, 0..36, subjective = true) { limitStackCounts }
		private val maxThrowableStacks by IntegerValue("MaxThrowableStacks", 5, 0..36, subjective = true) { limitStackCounts }
		// TODO: max potion, vehicle, ..., stacks?

	private val mergeStacks by BoolValue("MergeStacks", true, subjective = true)

	private val repairEquipment by BoolValue("RepairEquipment", true, subjective = true)

	private val invOpen by InventoryManager.invOpenValue
	private val simulateInventory by InventoryManager.simulateInventoryValue

	private val autoClose by InventoryManager.autoCloseValue
	private val startDelay by InventoryManager.startDelayValue
	private val closeDelay by InventoryManager.closeDelayValue

	private val noMove by InventoryManager.noMoveValue
	private val noMoveAir by InventoryManager.noMoveAirValue
	private val noMoveGround by InventoryManager.noMoveGroundValue

	private val randomSlot by BoolValue("RandomSlot", false)
	private val ignoreVehicles by BoolValue("IgnoreVehicles", false, subjective = true)

	private val onlyGoodPotions by BoolValue("OnlyGoodPotions", false, subjective = true)

	val highlightUseful by BoolValue("HighlightUseful", true, subjective = true)

	private val slot1Value = SortValue("Slot1", "Sword")
	private val slot2Value = SortValue("Slot2", "Bow")
	private val slot3Value = SortValue("Slot3", "Pickaxe")
	private val slot4Value = SortValue("Slot4", "Axe")
	private val slot5Value = SortValue("Slot5", "Shovel")
	private val slot6Value = SortValue("Slot6", "Food")
	private val slot7Value = SortValue("Slot7", "Throwable")
	private val slot8Value = SortValue("Slot8", "Block")
	private val slot9Value = SortValue("Slot9", "Block")

	private val SORTING_VALUES = arrayOf(
		slot1Value, slot2Value, slot3Value, slot4Value, slot5Value, slot6Value, slot7Value, slot8Value, slot9Value
	)

	private suspend fun shouldOperate(): Boolean {
		while (true) {
			if (!handleEvents())
				return false

			if (mc.playerController?.currentGameType?.isSurvivalOrAdventure != true)
				return false

			if (mc.thePlayer?.openContainer?.windowId != 0)
				return false

			if (invOpen && mc.currentScreen !is GuiInventory)
				return false

			// Wait till NoMove check isn't violated
			if (canClickInventory(closeWhenViolating = true))
				return true

			// If NoMove is violated, wait a tick and check again
			// If there is no delay, very weird things happen: https://www.guilded.gg/CCBlueX/groups/1dgpg8Jz/channels/034be45e-1b72-4d5a-bee7-d6ba52ba1657/chat?messageId=94d314cd-6dc4-41c7-84a7-212c8ea1cc2a
			delay(50)
		}
	}

	// Compact multiple small stacks into one to free up inventory space
	suspend fun mergeStacks() {
		if (!mergeStacks || !shouldOperate())
			return

		val thePlayer = mc.thePlayer ?: return

		// Loop multiple times until no clicks were scheduled
		while (true) {
			if (!shouldOperate()) return

			val stacks = thePlayer.openContainer.inventory

			// List of stack indices with different types to be compacted by double-clicking
			val indicesToDoubleClick = stacks.withIndex()
				.groupBy { it.value?.item }
				.mapNotNull { (item, groupedStacks) ->
					item ?: return@mapNotNull null

					val sortedStacks = groupedStacks
						// Only try to merge non-full stacks, without limiting stack counts in isStackUseful
						.filter {
							it.value.hasItemAgePassed(minItemAge) &&
							it.value.stackSize != it.value.maxStackSize && isStackUseful(it.value, stacks, noLimits = true)
						}
						// Prioritise stacks that are lower in inventory
						.sortedByDescending { it.index }
						// Prioritise stacks that are sorted
						.sortedByDescending { canBeSortedTo(it.index, it.value?.item, stacks.size) }

					// Return first stack that can be merged with a different stack of the same type else null
					sortedStacks.firstOrNull { (_, clickedStack) ->
						sortedStacks.any { (_, stackToMerge) ->
							clickedStack != stackToMerge
								&& clickedStack.stackSize + stackToMerge.stackSize <= clickedStack.maxStackSize
								// Check if stacks have the same NBT data and are actually mergeable
								&& clickedStack.isItemEqual(stackToMerge)
								&& ItemStack.areItemStackTagsEqual(clickedStack, stackToMerge)
						}
					}?.index
				}

			for (index in indicesToDoubleClick) {
				if (!shouldOperate()) return

				if (index in TickScheduler) continue

				// TODO: Perhaps add a slider for merge delay?

				click(index, 0, 0, coerceTo = 100)

				click(index, 0, 6, allowDuplicates = true, coerceTo = 100)

				click(index, 0, 0, allowDuplicates = true, coerceTo = 100)
			}

			if (indicesToDoubleClick.isEmpty())
				break

			// This part isn't fully instant because of the complex vanilla merging behaviour, stack size changes and so on
			// Waits a tick to see how the stacks got merged
			waitUntil(TickScheduler::isEmpty)
		}
	}

	// Repair tools by merging them in the crafting grid
	suspend fun repairEquipment() {
		if (!repairEquipment || !shouldOperate())
			return

		val thePlayer = mc.thePlayer ?: return

		// Loop multiple times until no repairs were done
		while (true) {
			if (!shouldOperate()) return

			val stacks = thePlayer.openContainer.inventory

			val pairsToRepair = stacks.withIndex()
				.filter { (_, stack) ->
					// Check if stack is damageable and either has no enchantments or just unbreaking.
					stack.hasItemAgePassed(minItemAge) && shouldBeRepaired(stack)
				}
				.groupBy { it.value.item }
				.filter { (_, stackGroup) ->
					// Only try to repair groups of items when they contain a useful item that can be repaired
					// Prevents repairing of items that would get thrown out
					stackGroup.any { isStackUseful(it.value, stacks, noLimits = true) && it.value.isItemDamaged }
				}
				.mapValues { (_, groupStacks) ->
					// Get all pairs of stacks that can be merged
					val bestCombination = groupStacks.withIndex()
						.flatMap { (index, indexedStack) ->
							groupStacks.drop(index + 1).map { indexedStack to it }
						}
						.mapNotNull {
							val (index1, stack1) = it.first
							val (index2, stack2) = it.second

							// Get combined durability of both stacks (with vanilla repair bonus) coerced to max durability
							val durability = getCombinedDurabilityIfBeneficial(stack1, stack2) ?: return@mapNotNull null

							Triple(index1, index2, durability)
						}
						.maxByOrNull { it.third } ?: return@mapValues null

					// If there is a stack with higher or equal durability than the best combination, don't repair
					if (bestCombination.third <= groupStacks.maxOf { it.value.totalDurability }) return@mapValues null
					else bestCombination
				}
				.mapNotNull { it.value }

			repair@ for ((index1, index2) in pairsToRepair) {
				if (!shouldOperate()) return

				if (index1 in TickScheduler || index2 in TickScheduler)
					continue

				// Drag and drop stack1 to crafting grid
				click(index1, 0, 0)
				click(1, 0, 0)

				// Drag and drop stack2 to crafting grid
				click(index2, 0, 0)
				click(2, 0, 0)

				val repairedStack = thePlayer.openContainer.getSlot(0).stack
				val repairedItem = repairedStack.item

				// Handle armor repairs with support for AutoArmor smart-swapping and equipping straight from crafting output
				if (repairedItem is ItemArmor) {
					val armorSlot = repairedItem.armorType + 5
					var equipAfterCrafting = true

					// Check if armor can be equipped straight from crafting output
					if (thePlayer.openContainer.getSlot(armorSlot).hasStack) {
						when {
							// Smart swap armor from crafting output to armor slot
							AutoArmor.handleEvents() && AutoArmor.smartSwap -> {
								// Grab repaired armor
								click(0, 0, 0)

								// Swap it with currently equipped armor
								click(armorSlot, 0, 0)

								// Drop worse item by dragging and dropping it
								click(-999, 0, 0)

								continue@repair
							}

							// Drop equipped armor and continue equipping repaired armor normally
							drop || AutoArmor.handleEvents() -> click(armorSlot, 0, 4)

							// Can't smart swap or drop, don't equip
							else -> equipAfterCrafting = false
						}
					}

					if (equipAfterCrafting) {
						// Grab repaired armor
						click(0, 0, 0)

						// Place it in armor slot
						click(repairedItem.armorType + 5, 0, 0)

						continue@repair
					}
				}

				if (sort) {
					for (hotbarIndex in 0..8) {
						if (!canBeSortedTo(hotbarIndex, repairedItem))
							continue

						val hotbarStack = stacks.getOrNull(stacks.size - 9 + hotbarIndex)

						// If occupied hotbar slot isn't already sorted or isn't strictly best, sort to it
						if (!canBeSortedTo(hotbarIndex, hotbarStack?.item)
							|| !isStackUseful(hotbarStack, stacks, strictlyBest = true))
						{
							// Sort repaired item to hotbar right after repairing
							click(0, hotbarIndex, 2)
							continue@repair
						}
					}
				}

				// Shift + left-click repaired item from crafting output into inventory
				click(0, 0, 1)
			}

			if (pairsToRepair.isEmpty())
				break

			// Waits a tick to see how the stacks got repaired
			waitUntil(TickScheduler::isEmpty)
		}
	}

	// Sort hotbar (with useful items without even dropping bad items first)
	suspend fun sortHotbar() {
		if (!sort || !shouldOperate()) return

		val thePlayer = mc.thePlayer ?: return

		hotbarLoop@ for ((hotbarIndex, value) in SORTING_VALUES.withIndex().shuffled(randomSlot)) {
			// Check if slot has a valid sorting target
			val isRightType = SORTING_TARGETS[value.get()] ?: continue

			// Stop if player violates invopen or nomove checks
			if (!shouldOperate()) return

			val stacks = thePlayer.openContainer.inventory

			val index = hotbarIndex + 36

			val stack = stacks.getOrNull(index)
			val item = stack?.item

			// Search for best item to sort
			suspend fun searchAndSort(strictlyBest: Boolean = false): Boolean {
				// Slot is already sorted
				if (isRightType(item) && isStackUseful(stack, stacks, strictlyBest = strictlyBest))
					return true

				for ((otherIndex, otherStack) in stacks.withIndex()) {
					if (otherIndex in TickScheduler)
						continue

					val otherItem = otherStack?.item

					// Check if an item is the correct type, isn't bad and isn't already sorted to a different slot
					if (isRightType(otherItem) && isStackUseful(otherStack, stacks, strictlyBest = strictlyBest) && !canBeSortedTo(otherIndex, otherItem, stacks.size)) {
						// If best item to sort was found, but its item age hasn't yet passed, skip search for this hotbar slot
						if (otherStack.hasItemAgePassed(minItemAge))
							click(otherIndex, hotbarIndex, 2)

						return true
					}
				}

				return false
			}

			// Try to sort strictly the best and if it is already sorted in a different slot try any other useful item of the correct type
			if (!searchAndSort(strictlyBest = true))
				searchAndSort()
		}

		waitUntil(TickScheduler::isEmpty)
	}

	// Drop bad items to free up inventory space
	suspend fun dropGarbage() {
		if (!drop || !shouldOperate()) return

		val thePlayer = mc.thePlayer ?: return

		for (index in thePlayer.openContainer.inventorySlots.indices.shuffled(randomSlot)) {
			// Stop if player violates invopen or nomove checks
			if (!shouldOperate()) return

			if (index in TickScheduler)
				continue

			val stacks = thePlayer.openContainer.inventory
			val stack = stacks.getOrNull(index) ?: continue

			if (!stack.hasItemAgePassed(minItemAge))
				continue

			// If stack isn't useful, drop it
			if (!isStackUseful(stack, stacks))
				click(index, 1, 4)
		}

		waitUntil(TickScheduler::isEmpty)
	}

	private suspend fun click(slot: Int, button: Int, mode: Int, allowDuplicates: Boolean = false, coerceTo: Int = Int.MAX_VALUE) {
		// Wait for NoMove or cancel click
		if (!shouldOperate())
			return

		if (simulateInventory || invOpen)
			serverOpenInventory = true

		if (isFirstInventoryClick) {
			// Have to set this manually, because it would delay all clicks until a first scheduled click was sent
			isFirstInventoryClick = false

			delay(startDelay.toLong())
		}

		TickScheduler.scheduleClick(slot, button, mode, allowDuplicates)

		hasScheduledInLastLoop = true

		delay(randomDelay(minDelay, maxDelay).coerceAtMost(coerceTo).toLong())
	}

	fun canBeSortedTo(index: Int, item: Item?, stacksSize: Int? = null): Boolean {
		if (!sort) return false

		// If stacksSize argument is passed, check if index is a hotbar slot
		val index =
			if (stacksSize != null) index.toHotbarIndex(stacksSize) ?: return false
			else index

		return SORTING_TARGETS[SORTING_VALUES.getOrNull(index)?.get()]?.invoke(item) ?: false
	}

	// TODO: Simplify all is useful checks by a single getBetterAlternativeCount and checking if it is above 0, above stack limit, ...
	fun isStackUseful(stack: ItemStack?, stacks: List<ItemStack?>, entityStacksMap: Map<ItemStack, EntityItem>? = null, noLimits: Boolean = false, strictlyBest: Boolean = false): Boolean {
		val item = stack?.item ?: return false

		return when (item) {
			in ITEMS_WHITELIST -> true

			is ItemEnderPearl, is ItemEnchantedBook, is ItemBed -> true

			is ItemFood -> isUsefulFood(stack, stacks, entityStacksMap, noLimits, strictlyBest)
			is ItemBlock -> isUsefulBlock(stack, stacks, entityStacksMap, noLimits, strictlyBest)

			is ItemArmor, is ItemTool, is ItemSword, is ItemBow -> isUsefulEquipment(stack, stacks, entityStacksMap)

			is ItemBoat, is ItemMinecart -> !ignoreVehicles

			is ItemPotion -> isUsefulPotion(stack)

			is ItemBucket -> isUsefulBucket(stack, stacks, entityStacksMap)

			is ItemFlintAndSteel -> isUsefulLighter(stack, stacks, entityStacksMap)

			in THROWABLE_ITEMS -> isUsefulThrowable(stack, stacks, entityStacksMap, noLimits, strictlyBest)

			else -> false
		}
	}

	private fun isUsefulEquipment(stack: ItemStack?, stacks: List<ItemStack?>, entityStacksMap: Map<ItemStack, EntityItem>? = null): Boolean {
		val item = stack?.item ?: return false

		return when (item) {
			is ItemArmor -> stack in getBestArmorSet(stacks, entityStacksMap)

			is ItemTool -> {
				val blockType = when (item) {
					is ItemAxe -> Blocks.log
					is ItemPickaxe -> Blocks.stone
					else -> Blocks.dirt
				}

				return hasBestParameters(stack, stacks, entityStacksMap) {
					it.item.getStrVsBlock(it, blockType) * it.durability
				}
			}

			is ItemSword ->
				hasBestParameters(stack, stacks, entityStacksMap) {
					it.attackDamage.toFloat()
				}

			is ItemBow ->
				hasBestParameters(stack, stacks, entityStacksMap) {
					it.getEnchantmentLevel(Enchantment.power).toFloat()
				}

			else -> false
		}
	}

	private fun isUsefulPotion(stack: ItemStack?): Boolean {
		val item = stack?.item ?: return false

		if (item !is ItemPotion) return false

		val isSplash = stack.isSplashPotion()
		val isHarmful = item.getEffects(stack)?.any { it.potionID in NEGATIVE_EFFECT_IDS } ?: return false

		// Only keep helpful potions and, if 'onlyGoodPotions' is disabled, also splash harmful potions
		return !isHarmful || (!onlyGoodPotions && isSplash)
	}

	private fun isUsefulLighter(stack: ItemStack?, stacks: List<ItemStack?>, entityStacksMap: Map<ItemStack, EntityItem>? = null): Boolean {
		val item = stack?.item ?: return false

		if (item !is ItemFlintAndSteel) return false

		val index = stacks.indexOf(stack)

		val isSorted = canBeSortedTo(index, item, stacks.size)

		if (isSorted) return true

		val stacksToIterate = stacks.toMutableList()

		var distanceSqToItem = .0

		if (!entityStacksMap.isNullOrEmpty()) {
			distanceSqToItem = mc.thePlayer.getDistanceSqToEntity(entityStacksMap[stack] ?: return false)
			stacksToIterate += entityStacksMap.keys
		}

		return stacksToIterate.withIndex().none { (otherIndex, otherStack) ->
			if (otherStack == stack)
				return@none false

			val otherItem = otherStack?.item ?: return@none false

			if (otherItem !is ItemFlintAndSteel)
				return@none false

			// Items dropped on ground should have index -1
			val otherIndex = if (otherIndex > stacks.lastIndex) -1 else otherIndex

			// Only when both items are dropped on ground
			if (index == otherIndex) {
				val otherEntityItem = entityStacksMap?.get(otherStack) ?: return@none false

				return distanceSqToItem > mc.thePlayer.getDistanceSqToEntity(otherEntityItem)
			}

			canBeSortedTo(otherIndex, otherItem, stacks.size)
					|| otherStack.totalDurability > stack.totalDurability || otherIndex > index
		}
	}

	private fun isUsefulFood(stack: ItemStack?, stacks: List<ItemStack?>, entityStacksMap: Map<ItemStack, EntityItem>?, ignoreLimits: Boolean, strictlyBest: Boolean): Boolean {
		val item = stack?.item ?: return false

		if (item !is ItemFood) return false

		// Skip checks if there is no stack limit set and when you are not strictly searching for best option
		if (ignoreLimits || !limitStackCounts) {
			if (!strictlyBest)
				return true
		// Skip checks if limit is set to 0
		} else if (maxFoodStacks == 0)
			return false

		val stackSaturation = item.getSaturationModifier(stack) * stack.stackSize

		val index = stacks.indexOf(stack)

		val isSorted = canBeSortedTo(index, item, stacks.size)

		val stacksToIterate = stacks.toMutableList()

		var distanceSqToItem = .0

		if (!entityStacksMap.isNullOrEmpty()) {
			distanceSqToItem = mc.thePlayer.getDistanceSqToEntity(entityStacksMap[stack] ?: return false)
			stacksToIterate += entityStacksMap.keys
		}

		val betterCount = stacksToIterate.withIndex().count { (otherIndex, otherStack) ->
			if (stack == otherStack)
				return@count false

			val otherItem = otherStack?.item ?: return@count false

			if (otherItem !is ItemFood)
				return@count false

			// Items dropped on ground should have index -1
			val otherIndex = if (otherIndex > stacks.lastIndex) -1 else otherIndex

			val otherStackSaturation = otherItem.getSaturationModifier(otherStack) * otherStack.stackSize

			when (otherStackSaturation.compareTo(stackSaturation)) {
				// Other stack has bigger saturation sum
				1 -> true
				// Both stacks are equally good
				0 -> {
					// Only true when both items are dropped on ground
					if (index == otherIndex) {
						val otherEntityItem = entityStacksMap?.get(otherStack) ?: return@count false

						// If other item is closer, count it as better
						distanceSqToItem > mc.thePlayer.getDistanceSqToEntity(otherEntityItem)
					} else {
						val isOtherSorted = canBeSortedTo(otherIndex, otherItem, stacks.size)

						// Count as better alternative only when compared stack isn't sorted and the other is sorted, or has higher index
						!isSorted && (isOtherSorted || otherIndex > index)
					}
				}
				else -> false
			}
		}

		// If sorting is checking if item is strictly the best option, only return true for items that have no better alternatives
		return if (strictlyBest) betterCount == 0 else betterCount < maxFoodStacks
	}

	private fun isUsefulBlock(stack: ItemStack?, stacks: List<ItemStack?>, entityStacksMap: Map<ItemStack, EntityItem>?, ignoreLimits: Boolean, strictlyBest: Boolean): Boolean {
		if (!isSuitableBlock(stack)) return false

		// Skip checks if there is no stack limit set and when you are not strictly searching for best option
		if (ignoreLimits || !limitStackCounts) {
			if (!strictlyBest)
				return true
		// Skip checks if limit is set to 0
		} else if (maxBlockStacks == 0)
			return false

		val index = stacks.indexOf(stack)

		val isSorted = canBeSortedTo(index, stack!!.item, stacks.size)

		val stacksToIterate = stacks.toMutableList()

		var distanceSqToItem = .0

		if (!entityStacksMap.isNullOrEmpty()) {
			distanceSqToItem = mc.thePlayer.getDistanceSqToEntity(entityStacksMap[stack] ?: return false)
			stacksToIterate += entityStacksMap.keys
		}

		val betterCount = stacksToIterate.withIndex().count { (otherIndex, otherStack) ->
			if (otherStack == stack || !isSuitableBlock(otherStack))
				return@count false

			// Items dropped on ground should have index -1
			val otherIndex = if (otherIndex > stacks.lastIndex) -1 else otherIndex

			when (otherStack!!.stackSize.compareTo(stack.stackSize)) {
				// Found a stack that has higher size
				1 -> true
				// Both stacks are equally good
				0 -> {
					// Only true when both items are dropped on ground
					if (index == otherIndex) {
						val otherEntityItem = entityStacksMap?.get(otherStack) ?: return@count false

						// If other item is closer, count it as better
						distanceSqToItem > mc.thePlayer.getDistanceSqToEntity(otherEntityItem)
					} else {
						val isOtherSorted = canBeSortedTo(otherIndex, otherStack.item, stacks.size)

						// Count as better alternative only when compared stack isn't sorted and the other is sorted, or has higher index
						!isSorted && (isOtherSorted || otherIndex > index)
					}
				}
				else -> false
			}
		}

		// If sorting is checking if item is strictly the best option, only return true for items that have no better alternatives
		return if (strictlyBest) betterCount == 0 else betterCount < maxBlockStacks
	}

	private fun isUsefulThrowable(stack: ItemStack?, stacks: List<ItemStack?>, entityStacksMap: Map<ItemStack, EntityItem>?, ignoreLimits: Boolean, strictlyBest: Boolean): Boolean {
		val item = stack?.item ?: return false

		if (item !in THROWABLE_ITEMS) return false

		// Skip checks if there is no stack limit set and when you are not strictly searching for best option
		if (ignoreLimits || !limitStackCounts) {
			if (!strictlyBest)
				return true
		// Skip checks if limit is set to 0
		} else if (maxBlockStacks == 0)
			return false

		val index = stacks.indexOf(stack)

		val isSorted = canBeSortedTo(index, item, stacks.size)

		val stacksToIterate = stacks.toMutableList()

		var distanceSqToItem = .0

		if (!entityStacksMap.isNullOrEmpty()) {
			distanceSqToItem = mc.thePlayer.getDistanceSqToEntity(entityStacksMap[stack] ?: return false)
			stacksToIterate += entityStacksMap.keys
		}

		val betterCount = stacksToIterate.withIndex().count { (otherIndex, otherStack) ->
			if (otherStack == stack)
				return@count false

			val otherItem = otherStack?.item ?: return@count false

			if (otherItem !in THROWABLE_ITEMS) return@count false

			// Items dropped on ground should have index -1
			val otherIndex = if (otherIndex > stacks.lastIndex) -1 else otherIndex

			when (otherStack.stackSize.compareTo(stack.stackSize)) {
				// Found a stack that has higher size
				1 -> true
				// Both stacks are equally good
				0 -> {
					// Only true when both items are dropped on ground
					if (index == otherIndex) {
						val otherEntityItem = entityStacksMap?.get(otherStack) ?: return@count false

						// If other item is closer, count it as better
						distanceSqToItem > mc.thePlayer.getDistanceSqToEntity(otherEntityItem)
					} else {
						val isOtherSorted = canBeSortedTo(otherIndex, otherStack.item, stacks.size)

						// Count as better alternative only when compared stack isn't sorted and the other is sorted, or has higher index
						!isSorted && (isOtherSorted || otherIndex > index)
					}
				}
				else -> false
			}
		}

		// If sorting is checking if item is strictly the best option, only return true for items that have no better alternatives
		return if (strictlyBest) betterCount == 0 else betterCount < maxThrowableStacks
	}

	// Limit buckets to max 1 per type
	private fun isUsefulBucket(stack: ItemStack?, stacks: List<ItemStack?>, entityStacksMap: Map<ItemStack, EntityItem>?): Boolean {
		val item = stack?.item ?: return false

		if (item !is ItemBucket) return false

		val index = stacks.indexOf(stack)

		val isSorted = canBeSortedTo(index, item, stacks.size)

		if (isSorted) return true

		val stacksToIterate = stacks.toMutableList()

		var distanceSqToItem = .0

		if (!entityStacksMap.isNullOrEmpty()) {
			distanceSqToItem = mc.thePlayer.getDistanceSqToEntity(entityStacksMap[stack] ?: return false)
			stacksToIterate += entityStacksMap.keys
		}

		return stacksToIterate.withIndex().none { (otherIndex, otherStack) ->
			if (otherStack == stack)
				return@none false

			val otherItem = otherStack?.item ?: return@none false

			if (otherItem != item)
				return@none false

			// Items dropped on ground should have index -1
			val otherIndex = if (otherIndex > stacks.lastIndex) -1 else otherIndex

			// Only when both items are dropped on ground
			if (index == otherIndex) {
				val otherEntityItem = entityStacksMap?.get(otherStack) ?: return@none false

				return distanceSqToItem > mc.thePlayer.getDistanceSqToEntity(otherEntityItem)
			}

			canBeSortedTo(otherIndex, otherItem, stacks.size) || otherIndex > index
		}
	}

	private fun hasBestParameters(stack: ItemStack?, stacks: List<ItemStack?>, entityStacksMap: Map<ItemStack, EntityItem>? = null, parameters: (ItemStack) -> Float): Boolean {
		val item = stack?.item ?: return false

		val index = stacks.indexOf(stack)

		val currentStats = parameters(stack)

		val isSorted = canBeSortedTo(index, item, stacks.size)

		val stacksToIterate = stacks.toMutableList()

		var distanceSqToItem = .0

		if (!entityStacksMap.isNullOrEmpty()) {
			distanceSqToItem = mc.thePlayer.getDistanceSqToEntity(entityStacksMap[stack] ?: return false)
			stacksToIterate += entityStacksMap.keys
		}

		stacksToIterate.forEachIndexed { otherIndex, otherStack ->
			val otherItem = otherStack?.item ?: return@forEachIndexed

			// Check if items aren't the same instance but are the same type
			if (stack == otherStack || item.javaClass != otherItem.javaClass)
				return@forEachIndexed

			// Items dropped on ground should have index -1
			val otherIndex = if (otherIndex > stacks.lastIndex) -1 else otherIndex

			val otherStats = parameters(otherStack)

			val isOtherSorted = canBeSortedTo(otherIndex, otherItem, stacks.size)

			// Compare stats one by one
			when (otherStats.compareTo(currentStats)) {
				// Other item had better base stat, compared item isn't the best
				1 -> return false
				// Both have the same base stat, compare sum of their enchantment levels
				0 -> when (otherStack.enchantmentSum.compareTo(stack.enchantmentSum)) {
					1 -> return false
					// Same base stat, sum of enchantment levels, compare durability * unbreaking
					0 -> when (otherStack.totalDurability.compareTo(stack.totalDurability)) {
						1 -> return false
						// Both items are pretty much equally good, sorted item wins over not sorted, otherwise the one with higher index
						0 ->  {
							// Only true when both items are dropped on ground, if other item is closer, compared one isn't the best
							if (index == otherIndex) {
								val otherEntityItem = entityStacksMap?.get(otherStack) ?: return@forEachIndexed
								when (distanceSqToItem.compareTo(mc.thePlayer.getDistanceSqToEntity(otherEntityItem))) {
									1 -> return false
									// Both items are exactly far, pretty much impossible
									0 -> return true
								}
							} else if (!isSorted && (isOtherSorted || otherIndex > index))
								return false
						}
					}
				}
			}
		}

		return true
	}

	@Suppress("DEPRECATION")
	private fun isSuitableBlock(stack: ItemStack?): Boolean {
		val item = stack?.item ?: return false

		if (item is ItemBlock) {
			val block = item.block

			return isFullBlock(block) && !block.hasTileEntity()
					&& block !is BlockWorkbench && block !is BlockContainer && block !is BlockFalling
		}

		return false
	}

	/**
	 * Calculates resulting durability after repairing equipment.
	 * @return Combined durability of both stacks (with vanilla repair bonus) coerced to max durability
	 * or null if either stack has higher or equal durability than the combined durability.
	 */
	private fun getCombinedDurabilityIfBeneficial(stack1: ItemStack, stack2: ItemStack): Int? {
		// Get combined durability of both stacks (with vanilla repair bonus) coerced to max durability
		val combinedDurability =
			(stack1.durability + stack2.durability + stack1.maxDamage * 5 / 100)
				.coerceAtMost(stack1.maxDamage)

		// Check if combined durability is higher than total enchanted durability of either stack
		return if (stack1.totalDurability >= combinedDurability || stack2.totalDurability >= combinedDurability) null
		else combinedDurability
	}

	// Check if stack is repairable and either has no enchantments or just unbreaking.
	private fun shouldBeRepaired(stack: ItemStack?) =
		!stack.isEmpty() && stack.item.isRepairable && (
			!stack.isItemEnchanted || (stack.enchantmentCount == 1 && Enchantment.unbreaking in stack.enchantments)
		)

	fun canBeRepairedWithOther(stack: ItemStack?, stacks: List<ItemStack?>): Boolean {
		if (!handleEvents() || !repairEquipment)
			return false

		val item = stack?.item ?: return false

		if (!shouldBeRepaired(stack))
			return false

		return stacks.any { otherStack ->
			if (otherStack.isEmpty() || otherStack == stack)
				return@any false

			if (otherStack.item != item)
				return@any false

			getCombinedDurabilityIfBeneficial(stack, otherStack) != null
		}
	}

	private class SortValue(name: String, value: String) : ListValue(name, SORTING_KEYS, value, subjective = true) {
		override fun isSupported() = sort
		override fun onChanged(oldValue: String, newValue: String) =
			SORTING_VALUES.forEach { value ->
				if (value != this && newValue == value.get() && SORTING_TARGETS.keys.indexOf(value.get()) < 5) {
					value.set(oldValue)
					value.openList = true

					displayChatMessage("§8[§9§lInventoryCleaner§8] §3Value §a${value.name}§3 was changed to §a$oldValue§3 to prevent conflicts.")
				}
			}
	}
}

private val ITEMS_WHITELIST = arrayOf(
	Items.arrow, Items.diamond, Items.iron_ingot, Items.gold_ingot, Items.stick
)

private val THROWABLE_ITEMS = arrayOf(Items.egg, Items.snowball)

val NEGATIVE_EFFECT_IDS = intArrayOf(
	Potion.moveSlowdown.id, Potion.digSlowdown.id, Potion.harm.id, Potion.confusion.id, Potion.blindness.id,
	Potion.hunger.id, Potion.weakness.id, Potion.poison.id, Potion.wither.id,
)

private val SORTING_TARGETS: Map<String, ((Item?) -> Boolean)?> = mapOf(
	"Sword" to { it is ItemSword },
	"Bow" to { it is ItemBow },
	"Pickaxe" to { it is ItemPickaxe },
	"Axe" to { it is ItemAxe },
	"Shovel" to { it is ItemSpade },
	"Food" to { it is ItemFood },
	"Block" to { it is ItemBlock },
	"Water" to { it == Items.water_bucket || it == Items.bucket },
	"Fire" to { it is ItemFlintAndSteel || it == Items.lava_bucket || it == Items.bucket },
	"Gapple" to { it is ItemAppleGold },
	"Pearl" to { it is ItemEnderPearl },
	"Potion" to { it is ItemPotion },
	"Throwable" to { it is ItemEgg || it is ItemSnowball },
	"Ignore" to null
)

private val SORTING_KEYS = SORTING_TARGETS.keys.toTypedArray()