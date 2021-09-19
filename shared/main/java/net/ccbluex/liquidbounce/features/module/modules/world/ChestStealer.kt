/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
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
import net.ccbluex.liquidbounce.value.IntegerRangeValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ValueGroup
import kotlin.math.max
import kotlin.random.Random

// TODO: Silent option (Steal without opening GUI)
@ModuleInfo(name = "ChestStealer", description = "Automatically steals all items from a chest.", category = ModuleCategory.WORLD)
class ChestStealer : Module()
{
	/**
	 * OPTIONS
	 */

	private val delayValue = IntegerRangeValue("Delay", 150, 200, 0, 2000, "MaxDelay" to "MinDelay")
	private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)

	private val takeRandomizedValue = BoolValue("TakeRandomized", false)
	private val onlyItemsValue = BoolValue("OnlyItems", false)
	private val noCompassValue = BoolValue("NoCompass", false)
	// private val invCleanBeforeSteal = BoolValue("PerformInvCleanBeforeSteal", true) // Disabled due bug

	private val chestTitleValue = BoolValue("ChestTitle", false)

	private val delayOnFirstGroup = ValueGroup("DelayOnFirst")
	private val delayOnFirstEnabledValue = BoolValue("Enabled", false, "DelayOnFirst")
	private val delayOnFirstDelayValue: IntegerRangeValue = object : IntegerRangeValue("Delay", 0, 0, 0, 2000, "MaxFirstDelay" to "MinFirstDelay")
	{
		override fun onMaxValueChanged(oldValue: Int, newValue: Int)
		{
			nextDelay = TimeUtils.randomDelay(getMin(), newValue)
		}

		override fun onMinValueChanged(oldValue: Int, newValue: Int)
		{
			nextDelay = TimeUtils.randomDelay(newValue, getMax())
		}
	}

	private val misclickGroup = ValueGroup("ClickMistakes")
	private val misclickEnabledValue = BoolValue("Enabled", false, "ClickMistakes")
	private val misclickRateValue = IntegerValue("Rate", 5, 1, 100, "ClickMistakeRate")
	private val misclickMaxPerChestValue = IntegerValue("MaxPerChest", 5, 1, 10, "MaxClickMistakesPerChest")

	private val autoCloseGroup = ValueGroup("AutoClose")
	private val autoCloseEnabledValue = BoolValue("Enabled", true, "AutoClose")
	private val autoCloseOnFullValue = BoolValue("CloseOnFull", true, "CloseOnFull")
	private val autoCloseDelayValue: IntegerRangeValue = object : IntegerRangeValue("Delay", 0, 0, 0, 2000, "AutoCloseMaxDelay" to "AutoCloseMinDelay")
	{
		override fun onMaxValueChanged(oldValue: Int, newValue: Int)
		{
			nextCloseDelay = TimeUtils.randomDelay(getMin(), newValue)
		}

		override fun onMinValueChanged(oldValue: Int, newValue: Int)
		{
			nextCloseDelay = TimeUtils.randomDelay(newValue, getMax())
		}
	}

	private val clickIndicationGroup = ValueGroup("ClickIndication")
	private val clickIndicationEnabledValue = BoolValue("Enabled", true, "ClickIndication")
	private val clickIndicationLengthValue = IntegerValue("Length", 100, 50, 1000, "ClickIndicationLength")

	/**
	 * VALUES
	 */

	private val delayTimer = MSTimer()
	private var nextDelay = delayValue.getRandomDelay()

	private val autoCloseTimer = MSTimer()
	private var nextCloseDelay = autoCloseDelayValue.getRandomDelay()

	private var contentReceived = 0

	// Remaining Misclicks count
	private var remainingMisclickCount = misclickMaxPerChestValue.get()

	private val infoUpdateCooldown = Cooldown.getNewCooldownMiliseconds(100)

	private var cachedInfo: String? = null

	val advancedInformations: String
		get()
		{
			val cache = cachedInfo

			return if (cache == null || infoUpdateCooldown.attemptReset()) (if (!state) "ChestStealer is not active"
			else
			{
				val minStartDelay = delayOnFirstDelayValue.getMin()
				val maxStartDelay = delayOnFirstDelayValue.getMax()
				val minDelay = delayValue.getMin()
				val maxDelay = delayValue.getMax()
				val invCleanerState = LiquidBounce.moduleManager[InventoryCleaner::class.java].state
				val random = takeRandomizedValue.get()
				val autoClose = autoCloseEnabledValue.get()
				val minAutoClose = autoCloseDelayValue.getMin()
				val maxAutoClose = autoCloseDelayValue.getMax()
				val itemDelay = itemDelayValue.get()
				val misclick = misclickEnabledValue.get()
				val misclickRate = misclickRateValue.get()
				val maxmisclick = misclickMaxPerChestValue.get()

				"ChestStealer active [startdelay: ($minStartDelay ~ $maxStartDelay), delay: ($minDelay ~ $maxDelay), itemdelay: $itemDelay, random: $random, onlyuseful: $invCleanerState${if (misclick) ", misclick($misclickRate%, max $maxmisclick per chest)" else ""}${if (autoClose) ", autoclose($minAutoClose ~ $maxAutoClose)" else ""}]"
			}).apply { cachedInfo = this }
			else cache
		}

	init
	{
		delayOnFirstGroup.addAll(delayOnFirstEnabledValue, delayOnFirstDelayValue)
		misclickGroup.addAll(misclickEnabledValue, misclickRateValue, misclickMaxPerChestValue)
		autoCloseGroup.addAll(autoCloseEnabledValue, autoCloseEnabledValue, autoCloseOnFullValue, autoCloseDelayValue)
		clickIndicationGroup.addAll(clickIndicationEnabledValue, clickIndicationLengthValue)
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val provider = classProvider

		val itemDelay = itemDelayValue.get().toLong()

		if (!provider.isGuiChest(mc.currentScreen))
		{
			if (delayOnFirstEnabledValue.get() || itemDelay > 0L)
			{
				delayTimer.reset()
				if (nextDelay < delayOnFirstDelayValue.getMin()) nextDelay = max(delayOnFirstDelayValue.getRandomDelay(), itemDelay)
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
		val notEmpty = !this.isEmpty(thePlayer, screen, itemDelay)

		val container = screen.inventorySlots ?: return
		val end = screen.inventoryRows * 9

		if (container.windowId != contentReceived)
		{
			autoCloseTimer.reset()
			return
		}

		// Disabled due bug
		// // Perform the InventoryCleaner before start stealing if option is present and InventoryCleaner is enabled. This will be helpful if player's inventory is nearly fucked up with tons of garbage. The settings of InventoryCleaner is depends on InventoryCleaner's official settings.
		// if (notEmpty && invCleanBeforeSteal.get() && inventoryCleaner.state && !inventoryCleaner.cleanInventory(start = end, end = end + if (inventoryCleaner.hotbarValue.get()) 36 else 27, timer = InventoryUtils.CLICK_TIMER, container = container, delayResetFunc = Runnable { nextDelay = TimeUtils.randomDelay(inventoryCleaner.minDelayValue.get(), inventoryCleaner.maxDelayValue.get()) })) return

		if (notEmpty && (!autoCloseOnFullValue.get() || !getFullInventory(thePlayer)))
		{
			autoCloseTimer.reset()

			// Pick Randomized
			if (takeRandomizedValue.get())
			{
				do
				{
					val items = (0 until end).map(container::getSlot).filter { shouldTake(thePlayer, it.stack, it.slotNumber, inventoryCleaner, end, container, itemDelay) }.toList()

					val randomSlot = Random.nextInt(items.size)
					var slot = items[randomSlot]

					var misclick = false

					// Simulate Click Mistakes to bypass some anti-cheats
					if (misclickEnabledValue.get() && remainingMisclickCount > 0 && misclickRateValue.get() > 0 && Random.nextInt(100) <= misclickRateValue.get())
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

				if (delayTimer.hasTimePassed(nextDelay) && shouldTake(thePlayer, stack, slot.slotNumber, inventoryCleaner, end, container, itemDelay))
				{
					var misclick = false

					if (misclickEnabledValue.get() && remainingMisclickCount > 0 && misclickRateValue.get() > 0 && Random.nextInt(100) <= misclickRateValue.get())
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
		else if (autoCloseEnabledValue.get() && autoCloseTimer.hasTimePassed(nextCloseDelay))
		{
			thePlayer.closeScreen()
			nextCloseDelay = autoCloseDelayValue.getRandomDelay()
		}
	}

	@EventTarget
	private fun onPacket(event: PacketEvent)
	{
		val packet = event.packet

		if (classProvider.isSPacketWindowItems(packet)) contentReceived = packet.asSPacketWindowItems().windowId
	}

	private fun shouldTake(thePlayer: IEntityPlayer, stack: IItemStack?, slot: Int, inventoryCleaner: InventoryCleaner, end: Int, container: IContainer, itemDelay: Long): Boolean
	{
		val currentTime = System.currentTimeMillis()

		return stack != null && (!onlyItemsValue.get() || !classProvider.isItemBlock(stack.item)) && (currentTime - stack.itemDelay >= itemDelay && (!inventoryCleaner.state || inventoryCleaner.isUseful(thePlayer, slot, stack, end = end, container = container) && inventoryCleaner.isUseful(thePlayer, -1, stack, container = thePlayer.inventoryContainer) /* 상자 안에서 가장 좋은 템이랑 인벤 안의 가장 좋은 템이랑 비교한 후, 상자 안의 것이 더 좋을 경우에만 가져가기 */))
	}

	private fun move(screen: IGuiChest, slot: ISlot, misclick: Boolean)
	{
		screen.handleMouseClick(slot, slot.slotNumber, 0, 1)
		if (clickIndicationEnabledValue.get()) screen.asGuiContainer().highlight(slot.slotNumber, clickIndicationLengthValue.get().toLong(), if (misclick) -2130771968 /* 0x80FF0000 */ else -2147418368 /* 0x8000FF00 */)
		delayTimer.reset()
		nextDelay = delayValue.getRandomDelay()
	}

	private fun isEmpty(thePlayer: IEntityPlayer, chest: IGuiChest, itemDelay: Long): Boolean
	{
		val inventoryCleaner = LiquidBounce.moduleManager[InventoryCleaner::class.java] as InventoryCleaner
		val container = chest.inventorySlots ?: return false
		val end = chest.inventoryRows * 9

		return (0 until end).map(container::getSlot).none { shouldTake(thePlayer, it.stack, it.slotNumber, inventoryCleaner, end, container, itemDelay) }
	}

	private fun firstEmpty(slots: List<ISlot>?, length: Int, random: Boolean): ISlot?
	{
		slots ?: return null
		val emptySlots = (0 until length).map { slots[it] }.filter { it.stack == null }.toMutableList()

		if (emptySlots.isEmpty()) return null

		return if (random) emptySlots[Random.nextInt(emptySlots.size)] else emptySlots.first()
	}

	private fun getFullInventory(thePlayer: IEntityPlayer): Boolean = thePlayer.inventory.mainInventory.none(ItemUtils::isStackEmpty)

	override val tag: String
		get() = "${delayValue.getMin()} ~ ${delayValue.getMax()}"
}
