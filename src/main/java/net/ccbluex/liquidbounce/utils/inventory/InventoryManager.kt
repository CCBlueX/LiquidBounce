/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.inventory

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoArmor
import net.ccbluex.liquidbounce.features.module.modules.player.InventoryCleaner
import net.ccbluex.liquidbounce.features.module.modules.world.ChestStealer
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.serverOnGround
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.gui.inventory.GuiInventory

object InventoryManager: MinecraftInstance() {

	// Shared no move click values
	val noMoveValue = BoolValue("NoMoveClicks", false)
		val noMoveAirValue = BoolValue("NoClicksInAir", false) { noMoveValue.get() }
		val noMoveGroundValue = BoolValue("NoClicksOnGround", true) { noMoveValue.get() }

	// Shared values between AutoArmor and InventoryCleaner
	val invOpenValue = BoolValue("InvOpen", false)
		val simulateInventoryValue = BoolValue("SimulateInventory", true) { !invOpenValue.get() }
		val autoCloseValue = BoolValue("AutoClose", false) { invOpenValue.get() }

		val startDelayValue = IntegerValue("StartDelay", 0, 0..500)
			{ invOpenValue.get() || simulateInventoryValue.get() }
		val closeDelayValue = IntegerValue("CloseDelay", 0, 0..500)
			{ if (invOpenValue.get()) autoCloseValue.get() else simulateInventoryValue.get() }

	// Shared highlight slot values between AutoArmor and InventoryCleaner
	val highlightSlotValue = BoolValue("Highlight-Slot", false, subjective = true)

	// Shared highlight slot background values between AutoArmor and InventoryCleaner
	val backgroundRedValue = IntegerValue("Background-R", 128, 0..255, subjective = true) { highlightSlotValue.get() }
	val backgroundGreenValue = IntegerValue("Background-G", 128, 0..255, subjective = true) { highlightSlotValue.get() }
	val backgroundBlueValue = IntegerValue("Background-B", 128, 0..255, subjective = true) { highlightSlotValue.get() }
	val backgroundAlphaValue = IntegerValue("Background-Alpha", 128, 0..255, subjective = true) { highlightSlotValue.get() }

	// Shared highlight slot border values between AutoArmor and InventoryCleaner
	val borderStrength = IntegerValue("Border-Strength", 3, 1..5, subjective = true) { highlightSlotValue.get() }
	val borderRed = IntegerValue("Border-R", 128, 0..255, subjective = true) { highlightSlotValue.get() }
	val borderGreen = IntegerValue("Border-G", 128, 0..255, subjective = true) { highlightSlotValue.get() }
	val borderBlue = IntegerValue("Border-B", 128, 0..255, subjective = true) { highlightSlotValue.get() }
	val borderAlpha = IntegerValue("Border-Alpha", 255, 0..255, subjective = true) { highlightSlotValue.get() }

	// Undetectable
	val undetectableValue = BoolValue("Undetectable", false)

	private val inventoryWorker = CoroutineScope(Dispatchers.Default + SupervisorJob())

	var hasScheduledInLastLoop = false
		set(value) {
			// If hasScheduled gets set to true any time during the searching loop, inventory can be closed when the loop finishes.
			if (value) canCloseInventory = true

			field = value
		}

	private var canCloseInventory = false

	// ChestStealer Highlight
	var chestStealerCurrentSlot = -1
	var chestStealerLastSlot = -1

	// InventoryCleaner Highlight
	var invCleanerCurrentSlot = -1
	var invCleanerLastSlot = -1

	// AutoArmor Highlight
	var autoArmorCurrentSlot = -1
	var autoArmorLastSlot = -1

	private suspend fun manageInventory() {
		while (inventoryWorker.isActive) {
			try {
				/**
				 * ChestStealer actions
				 */

				ChestStealer.stealFromChest()

				/**
				 * AutoArmor actions
				 */

				AutoArmor.equipFromHotbar()

				// Following actions require inventory / simulated inventory, ...

				// TODO: This could be at start of each action?
				// Don't wait for NoMove not to be violated, check if there is anything to equip from hotbar and such by looping again
				if (!canClickInventory() || (invOpenValue.get() && mc.currentScreen !is GuiInventory)) {
					delay(50)
					continue
				}

				canCloseInventory = false

				AutoArmor.equipFromInventory()

				/**
				 * InventoryCleaner actions
				 */

				// Repair useful equipment by merging in the crafting grid
				InventoryCleaner.repairEquipment()

				// Compact multiple small stacks into one to free up inventory space
				InventoryCleaner.mergeStacks()

				// Sort hotbar (with useful items without even dropping bad items first)
				InventoryCleaner.sortHotbar()

				// Drop bad items to free up inventory space
				InventoryCleaner.dropGarbage()

				// Stores which action should be executed to close open inventory or simulated inventory
				// If no clicks were scheduled throughout any iteration (canCloseInventory == false), then it is null, to prevent closing inventory all the time
				val action = closingAction
				if (action == null) {
					delay(50)
					continue
				}

				// Prepare for closing the inventory
				delay(closeDelayValue.get().toLong())

				// Try to search through inventory one more time, only close when no actions were scheduled in current iteration
				if (!hasScheduledInLastLoop) {
					action.invoke()
				}
			} catch (e: Exception) {
				// TODO: Remove when stable, probably in b86
				displayChatMessage("§cReworked coroutine inventory management ran into an issue! Please report this: ${e.message ?: e.cause}")
				e.printStackTrace()
			}
		}
	}

	private val closingAction
		get() = when {
			// Check if any click was scheduled since inventory got open
			!canCloseInventory -> null

			// Prevent any other container guis from getting closed
			mc.thePlayer?.openContainer?.windowId != 0 -> null

			// Check if open inventory should be closed
			mc.currentScreen is GuiInventory && invOpenValue.get() && autoCloseValue.get() ->
				({ mc.thePlayer?.closeScreen() })

			// Check if simulated inventory should be closed
			mc.currentScreen !is GuiInventory && simulateInventoryValue.get() && serverOpenInventory ->
				({ serverOpenInventory = false })

			else -> null
		}

	fun canClickInventory(closeWhenViolating: Boolean = false) =
		if (noMoveValue.get() && isMoving && if (serverOnGround) noMoveGroundValue.get() else noMoveAirValue.get()) {

			// NoMove check is violated, close simulated inventory
			if (closeWhenViolating)
				serverOpenInventory = false

			false
		} else true // Simulated inventory will get reopen before a window click, delaying it by start delay

	fun startCoroutine() = inventoryWorker.launch {
		manageInventory()
	}
}
