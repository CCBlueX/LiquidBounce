/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.network.IINetHandlerPlayClient
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.createOpenInventoryPacket
import net.ccbluex.liquidbounce.utils.createUseItemPacket
import net.ccbluex.liquidbounce.utils.item.ArmorComparator
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream

@ModuleInfo(name = "AutoArmor", description = "Automatically equips the best armor in your inventory.", category = ModuleCategory.COMBAT)
class AutoArmor : Module()
{
	/**
	 * Options
	 */
	val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 100, 0, 400)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val maxDelay = maxDelayValue.get()
			if (maxDelay < newValue) set(maxDelay)
		}
	}
	val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 200, 0, 400)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val minDelay = minDelayValue.get()
			if (minDelay > newValue) set(minDelay)
		}
	}
	private val invOpenValue = BoolValue("InvOpen", false)
	private val simulateInventory = BoolValue("SimulateInventory", true)
	private val noMoveValue = BoolValue("NoMove", false)
	private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)
	private val hotbarValue = BoolValue("Hotbar", true)

	/**
	 * Variables
	 */
	private var nextDelay: Long = 0
	private var locked = false

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent?)
	{
		val thePlayer = mc.thePlayer ?: return
		val netHandler = mc.netHandler

		if (!InventoryUtils.CLICK_TIMER.hasTimePassed(nextDelay) || thePlayer.openContainer != null && thePlayer.openContainer!!.windowId != 0) return

		// Find best armor
		val armorPieces = IntStream.range(0, 36).filter { i: Int ->
			val itemStack = thePlayer.inventory.getStackInSlot(i)
			itemStack != null && classProvider.isItemArmor(itemStack.item) && (i < 9 || System.currentTimeMillis() - itemStack.itemDelay >= itemDelayValue.get())
		}.mapToObj { ArmorPiece(thePlayer.inventory.getStackInSlot(it), it) }.collect(Collectors.groupingBy { armor: ArmorPiece -> armor.armorType })

		val bestArmor = arrayOfNulls<ArmorPiece>(4)

		for ((key, value) in armorPieces) bestArmor[key] = value.stream().max(ARMOR_COMPARATOR).orElse(null)

		// Swap armor
		for (i in 0..3)
		{
			val armorPiece = bestArmor[i] ?: continue
			val armorSlot = 3 - i
			val oldArmor = ArmorPiece(thePlayer.inventory.armorItemInSlot(armorSlot), -1)

			if (ItemUtils.isStackEmpty(oldArmor.itemStack) || !classProvider.isItemArmor(oldArmor.itemStack.item) || ARMOR_COMPARATOR.compare(oldArmor, armorPiece) < 0)
			{
				if (!ItemUtils.isStackEmpty(oldArmor.itemStack) && move(thePlayer, netHandler, 8 - (3 - armorSlot), true))
				{
					locked = true
					return
				}

				if (ItemUtils.isStackEmpty(thePlayer.inventory.armorItemInSlot(armorSlot)) && move(thePlayer, netHandler, armorPiece.slot, false))
				{
					locked = true
					return
				}
			}
		}

		locked = false
	}

	val isLocked: Boolean
		get() = !state || locked

	/**
	 * Shift+Left clicks the specified item
	 *
	 * @param  item
	 * Slot of the item to click
	 * @param  isArmorSlot
	 * @return             True if it is unable to move the item
	 */
	private fun move(thePlayer: IEntityPlayerSP, netHandler: IINetHandlerPlayClient, item: Int, isArmorSlot: Boolean): Boolean
	{
		if (!isArmorSlot && item < 9 && hotbarValue.get() && !classProvider.isGuiInventory(mc.currentScreen))
		{
			netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(item))
			netHandler.addToSendQueue(createUseItemPacket(thePlayer.inventoryContainer.getSlot(item).stack, WEnumHand.MAIN_HAND))
			netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))

			nextDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

			return true
		}

		if (!(noMoveValue.get() && isMoving(thePlayer)) && (!invOpenValue.get() || classProvider.isGuiInventory(mc.currentScreen)) && item != -1)
		{
			val openInventory = simulateInventory.get() && !classProvider.isGuiInventory(mc.currentScreen)

			if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

			var full = isArmorSlot

			if (full) for (iItemStack in thePlayer.inventory.mainInventory) if (ItemUtils.isStackEmpty(iItemStack))
			{
				full = false
				break
			}

			if (full) mc.playerController.windowClick(thePlayer.inventoryContainer.windowId, item, 1, 4, thePlayer) else mc.playerController.windowClick(thePlayer.inventoryContainer.windowId, if (isArmorSlot) item else if (item < 9) item + 36 else item, 0, 1, thePlayer)

			nextDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

			if (openInventory) netHandler.addToSendQueue(classProvider.createCPacketCloseWindow())

			return true
		}
		return false
	}

	override val tag: String
		get() = "${minDelayValue.get()} ~ ${maxDelayValue.get()}"

	companion object
	{
		val ARMOR_COMPARATOR: Comparator<ArmorPiece?> = ArmorComparator()
	}
}
