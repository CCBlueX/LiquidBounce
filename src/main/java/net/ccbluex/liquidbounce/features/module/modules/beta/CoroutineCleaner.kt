@file:Suppress("ControlFlowWithEmptyBody")

package net.ccbluex.liquidbounce.features.module.modules.beta

import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.movement.InventoryMove
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.CoroutineUtils.waitUntil
import net.ccbluex.liquidbounce.utils.InventoryUtils.isFirstInventoryClick
import net.ccbluex.liquidbounce.utils.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isFullBlock
import net.ccbluex.liquidbounce.utils.extensions.shuffled
import net.ccbluex.liquidbounce.utils.item.*
import net.ccbluex.liquidbounce.utils.item.CoroutineArmorComparator.getBestArmorSet
import net.ccbluex.liquidbounce.utils.timer.TimeUtils.randomDelay
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

// TODO: diamond pickaxe slot 2, diamond pickaxe effi 1 slot 3, idk it works fine?

object CoroutineCleaner: Module("CoroutineCleaner", ModuleCategory.BETA) {
	private val drop by BoolValue("Drop", true)
	val sort by BoolValue("Sort", true)

	private val maxDelay: Int by object : IntegerValue("MaxDelay", 50, 0..500) {
		override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minDelay)
	}
	private val minDelay by object : IntegerValue("MinDelay", 50, 0..500) {
		override fun isSupported() = maxDelay > 0

		override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxDelay)
	}

	private val minItemAge by IntegerValue("MinItemAge", 0, 0..2000)

	private val maxBlockStacks by IntegerValue("MaxBlockStacks", 5, 0..36)
	private val maxFoodStacks by IntegerValue("MaxFoodStacks", 5, 0..36)

	private val compactStacks by BoolValue("CompactStacks", true)

	private val invOpen by BoolValue("InvOpen", false)
	private val simulateInventory by BoolValue("SimulateInventory", true) { !invOpen }

	private val autoClose by BoolValue("AutoClose", false) { invOpen }
	private val startDelay by IntegerValue("StartDelay", 0, 0..500) { invOpen || simulateInventory }

	private val closeDelay by IntegerValue("CloseDelay", 0, 0..500) { (invOpen && autoClose) || (!invOpen && simulateInventory) }
	private val noMove by BoolValue("NoMoveClicks", false)
	private val noMoveAir by BoolValue("NoClicksInAir", false) { noMove }

	private val noMoveGround by BoolValue("NoClicksOnGround", true) { noMove }

	private val randomSlot by BoolValue("RandomSlot", false)
	private val ignoreVehicles by BoolValue("IgnoreVehicles", false)

	private val onlyGoodPotions by BoolValue("OnlyGoodPotions", false)

	val highlightUseful by BoolValue("HighlightUseful", true)

	private val slot1Value = SortValue("Slot1", "Sword")
	private val slot2Value = SortValue("Slot2", "Bow")
	private val slot3Value = SortValue("Slot3", "Pickaxe")
	private val slot4Value = SortValue("Slot4", "Axe")
	private val slot5Value = SortValue("Slot5", "Shovel")
	private val slot6Value = SortValue("Slot6", "Food")
	private val slot7Value = SortValue("Slot7", "Ignore")
	private val slot8Value = SortValue("Slot8", "Block")
	private val slot9Value = SortValue("Slot9", "Block")

	private var hasClicked = false

	private suspend fun shouldExecute(): Boolean {
		while (true) {
			if (!state)
				return false

			// Wait for AutoArmor to execute first, when opening inventory (for AutoClose compatibility)
			if (CoroutineArmorer.state && !CoroutineArmorer.hasSearched)
				return false

			if (mc.thePlayer?.openContainer?.windowId != 0)
				return false

			if (invOpen && mc.currentScreen !is GuiInventory && !serverOpenInventory)
				return false

			// Wait till NoMove check isn't violated
			if (InventoryMove.canClickInventory() && !(noMove && isMoving && if (mc.thePlayer.onGround) noMoveGround else noMoveAir))
				return true

			// If NoMove is violated, wait a tick and check again
			// If there is no delay, very weird things happen: https://www.guilded.gg/CCBlueX/groups/1dgpg8Jz/channels/034be45e-1b72-4d5a-bee7-d6ba52ba1657/chat?messageId=94d314cd-6dc4-41c7-84a7-212c8ea1cc2a
			delay(50)
		}
	}

	suspend fun execute() {
		val thePlayer = mc.thePlayer ?: return

		if (!shouldExecute())
			return

		hasClicked = false

		// Compact multiple small stacks into one
		if (compactStacks) {
			// Loop multiple times until no clicks were scheduled
			while (true) {
				if (!shouldExecute()) return

				val stacks = thePlayer.openContainer.inventory

				// List of stack indices with different types to be compacted by double-clicking
				val indicesToDoubleClick = stacks.withIndex()
					.groupBy { it.value?.item }
					.mapNotNull { (item, groupedStacks) ->
						item ?: return@mapNotNull null

						val sortedStacks = groupedStacks
							.filterNot { it.value.stackSize == it.value.maxStackSize }
							// Prioritise stacks that are lower in inventory
							.sortedByDescending { it.index }
							// Prioritise stacks that are sorted
							.sortedByDescending { canBeSortedTo(it.index, it.value?.item, stacks.size) }

						// Return first stack that can be merged with a different stack of the same type else null
						sortedStacks.firstOrNull { (_, clickedStack) ->
							sortedStacks.any { (_, mergedStack) ->
								clickedStack.stackSize + mergedStack.stackSize <= clickedStack.maxStackSize
							}
						}?.index
					}

				var hasMerged = false

				for (index in indicesToDoubleClick) {
					if (!shouldExecute()) return

					if (index in TickScheduler) continue

					hasMerged = true

					click(index, 0, 0)

					click(index, 0, 6, allowDuplicates = true)

					click(index, 0, 0, allowDuplicates = true)
				}

				if (!hasMerged)
					break

				// This part can't be fully instant because of the complex vanilla merging behaviour, stack size changes and so on
				waitUntil { TickScheduler.isEmpty() }
			}
		}

		// Sort hotbar (with useful items without even dropping bad items first)
		if (sort) {
			for ((hotbarIndex, value) in SORTING_VALUES.withIndex().shuffled(randomSlot)) {
				// Check if slot has a valid sorting target
				val isRightType = SORTING_TARGETS[value.get()] ?: continue

				// Stop if player violates invopen or nomove checks
				if (!shouldExecute()) return

				val stacks = thePlayer.openContainer.inventory

				val index = hotbarIndex + 36

				val stack = stacks.getOrNull(index)
				val item = stack?.item

				// Slot is already sorted
				if (isRightType(item) && isStackUseful(stack, stacks))
					continue

				// Search for best item to sort
				for ((otherIndex, otherStack) in stacks.withIndex()) {
					if (otherIndex in TickScheduler)
						continue

					if (!otherStack.hasItemAgePassed(minItemAge))
						continue

					val otherItem = otherStack?.item

					// Check if an item is the correct type, isn't bad and isn't already sorted
					if (isRightType(otherItem) && isStackUseful(otherStack, stacks) && !canBeSortedTo(otherIndex, otherItem, stacks.size)) {
						click(otherIndex, hotbarIndex, 2)
						break
					}
				}
			}
		}

		// Drop bad items
		if (drop) {
			for (index in thePlayer.openContainer.inventorySlots.indices.shuffled(randomSlot)) {
				// Stop if player violates invopen or nomove checks
				if (!shouldExecute()) return

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
		}

		// Wait till all scheduled clicks were sent
		waitUntil { TickScheduler.isEmpty() }

		// If InventoryCleaner had clicked on items, use its CloseDelay, else use AutoArmor's
		val sharedCloseDelay = if (hasClicked) closeDelay else CoroutineArmorer.closeDelay

		// Close visually open inventory (also for AutoArmor, that depends on InventoryCleaner to do this)
		if (shouldCloseOpenInv()) {
			delay(sharedCloseDelay.toLong())

			// Check if screen hasn't changed after the delay
			if (shouldCloseOpenInv())
				thePlayer.closeScreen()
		}

		// Close simulated inventory if player doesn't have it open visually
		else if (shouldCloseSimulatedInv()) {
			delay(sharedCloseDelay.toLong())

			// Check if screen hasn't changed after the delay
			if (shouldCloseSimulatedInv())
				serverOpenInventory = false
		}
	}

	private val SORTING_VALUES = arrayOf(
		slot1Value, slot2Value, slot3Value, slot4Value, slot5Value, slot6Value, slot7Value, slot8Value, slot9Value
	)

	private fun shouldCloseOpenInv() = (hasClicked || CoroutineArmorer.hasClicked) && mc.currentScreen is GuiInventory
			&& ((invOpen && autoClose) || (CoroutineArmorer.invOpen && CoroutineArmorer.autoClose))

	private fun shouldCloseSimulatedInv() = simulateInventory && serverOpenInventory && mc.currentScreen !is GuiInventory

	private class SortValue(name: String, value: String) : ListValue(name, SORTING_KEYS, value) {
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

	fun canBeSortedTo(index: Int, item: Item?, stacksSize: Int? = null): Boolean {
		if (!sort) return false

		// If stacksSize argument is passed, check if index is a hotbar slot
		val index =
			if (stacksSize != null) index.toHotbarIndex(stacksSize) ?: return false
			else index

		return SORTING_TARGETS[SORTING_VALUES.getOrNull(index)?.get()]?.invoke(item) ?: false
	}

	private fun Int.toHotbarIndex(stacksSize: Int): Int? {
		val parsed = this - stacksSize + 9

		return if (parsed in 0..8) parsed else null
	}

	suspend fun click(slot: Int, button: Int, mode: Int, allowDuplicates: Boolean = false) {
		if (simulateInventory) serverOpenInventory = true

		if (!hasClicked) {
			// Delay first click
			// If AutoArmor finished with open inventory, don't delay InventoryCleaner by another start delay
			if (isFirstInventoryClick) {
				// Have to set this manually, because it would delay all clicks until scheduled clicks were sent
				isFirstInventoryClick = false

				delay(startDelay.toLong())

			}

			hasClicked = true
		}

		TickScheduler.scheduleClick(slot, button, mode, allowDuplicates)

		delay(randomDelay(minDelay, maxDelay).toLong())
	}

	fun isStackUseful(stack: ItemStack?, stacks: List<ItemStack?>, entityStacksMap: Map<ItemStack, EntityItem>? = null): Boolean {
		val item = stack?.item ?: return false

		return when (item) {
			in ITEMS_WHITELIST -> true

			// TODO: Limit max bucket count, vehicle count
			is ItemEnderPearl, is ItemEnchantedBook, is ItemBucket, is ItemBed -> true

			// TODO: Simplify and maybe add support for stack merging when comparing max stack counts
			is ItemFood -> isUsefulFood(stack, stacks, entityStacksMap)
			is ItemBlock -> isUsefulBlock(stack, stacks, entityStacksMap)

			is ItemArmor, is ItemTool, is ItemSword, is ItemBow -> isUsefulEquipment(stack, stacks, entityStacksMap)

			is ItemBoat, is ItemMinecart -> !ignoreVehicles

			is ItemPotion -> isUsefulPotion(stack)


			else -> false
		}
	}

	private fun isUsefulPotion(stack: ItemStack?): Boolean {
		val item = stack?.item ?: return false

		if (item is ItemPotion) {
			val isSplash = stack.isSplashPotion()
			val isHarmful = item.getEffects(stack)?.any { it.potionID in NEGATIVE_EFFECT_IDS } ?: return false

			// Only keep helpful potions and, if 'onlyGoodPotions' is disabled, also splash harmful potions
			return !isHarmful || (!onlyGoodPotions && isSplash)
		}

		return false
	}

	private fun isUsefulEquipment(stack: ItemStack?, stacks: List<ItemStack?>, entityStacksMap: Map<ItemStack, EntityItem>? = null): Boolean {
		val item = stack?.item ?: return false

		return when (item) {
			is ItemArmor -> getBestArmorSet(stacks, entityStacksMap)?.contains(stack) ?: true

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

	private fun isUsefulFood(stack: ItemStack?, stacks: List<ItemStack?>, entityStacksMap: Map<ItemStack, EntityItem>? = null): Boolean {
		val item = stack?.item ?: return false

		if (item !is ItemFood) return false

		val stackSaturation = item.getSaturationModifier(stack) * stack.stackSize

		val index = stacks.indexOf(stack)

		val isSorted = canBeSortedTo(index, item, stacks.size)

		val iteratedStacks = stacks.toMutableList()

		var distanceSqToItem = .0

		if (!entityStacksMap.isNullOrEmpty()) {
			distanceSqToItem = mc.thePlayer.getDistanceSqToEntity(entityStacksMap[stack] ?: return false)
			iteratedStacks += entityStacksMap.keys
		}

		val betterCount = iteratedStacks.withIndex().count { (otherIndex, otherStack) ->
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

		return betterCount < maxFoodStacks
	}

	private fun isUsefulBlock(stack: ItemStack?, stacks: List<ItemStack?>, entityStacksMap: Map<ItemStack, EntityItem>? = null): Boolean {
		if (!isSuitableBlock(stack))
			return false

		val index = stacks.indexOf(stack)

		val isSorted = canBeSortedTo(index, stack!!.item, stacks.size)

		val iteratedStacks = stacks.toMutableList()

		var distanceSqToItem = .0

		if (!entityStacksMap.isNullOrEmpty()) {
			distanceSqToItem = mc.thePlayer.getDistanceSqToEntity(entityStacksMap[stack] ?: return false)
			iteratedStacks += entityStacksMap.keys
		}

		val betterCount = iteratedStacks.withIndex().count { (otherIndex, otherStack) ->
			if (!isSuitableBlock(otherStack) || otherStack == stack)
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

		return betterCount < maxBlockStacks
	}

	private fun hasBestParameters(stack: ItemStack?, stacks: List<ItemStack?>, entityStacksMap: Map<ItemStack, EntityItem>? = null, parameters: (ItemStack) -> Float): Boolean {
		val item = stack?.item ?: return false

		val index = stacks.indexOf(stack)

		val currentStats = parameters(stack)

		val isSorted = canBeSortedTo(index, item, stacks.size)

		val iteratedStacks = stacks.toMutableList()

		var distanceSqToItem = .0

		if (!entityStacksMap.isNullOrEmpty()) {
			distanceSqToItem = mc.thePlayer.getDistanceSqToEntity(entityStacksMap[stack] ?: return false)
			iteratedStacks += entityStacksMap.keys
		}

		iteratedStacks.forEachIndexed { otherIndex, otherStack ->
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
	fun isSuitableBlock(stack: ItemStack?): Boolean {
		val item = stack?.item ?: return false

		if (item is ItemBlock) {
			val block = item.block

			return isFullBlock(block) && !block.hasTileEntity()
					&& block !is BlockWorkbench && block !is BlockContainer && block !is BlockFalling
		}

		return false
	}
}

private val ITEMS_WHITELIST = arrayOf(
	Items.arrow, Items.diamond, Items.iron_ingot, Items.gold_ingot, Items.stick
)

private val NEGATIVE_EFFECT_IDS = arrayOf(
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
	"Fire" to { it == Items.flint_and_steel || it == Items.lava_bucket || it == Items.bucket },
	"Gapple" to { it is ItemAppleGold },
	"Pearl" to { it is ItemEnderPearl },
	"Potion" to { it is ItemPotion },
	"Ignore" to null
)

private val SORTING_KEYS = SORTING_TARGETS.keys.toTypedArray()