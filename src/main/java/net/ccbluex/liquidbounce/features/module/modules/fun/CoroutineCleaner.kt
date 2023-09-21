@file:Suppress("ControlFlowWithEmptyBody")

package net.ccbluex.liquidbounce.features.module.modules.`fun`

import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.movement.InventoryMove
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
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
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.*
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.client.C16PacketClientStatus
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT
import net.minecraft.potion.Potion

object CoroutineCleaner: Module("CoroutineCleaner", ModuleCategory.BETA) {
	private val invOpen by BoolValue("InvOpen", false)
	private val simulateInventory by BoolValue("SimulateInventory", true) { !invOpen }
	private val autoClose by BoolValue("AutoClose", false) { invOpen }

	private val startDelay by IntegerValue("StartDelay", 0, 0..500) { invOpen || simulateInventory }
	private val closeDelay by IntegerValue("CloseDelay", 0, 0..500) { (invOpen && autoClose) || simulateInventory }

	private val noMove by BoolValue("NoMoveClicks", false)
	private val noMoveAir by BoolValue("NoClicksInAir", false) { noMove }
	private val noMoveGround by BoolValue("NoClicksOnGround", true) { noMove }

	private val randomSlot by BoolValue("RandomSlot", false)

	private val itemDelay by IntegerValue("ItemDelay", 0, 0..2000)

	private val ignoreVehicles by BoolValue("IgnoreVehicles", false)

	private val onlyGoodPotions by BoolValue("OnlyGoodPotions", false)

	private val drop by BoolValue("Drop", true)

