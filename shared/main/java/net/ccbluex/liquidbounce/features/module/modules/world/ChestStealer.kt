/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.gui.inventory.IGuiChest
import net.ccbluex.liquidbounce.api.minecraft.inventory.IContainer
import net.ccbluex.liquidbounce.api.minecraft.inventory.ISlot
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.InventoryCleaner
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.utils.timer.Cooldown
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import kotlin.math.max
import kotlin.random.Random

// TODO: Silent option (Steal without opening GUI)
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
	// private val invCleanBeforeSteal = BoolValue("PerformInvCleanBeforeSteal", true) // Disabled due bug

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
	private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)

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

	private val infoUpdateCooldown = Cooldown.getNewCooldownMiliseconds(100)

	private var cachedInfo: String? = null

	val advancedInformations: String
		get()
		{
			val cache = cachedInfo

			return if (cache == null || infoUpdateCooldown.attemptReset()) (if (!state) "ChestStealer is not active"
			else
			{
				val minStartDelay = minStartDelay.get()
				val maxStartDelay = maxStartDelay.get()
				val minDelay = minDelayValue.get()
				val maxDelay = maxDelayValue.get()
				val invCleanerState = LiquidBounce.moduleManager[InventoryCleaner::class.java].state
				val random = takeRandomizedValue.get()
				val autoClose = autoCloseValue.get()
				val minAutoClose = autoCloseMinDelayValue.get()
				val maxAutoClose = autoCloseMaxDelayValue.get()
				val itemDelay = itemDelayValue.get()
				val misclick = allowMisclicksValue.get()
				val misclickRate = misclicksRateValue.get()
				val maxmisclick = maxAllowedMisclicksPerChestValue.get()

				"ChestStealer active [startdelay: ($minStartDelay ~ $maxStartDelay), delay: ($minDelay ~ $maxDelay), itemdelay: $itemDelay, random: $random, onlyuseful: $invCleanerState${if (misclick) ", misclick($misclickRate%, max $maxmisclick per chest)" else ""}${if (autoClose) ", autoclose($minAutoClose ~ $maxAutoClose)" else ""}]"
			}).apply { cachedInfo = this }
			else cache
		}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent?)
	{
		val thePlayer = mc.thePlayer ?: return

		val provider = classProvider

		if (!provider.isGuiChest(mc.currentScreen))
		{
			if (delayOnFirstValue.get() || itemDelayValue.get() > 0)
			{
				delayTimer.reset()
				if (nextDelay < minStartDelay.get()) nextDelay = max(TimeUtils.randomDelay(minStartDelay.get(), maxStartDelay.get()), itemDelayValue.get().toLong())
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
		val lowerChestInventory = screen.lowerChestInventory

		// No Compass
		if (noCompassValue.get() && thePlayer.inventory.getCurrentItemInHand()?.item?.unlocalizedName == "item.compass") return

		// Chest title
		if (chestTitleValue.get() && (lowerChestInventory == null || !lowerChestInventory.name.contains(functions.getObjectFromItemRegistry(provider.createResourceLocation("minecraft:chest"))?.let { provider.createItemStack(it).displayName } ?: "Chest"))) return

		// inventory cleaner
		val inventoryCleaner = LiquidBounce.moduleManager[InventoryCleaner::class.java] as InventoryCleaner

		// Is empty?
		val notEmpty = !this.isEmpty(thePlayer, screen)

		val container = screen.inventorySlots ?: return
		val end = screen.inventoryRows * 9

		// Disabled due bug
		// // Perform the InventoryCleaner before start stealing if option is present and InventoryCleaner is enabled. This will be helpful if player's inventory is nearly fucked up with tons of garbage. The settings of InventoryCleaner is depends on InventoryCleaner's official settings.
		// if (notEmpty && invCleanBeforeSteal.get() && inventoryCleaner.state && !inventoryCleaner.cleanInventory(start = end, end = end + if (inventoryCleaner.hotbarValue.get()) 36 else 27, timer = InventoryUtils.CLICK_TIMER, container = container, delayResetFunc = Runnable { nextDelay = TimeUtils.randomDelay(inventoryCleaner.minDelayValue.get(), inventoryCleaner.maxDelayValue.get()) })) return

		if (notEmpty && (!closeOnFullValue.get() || !getFullInventory(thePlayer)))
		{
			autoCloseTimer.reset()

			// Pick Randomized
			if (takeRandomizedValue.get())
			{
				do
				{
					val items = (0 until end).map(container::getSlot).filter { shouldTake(thePlayer, it.stack, it.slotNumber, inventoryCleaner, end, container) }.toList()

					val randomSlot = Random.nextInt(items.size)
					var slot = items[randomSlot]

					var misclick = false

					// Simulate Click Mistakes to bypass some anti-cheats
					if (allowMisclicksValue.get() && remainingMisclickCount > 0 && misclicksRateValue.get() > 0 && Random.nextInt(100) <= misclicksRateValue.get())
					{
						val firstEmpty: ISlot? = firstEmpty(container.inventorySlots, end, true)
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
			for (slotIndex in end - 1 downTo 0) // Reversed-direction
			{
				var slot = container.getSlot(slotIndex)
				val stack = slot.stack

				if (delayTimer.hasTimePassed(nextDelay) && shouldTake(thePlayer, stack, slot.slotNumber, inventoryCleaner, end, container))
				{
					var misclick = false

					if (allowMisclicksValue.get() && remainingMisclickCount > 0 && misclicksRateValue.get() > 0 && Random.nextInt(100) <= misclicksRateValue.get())
					{
						val firstEmpty: ISlot? = firstEmpty(container.inventorySlots, end, false)
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
		}
		else if (autoCloseValue.get() && container.windowId == contentReceived && autoCloseTimer.hasTimePassed(nextCloseDelay))
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

	private fun shouldTake(thePlayer: IEntityPlayerSP, stack: IItemStack?, slot: Int, inventoryCleaner: InventoryCleaner, end: Int, container: IContainer): Boolean = stack != null && (!onlyItemsValue.get() || !classProvider.isItemBlock(stack.item)) && (System.currentTimeMillis() - stack.itemDelay >= itemDelayValue.get() && (!inventoryCleaner.state || inventoryCleaner.isUseful(thePlayer, slot, stack, end = end, container = container) && inventoryCleaner.isUseful(thePlayer, -1, stack, container = thePlayer.inventoryContainer) /* 상자 안에서 가장 좋은 템이랑 인벤 안의 가장 좋은 템이랑 비교한 후, 상자 안의 것이 더 좋을 경우에만 가져가기 */))

	private fun move(screen: IGuiChest, slot: ISlot, misclick: Boolean)
	{
		screen.handleMouseClick(slot, slot.slotNumber, 0, 1)
		if (indicateClick.get()) screen.asGuiContainer().highlight(slot.slotNumber, indicateLength.get().toLong(), if (misclick) -2130771968 /* 0x80FF0000 */ else -2147418368 /* 0x8000FF00 */)
		delayTimer.reset()
		nextDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
	}

	private fun isEmpty(thePlayer: IEntityPlayerSP, chest: IGuiChest): Boolean
	{
		val inventoryCleaner = LiquidBounce.moduleManager[InventoryCleaner::class.java] as InventoryCleaner
		val container = chest.inventorySlots ?: return false
		val end = chest.inventoryRows * 9

		return (0 until end).map(container::getSlot).none { shouldTake(thePlayer, it.stack, it.slotNumber, inventoryCleaner, end, container) }
	}

	private fun firstEmpty(slots: List<ISlot>?, length: Int, random: Boolean): ISlot?
	{
		slots ?: return null
		val emptySlots = (0 until length).map { slots[it] }.filter { it.stack == null }.toMutableList()

		if (emptySlots.isEmpty()) return null

		return if (random) emptySlots[Random.nextInt(emptySlots.size)] else emptySlots.first()
	}

	private fun getFullInventory(thePlayer: IEntityPlayerSP): Boolean = thePlayer.inventory.mainInventory.none(ItemUtils::isStackEmpty)

	override val tag: String
		get() = "${minDelayValue.get()} ~ ${maxDelayValue.get()}"
}
