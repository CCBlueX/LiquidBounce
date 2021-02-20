/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.enums.ItemType
import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.*
import kotlin.random.Random

@ModuleInfo(name = "AutoSoup", description = "Makes you automatically eat soup whenever your health is low.", category = ModuleCategory.COMBAT)
class AutoSoup : Module()
{
	private val healthValue = FloatValue("Health", 15f, 0f, 20f)

	private val maxDelayValue: IntegerValue = object : IntegerValue("MaxSoupDelay", 100, 0, 5000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minDelayValue.get()
			if (i > newValue) this.set(i)
		}
	}

	private val minDelayValue: IntegerValue = object : IntegerValue("MinSoupDelay", 100, 0, 5000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxDelayValue.get()
			if (i < newValue) this.set(i)
		}
	}

	private val maxInvDelayValue: IntegerValue = object : IntegerValue("MaxInvDelay", 200, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minInvDelayValue.get()
			if (i > newValue) this.set(i)
		}
	}

	private val minInvDelayValue: IntegerValue = object : IntegerValue("MinInvDelay", 100, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxInvDelayValue.get()
			if (i < newValue) this.set(i)
		}
	}
	private val openInventoryValue = BoolValue("OpenInv", false)
	private val simulateInventoryValue = BoolValue("SimulateInventory", true)
	private val noMoveValue = BoolValue("NoMove", false)
	private val randomSlotValue = BoolValue("RandomSlot", false)
	private val misClickValue = BoolValue("ClickMistakes", false)
	private val misClickRateValue = IntegerValue("ClickMistakeRate", 5, 0, 100)
	private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)

	private val bowlValue = ListValue("Bowl", arrayOf("Drop", "Move", "Stay"), "Drop")

	private val ignoreScreen = BoolValue("IgnoreScreen", true)

	private val soupDelayTimer = MSTimer()
	private var soupDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

	private val invDelayTimer = MSTimer()
	private var invDelay = TimeUtils.randomDelay(minInvDelayValue.get(), maxInvDelayValue.get())

	override val tag: String
		get() = "${healthValue.get()}"

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val thePlayer = mc.thePlayer ?: return
		val netHandler = mc.netHandler
		val inventory = thePlayer.inventory
		val inventoryContainer = thePlayer.inventoryContainer

		val provider = classProvider

		val itemDelay = itemDelayValue.get().toLong()
		val random = randomSlotValue.get()
		val handleBowl = bowlValue.get()

		if (soupDelayTimer.hasTimePassed(soupDelay) && (ignoreScreen.get() || provider.isGuiContainer(mc.currentScreen)))
		{
			val soupInHotbar = InventoryUtils.findItem(inventoryContainer, 36, 45, provider.getItemEnum(ItemType.MUSHROOM_STEW), itemDelay, random)

			if (thePlayer.health <= healthValue.get() && soupInHotbar != -1)
			{
				netHandler.addToSendQueue(provider.createCPacketHeldItemChange(soupInHotbar - 36))
				netHandler.addToSendQueue(createUseItemPacket(inventory.getStackInSlot(soupInHotbar), WEnumHand.MAIN_HAND))

				if (handleBowl.equals("Drop", true)) netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.DROP_ITEM, WBlockPos.ORIGIN, provider.getEnumFacing(EnumFacingType.DOWN)))

				netHandler.addToSendQueue(provider.createCPacketHeldItemChange(inventory.currentItem))
				soupDelayTimer.reset()
				return
			}
		}

		if (invDelayTimer.hasTimePassed(invDelay) && !(noMoveValue.get() && MovementUtils.isMoving(thePlayer)) && !(thePlayer.openContainer != null && thePlayer.openContainer!!.windowId != 0))
		{
			// Move empty bowls to inventory
			val bowlInHotbar = InventoryUtils.findItem(inventoryContainer, 36, 45, provider.getItemEnum(ItemType.BOWL), itemDelay, random)

			val isGuiInventory = provider.isGuiInventory(mc.currentScreen)
			val simulateInv = simulateInventoryValue.get()

			if (handleBowl.equals("Move", true) && bowlInHotbar != -1)
			{
				if (openInventoryValue.get() && !isGuiInventory) return

				var bowlMovable = false

				@Suppress("LoopToCallChain") for (i in 9..36)
				{
					val itemStack = inventory.getStackInSlot(i)

					if (itemStack == null)
					{
						bowlMovable = true
						break
					}
					else if (itemStack.item == provider.getItemEnum(ItemType.BOWL) && itemStack.stackSize < 64)
					{
						bowlMovable = true
						break
					}
				}

				if (bowlMovable)
				{
					val openInventory = !isGuiInventory && simulateInv

					if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

					mc.playerController.windowClick(0, bowlInHotbar, 0, 1, thePlayer)

					invDelay = TimeUtils.randomDelay(minInvDelayValue.get(), maxInvDelayValue.get())
					invDelayTimer.reset()
					return
				}
			}

			// Move soups to hotbar
			var soupInInventory = InventoryUtils.findItem(inventoryContainer, 9, 36, provider.getItemEnum(ItemType.MUSHROOM_STEW), itemDelay, random)

			if (soupInInventory != -1 && InventoryUtils.hasSpaceHotbar(inventory))
			{

				// OpenInventory Check
				if (openInventoryValue.get() && !isGuiInventory) return

				// Simulate Click Mistakes to bypass some anti-cheats
				if (misClickValue.get() && misClickRateValue.get() > 0 && Random.nextInt(100) <= misClickRateValue.get())
				{
					val firstEmpty = InventoryUtils.firstEmpty(inventoryContainer, 9, 36, random)
					if (firstEmpty != -1) soupInInventory = firstEmpty
				}

				val openInventory = !isGuiInventory && simulateInv
				if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

				mc.playerController.windowClick(0, soupInInventory, 0, 1, thePlayer)

				if (openInventory) netHandler.addToSendQueue(provider.createCPacketCloseWindow())

				invDelay = TimeUtils.randomDelay(minInvDelayValue.get(), maxInvDelayValue.get())
				invDelayTimer.reset()
			}
		}
	}
}