	private val maxDropDelay: Int by object : IntegerValue("MaxDropDelay", 50, 0..500) {
		override fun isSupported() = drop

		override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minDropDelay)
	}
	private val minDropDelay by object : IntegerValue("MinDropDelay", 50, 0..500) {
		override fun isSupported() = drop && maxDropDelay > 0

		override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxDropDelay)
	}

	val sort by BoolValue("Sort", true)

	private val maxSortDelay: Int by object : IntegerValue("MaxSortDelay", 50, 0..500) {
		override fun isSupported() = sort

		override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minSortDelay)
	}
	private val minSortDelay by object : IntegerValue("MinSortDelay", 50, 0..500) {
		override fun isSupported() = sort && maxSortDelay > 0

		override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxSortDelay)
	}

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

		if (!shouldExecute()) return

		hasClicked = false

		// Sort hotbar (with useful items without even dropping bad items first)
		if (sort) {
			for ((hotbarIndex, value) in SORTING_VALUES.withIndex().shuffled(randomSlot)) {
				// Check if slot has a valid sorting target
				val isRightType = SORTING_TARGETS[value.get()] ?: continue

				// Stop if player violates invopen or nomove checks
				if (!shouldExecute()) return

				val stacks = thePlayer.inventoryContainer.inventory

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

					if (!otherStack.hasItemDelayPassed(itemDelay))
						continue

					val otherItem = otherStack?.item

					// Check if an item is the correct type, isn't bad and isn't already sorted
					if (isRightType(otherItem) && isStackUseful(otherStack, stacks) && !canBeSortedTo(otherIndex, otherItem, stacks.size)) {
						click(otherIndex, hotbarIndex, 2)

						delay(randomDelay(minSortDelay, maxSortDelay).toLong())

						break
					}
				}
			}
		}

		// Drop bad items
		if (drop) {
			for (index in thePlayer.inventoryContainer.inventorySlots.indices.shuffled(randomSlot)) {
				// Stop if player violates invopen or nomove checks
				if (!shouldExecute()) return

				if (index in TickScheduler)
					continue

				val stacks = thePlayer.inventoryContainer.inventory
				val stack = stacks.getOrNull(index) ?: continue

				if (!stack.hasItemDelayPassed(itemDelay))
					continue

				if (!isStackUseful(stack, stacks)) {
					click(index, 1, 4)
					delay(randomDelay(minDropDelay, maxDropDelay).toLong())
				}
			}
		}

		// Wait till all scheduled clicks were sent
		while (!TickScheduler.isEmpty()) {}

		// Close inventory
		if ((hasClicked && mc.currentScreen is GuiInventory && invOpen && autoClose) || (mc.currentScreen !is GuiInventory && simulateInventory && serverOpenInventory)) {
			delay(closeDelay.toLong())

			if (mc.currentScreen is GuiInventory)
				thePlayer.closeScreen()
			else if (serverOpenInventory)
				sendPacket(C0DPacketCloseWindow(thePlayer.openContainer.windowId))
		}
	}

	private val SORTING_VALUES = arrayOf(
		slot1Value, slot2Value, slot3Value, slot4Value, slot5Value, slot6Value, slot7Value, slot8Value, slot9Value
	)

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

	suspend fun click(slot: Int, button: Int, mode: Int) {
		val hadOpenInventory = serverOpenInventory

		if (simulateInventory && !serverOpenInventory)
			sendPacket(C16PacketClientStatus(OPEN_INVENTORY_ACHIEVEMENT))

		// Delay first click
		if (!hasClicked) {
			// If AutoArmor finished with open inventory, don't delay InventoryCleaner by another start delay
			if (!hadOpenInventory)
				delay(startDelay.toLong())

			hasClicked = true
		}

		TickScheduler.scheduleClick(slot, button, mode)
	}

	fun isStackUseful(stack: ItemStack?, stacks: List<ItemStack?>): Boolean {
		val item = stack?.item ?: return false

		return when (item) {
			in ITEMS_WHITELIST -> true

			is ItemFood, is ItemEnderPearl, is ItemEnchantedBook, is ItemBucket, is ItemBed -> true

			is ItemBoat, is ItemMinecart -> !ignoreVehicles

			is ItemBlock -> isUsefulBlock(stack)

			is ItemPotion -> isUsefulPotion(stack)

			is ItemArmor, is ItemTool, is ItemSword, is ItemBow -> isUsefulEquipment(stack, stacks)

			else -> false
		}
	}

	private fun isUsefulPotion(stack: ItemStack?): Boolean {
		val item = stack?.item ?: return false

		if (item is ItemPotion) {
			val isSplash = stack.isSplashPotion()
			val isHarmful = item.getEffects(stack).any { it.potionID in NEGATIVE_EFFECT_IDS }

			// Only keep helpful potions and, if 'onlyGoodPotions' is disabled, also splash harmful potions
			return !isHarmful || (!onlyGoodPotions && isSplash)
		}

		return false
	}

	private fun isUsefulEquipment(stack: ItemStack?, stacks: List<ItemStack?>): Boolean {
		val item = stack?.item ?: return false

		return when (item) {
			is ItemArmor -> getBestArmorSet(stacks)?.contains(stack) ?: true

			is ItemTool -> {
				val blockType = when (item) {
					is ItemAxe -> Blocks.log
					is ItemPickaxe -> Blocks.stone
					else -> Blocks.dirt
				}

				return hasBestParameters(stack, stacks) {
					it.item.getStrVsBlock(it, blockType) * it.durability
				}
			}

			is ItemSword ->
				hasBestParameters(stack, stacks) {
					it.attackDamage.toFloat()
				}

			is ItemBow ->
				hasBestParameters(stack, stacks) {
					it.getEnchantmentLevel(Enchantment.power).toFloat()
				}

			else -> true
		}
	}

	@Suppress("DEPRECATION")
	fun isUsefulBlock(stack: ItemStack?): Boolean {
		val item = stack?.item ?: return false

		if (item is ItemBlock) {
			val block = item.block

			return isFullBlock(block) && !block.hasTileEntity()
					&& block !is BlockWorkbench && block !is BlockContainer && block !is BlockFalling
		}

		return false
	}

	private fun hasBestParameters(stack: ItemStack?, stacks: List<ItemStack?>, parameters: (ItemStack) -> Float): Boolean {
		val item = stack?.item ?: return false

		val index = stacks.indexOf(stack)

		val currentStats = parameters(stack)

		val isSorted = canBeSortedTo(index, item, stacks.size)

		stacks.forEachIndexed { otherIndex, otherStack ->
			val otherItem = otherStack?.item ?: return@forEachIndexed

			// Check if items aren't the same instance but are the same type
			if (stack == otherStack || item.javaClass != otherItem.javaClass)
				return@forEachIndexed

			val otherStats = parameters(otherStack)

			val isOtherSorted = canBeSortedTo(otherIndex, otherItem, stacks.size)

			when (otherStats.compareTo(currentStats)) {
				1 -> return false
				0 -> when (otherStack.enchantmentSum.compareTo(stack.enchantmentSum)) {
					1 -> return false
					0 -> when (otherStack.totalDurability.compareTo(stack.totalDurability)) {
						1 -> return false
						0 -> if (!isSorted && index < otherIndex)
							return false
					}
				}
			}
		}

		return true
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