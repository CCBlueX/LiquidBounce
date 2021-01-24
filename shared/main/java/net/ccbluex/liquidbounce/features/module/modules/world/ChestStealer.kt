/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.inventory.IGuiChest
import net.ccbluex.liquidbounce.api.minecraft.inventory.ISlot
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.InventoryCleaner
import net.ccbluex.liquidbounce.utils.InventoryUtils.CLICK_TIMER
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import kotlin.random.Random

@ModuleInfo(name = "ChestStealer", description = "Automatically steals all items from a chest.", category = ModuleCategory.WORLD)
class ChestStealer : Module()
{

	/**
	 * OPTIONS
	 */

	// Pick Delay
	private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 200, 0, 400)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minDelayValue.get()
			if (i > newValue) set(i)
		}
	}

	private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 150, 0, 400)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxDelayValue.get()
			if (i < newValue) set(i)
		}
	}

	// Bypass: Delay on Pick first item
	private val delayOnFirstValue = BoolValue("DelayOnFirst", false)
	private val maxStartDelay: IntegerValue = object : IntegerValue("MaxFirstDelay", 0, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minStartDelay.get()
			if (i > newValue) this.set(i)
			nextDelay = TimeUtils.randomDelay(minStartDelay.get(), this.get())
		}
	}

	private val minStartDelay: IntegerValue = object : IntegerValue("MinFirstDelay", 0, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxStartDelay.get()
			if (i < newValue) this.set(i)

			nextDelay = TimeUtils.randomDelay(this.get(), maxStartDelay.get())
		}
	}

	// Bypass: Take items with randomized order
	private val takeRandomizedValue = BoolValue("TakeRandomized", false)

	// Bypass: Add some click mistakes
	private val allowMisclicksValue = BoolValue("ClickMistakes", false)
	private val misclicksRateValue = IntegerValue("ClickMistakeRate", 5, 1, 100)
	private val maxAllowedMisclicksPerChestValue = IntegerValue("MaxClickMistakesPerChest", 5, 1, 10)

	// Pick Options
	private val onlyItemsValue = BoolValue("OnlyItems", false)
	private val noCompassValue = BoolValue("NoCompass", false)
	private val invCleanBeforeSteal = BoolValue("PerformInvCleanBeforeSteal", true)

	// AutoClose
	private val autoCloseValue = BoolValue("AutoClose", true)
	private val autoCloseMaxDelayValue: IntegerValue = object : IntegerValue("AutoCloseMaxDelay", 0, 0, 400)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = autoCloseMinDelayValue.get()
			if (i > newValue) set(i)
			nextCloseDelay = TimeUtils.randomDelay(autoCloseMinDelayValue.get(), this.get())
		}
	}

	private val autoCloseMinDelayValue: IntegerValue = object : IntegerValue("AutoCloseMinDelay", 0, 0, 400)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = autoCloseMaxDelayValue.get()
			if (i < newValue) set(i)
			nextCloseDelay = TimeUtils.randomDelay(this.get(), autoCloseMaxDelayValue.get())
		}
	}

	// Close Condition Options
	private val closeOnFullValue = BoolValue("CloseOnFull", true)

	// Bypass
	private val chestTitleValue = BoolValue("ChestTitle", false)

	// Visible
	private val indicateClick = BoolValue("ClickIndication", true)
	private val indicateLength = IntegerValue("ClickIndicationLength", 100, 50, 200)

	/**
	 * VALUES
	 */

	private val delayTimer = MSTimer()
	private var nextDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

	private val autoCloseTimer = MSTimer()
	private var nextCloseDelay = TimeUtils.randomDelay(autoCloseMinDelayValue.get(), autoCloseMaxDelayValue.get())

	private var contentReceived = 0

	// Remaining Misclicks count
	private var remainingMisclickCount = maxAllowedMisclicksPerChestValue.get()

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent?)
	{
		val thePlayer = mc.thePlayer ?: return

		if (!classProvider.isGuiChest(mc.currentScreen) || mc.currentScreen == null)
		{
			if (delayOnFirstValue.get())
			{
				delayTimer.reset()
				if (nextDelay < minStartDelay.get()) nextDelay = TimeUtils.randomDelay(minStartDelay.get(), maxStartDelay.get())
			}
			autoCloseTimer.reset()
			return
		}

		if (!delayTimer.hasTimePassed(nextDelay))
		{
			autoCloseTimer.reset()
			return
		}

		val screen = (mc.currentScreen ?: return).asGuiChest()

		// No Compass
		if (noCompassValue.get() && thePlayer.inventory.getCurrentItemInHand()?.item?.unlocalizedName == "item.compass") return

		// Chest title
		if (chestTitleValue.get() && (screen.lowerChestInventory == null || !screen.lowerChestInventory!!.name.contains(classProvider.createItemStack(functions.getObjectFromItemRegistry(classProvider.createResourceLocation("minecraft:chest"))!!).displayName))) return

		// inventory cleaner
		val inventoryCleaner = LiquidBounce.moduleManager[InventoryCleaner::class.java] as InventoryCleaner

		// Is empty?
		val notEmpty = !this.isEmpty(screen)

		// Perform the InventoryCleaner before start stealing if option is present and InventoryCleaner is enabled. This will be helpful if player's inventory is nearly fucked up with tons of garbage. The settings of InventoryCleaner is depends on InventoryCleaner's official settings.
		if (notEmpty && invCleanBeforeSteal.get() && inventoryCleaner.state && !inventoryCleaner.cleanInventory(start = screen.inventoryRows * 9,
				end = screen.inventoryRows * 9 + if (inventoryCleaner.hotbarValue.get()) 36 else 27,
				timer = CLICK_TIMER,
				container = screen.inventorySlots!!,
				delayResetFunc = Runnable { nextDelay = TimeUtils.randomDelay(inventoryCleaner.minDelayValue.get(), inventoryCleaner.maxDelayValue.get()) })
		) return

		if (notEmpty && (!closeOnFullValue.get() || !fullInventory))
		{
			autoCloseTimer.reset()

			// Pick Randomized
			if (takeRandomizedValue.get())
			{
				do
				{
					val items = (0 until screen.inventoryRows * 9).map(screen.inventorySlots!!::getSlot).filter {
						it.stack != null && (!onlyItemsValue.get() || !classProvider.isItemBlock(it.stack!!.item)) && (!inventoryCleaner.state || inventoryCleaner.isUseful(it.stack!!, -1))
					}

					val randomSlot = Random.nextInt(items.size)
					var slot = items[randomSlot]

					var misclick = false

					// Simulate Click Mistakes to bypass some anti-cheats
					if (allowMisclicksValue.get() && remainingMisclickCount > 0 && misclicksRateValue.get() > 0 && Random.nextInt(100) <= misclicksRateValue.get())
					{
						val firstEmpty: ISlot? = firstEmpty(screen.inventorySlots!!.inventorySlots, screen.inventoryRows * 9, true)
						if (firstEmpty != null)
						{
							slot = firstEmpty
							remainingMisclickCount--
							misclick = true
						}
					}

					move(screen, slot, misclick)
				} while (delayTimer.hasTimePassed(nextDelay) && items.isNotEmpty())
				return
			}

			// Non randomized
			for (slotIndex in screen.inventoryRows * 9 - 1 downTo 0) // Reversed-direction
			{
				var slot = screen.inventorySlots!!.getSlot(slotIndex)
				val stack = slot.stack

				if (delayTimer.hasTimePassed(nextDelay) && shouldTake(stack, inventoryCleaner))
				{
					var misclick = false

					if (allowMisclicksValue.get() && remainingMisclickCount > 0 && misclicksRateValue.get() > 0 && Random.nextInt(100) <= misclicksRateValue.get())
					{
						val firstEmpty: ISlot? = firstEmpty(screen.inventorySlots!!.inventorySlots, screen.inventoryRows * 9, false)
						if (firstEmpty != null)
						{
							slot = firstEmpty
							remainingMisclickCount--
							misclick = true
						}
					}

					move(screen, slot, misclick)
				}
			}
		} else if (autoCloseValue.get() && screen.inventorySlots!!.windowId == contentReceived && autoCloseTimer.hasTimePassed(nextCloseDelay))
		{
			thePlayer.closeScreen()
			nextCloseDelay = TimeUtils.randomDelay(autoCloseMinDelayValue.get(), autoCloseMaxDelayValue.get())
		}
	}

	@EventTarget
	private fun onPacket(event: PacketEvent)
	{
		val packet = event.packet

		if (classProvider.isSPacketWindowItems(packet)) contentReceived = packet.asSPacketWindowItems().windowId
	}

	private fun shouldTake(stack: IItemStack?, inventoryCleaner: InventoryCleaner): Boolean =
		stack != null && !ItemUtils.isStackEmpty(stack) && (!onlyItemsValue.get() || !classProvider.isItemBlock(stack.item)) && (!inventoryCleaner.state || inventoryCleaner.isUseful(stack, -1))

	private fun move(screen: IGuiChest, slot: ISlot, misclick: Boolean)
	{
		screen.handleMouseClick(slot, slot.slotNumber, 0, 1)
		if (indicateClick.get()) screen.asGuiContainer().highlight(slot.slotNumber, indicateLength.get().toLong(), if (misclick) -2130771968 /* 0x80FF0000 */ else -2147418368 /* 0x8000FF00 */)
		delayTimer.reset()
		nextDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
	}

	private fun isEmpty(chest: IGuiChest): Boolean
	{
		val inventoryCleaner = LiquidBounce.moduleManager[InventoryCleaner::class.java] as InventoryCleaner

		for (i in 0 until chest.inventoryRows * 9)
		{
			val slot = chest.inventorySlots!!.getSlot(i)

			val stack = slot.stack

			if (shouldTake(stack, inventoryCleaner)) return false
		}

		return true
	}

	private fun firstEmpty(slots: List<ISlot>?, length: Int, random: Boolean): ISlot?
	{
		slots ?: return null
		val emptySlots = (0 until length).map { slots[it] }.filter { it.stack == null }.toMutableList()

		if (emptySlots.isEmpty()) return null

		return if (random) emptySlots[Random.nextInt(emptySlots.size)] else emptySlots.first()
	}

	private val fullInventory: Boolean
		get() = mc.thePlayer?.inventory?.mainInventory?.none(ItemUtils::isStackEmpty) ?: false

	override val tag: String
		get() = "${minDelayValue.get()} ~ ${maxDelayValue.get()}"
}
