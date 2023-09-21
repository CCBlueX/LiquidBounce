@file:Suppress("ControlFlowWithEmptyBody")

package net.ccbluex.liquidbounce.features.module.modules.beta

import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.movement.InventoryMove
import net.ccbluex.liquidbounce.utils.CoroutineUtils.waitUntil
import net.ccbluex.liquidbounce.utils.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.item.CoroutineArmorComparator.getBestArmorSet
import net.ccbluex.liquidbounce.utils.item.hasItemAgePassed
import net.ccbluex.liquidbounce.utils.timer.TimeUtils.randomDelay
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.item.ItemArmor
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C16PacketClientStatus
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT

object CoroutineArmorer: Module("CoroutineArmorer", ModuleCategory.BETA) {
	private val maxDelay: Int by object : IntegerValue("MaxDelay", 50, 0..500) {
		override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minDelay)
	}
	private val minDelay by object : IntegerValue("MinDelay", 50, 0..500) {
		override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxDelay)

		override fun isSupported() = maxDelay > 0
	}
	private val minItemAge by IntegerValue("MinItemAge", 0, 0..2000)

	private val invOpen by BoolValue("InvOpen", false)
	private val simulateInventory by BoolValue("SimulateInventory", true) { !invOpen }
	private val autoClose by BoolValue("AutoClose", false) { invOpen }

	private val startDelay by IntegerValue("StartDelay", 0, 0..500) { invOpen || simulateInventory }
	private val closeDelay by IntegerValue("CloseDelay", 0, 0..500) { (invOpen && autoClose) || simulateInventory }

	// When swapping armor pieces, it grabs the better one, drags and swaps it with equipped one and drops the equipped one (no time of having no armor piece equipped)
	// Has to make more clicks, works slower
	private val smartSwap by BoolValue("SmartSwap", true)

	private val noMove by BoolValue("NoMoveClicks", false)
	private val noMoveAir by BoolValue("NoClicksInAir", false) { noMove }
	private val noMoveGround by BoolValue("NoClicksOnGround", true) { noMove }

	private val hotbar by BoolValue("Hotbar", true)
	// Sacrifices 1 tick speed for complete undetectability, needed to bypass Vulcan, Matrix
	private val delayedSlotSwitch by BoolValue("DelayedSlotSwitch", true) { hotbar }
	// Prevents AutoArmor from hotbar equipping while any screen is open
	private val onlyWhenNoScreen by BoolValue("OnlyWhenNoScreen", false) { hotbar }

	private var hasClicked = false

	private suspend fun shouldExecute(onlyHotbar: Boolean = false): Boolean {
		while (true) {
			if (!state)
				return false

			// It is impossible to equip armor when a container is open; only try to equip by right-clicking from hotbar (if onlyWhenNoScreen is disabled)
			if (mc.thePlayer?.openContainer?.windowId != 0 && (!onlyHotbar || onlyWhenNoScreen))
				return false

			// Player doesn't need to have inventory open or not to move, when equipping from hotbar
			if (onlyHotbar)
				return true

			if (invOpen && (mc.currentScreen !is GuiInventory || !serverOpenInventory))
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
		if (!state)
			return

		val thePlayer = mc.thePlayer ?: return

		if (hotbar) {
			if (!shouldExecute(onlyHotbar = true))
				return

			val stacks = thePlayer.openContainer.inventory

			val bestArmorSet = getBestArmorSet(stacks) ?: return

			var hasClickedHotbar = false

			for (hotbarIndex in 0..8) {
				if (!shouldExecute(onlyHotbar = true))
					return

				// Don't right-click to equip items while inventory is open when value onlyWhenNoScreen is enabled
				if (onlyWhenNoScreen && serverOpenInventory)
					break

				val stack = stacks.getOrNull(hotbarIndex + 36) ?: continue

				if (stack !in bestArmorSet)
					continue

				val equippedStack = stacks[(stack.item as ItemArmor).armorType + 5]

				// If armor slot isn't occupied, right click to equip
				if (equippedStack == null) {
					hasClickedHotbar = true

					if (delayedSlotSwitch) {
						// Schedule for the following tick and wait for them to be sent
						TickScheduler.scheduleAndSuspend({
							sendPackets(
								C09PacketHeldItemChange(hotbarIndex),
								C08PacketPlayerBlockPlacement(stack)
							)
						})
					} else {
						// Schedule all possible hotbar clicks for the following tick (doesn't suspend the loop)
						TickScheduler += {
							// Switch selected hotbar slot, right click to equip
							sendPackets(
								C09PacketHeldItemChange(hotbarIndex),
								C08PacketPlayerBlockPlacement(stack)
							)

							// Move the armor on the client-side to prevent repeated clicks (until server updates the inventory)
							thePlayer.openContainer.putStackInSlot(36 + hotbarIndex, null)
							thePlayer.openContainer.putStackInSlot((stack.item as ItemArmor).armorType + 5, stack)
						}
					}
				}
			}

			waitUntil { TickScheduler.isEmpty() }

			// Sync selected slot
			if (hasClickedHotbar)
				mc.playerController.updateController()
		}

		hasClicked = false

		for (armorType in 0..4) {
			if (!shouldExecute())
				return

			val stacks = thePlayer.openContainer.inventory

			val armorSet = getBestArmorSet(stacks) ?: continue

			// Shouldn't iterate over armor set because after waiting for nomove and invopen it could be outdated
			val (index, stack) = armorSet[armorType] ?: continue

			// Index is null when searching in chests for already equipped armor to prevent any accidental impossible interactions
			index ?: continue

			// Check if best item is already scheduled to be equipped next tick
			if (index in TickScheduler || (armorType + 5) in TickScheduler)
				continue

			if (!stack.hasItemAgePassed(minItemAge))
				continue

			when (stacks[armorType + 5]) {
				// Best armor is already equipped
				stack -> continue

				// No item is equipped in armor slot
				null ->
					// Equip by shift-clicking
					click(index, 0, 1)

				else -> {
					if (smartSwap) {
						// Player has worse armor equipped, drag the best armor, swap it with currently equipped armor and drop the bad armor
						// This way there is no time of having no armor (but more clicks)

						// Grab better armor
						click(index, 0, 0)

						// Swap it with currently equipped armor
						click(armorType + 5, 0, 0)

						// Drop worse item by dragging and dropping it
						click(-999, 0, 0)
					} else {
						// Normal version

						// Drop worse armor
						click(armorType + 5, 0, 4)

						// Equip better armor
						click(index, 0, 1)
					}

					// TODO: Make stable without this, can't instantly equip whole armor set without this, keeps dropping useful pieces
					// For stability, it is better to sync with tick loop
					waitUntil { TickScheduler.isEmpty() }
				}
			}
		}

		// Wait till all scheduled clicks were sent
		waitUntil { TickScheduler.isEmpty() }

		// Close inventory, doesn't have to close simulated inventory because it will get closed after inventory cleaner finishes
		if (hasClicked && mc.currentScreen is GuiInventory && invOpen && autoClose) {
			delay(closeDelay.toLong())

			if (mc.currentScreen is GuiInventory)
				thePlayer.closeScreen()
		}
	}

	suspend fun click(slot: Int, button: Int, mode: Int, allowDuplicates: Boolean = false) {
		if (simulateInventory && !serverOpenInventory) {
			sendPacket(C16PacketClientStatus(OPEN_INVENTORY_ACHIEVEMENT))
		}

		// Delay first click
		if (!hasClicked) {
			delay(startDelay.toLong())
			hasClicked = true
		}

		TickScheduler.scheduleClick(slot, button, mode, allowDuplicates)

		delay(randomDelay(minDelay, maxDelay).toLong())
	}
}