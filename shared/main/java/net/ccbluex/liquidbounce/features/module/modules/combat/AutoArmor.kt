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
import net.ccbluex.liquidbounce.utils.timer.Cooldown
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue

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

	private val infoUpdateCooldown = Cooldown.getNewCooldownMiliseconds(100)

	private var cachedInfo: String? = null

	val advancedInformations: String
		get() = if (cachedInfo == null || infoUpdateCooldown.attemptReset()) (if (!state) "AutoArmor is not active"
		else
		{
			val minDelay = minDelayValue.get()
			val maxDelay = maxDelayValue.get()
			val noMove = noMoveValue.get()
			val hotbar = hotbarValue.get()
			val itemDelay = itemDelayValue.get()

			"AutoArmor active [delay: ($minDelay ~ $maxDelay), itemdelay: $itemDelay, nomove: $noMove, hotbar: $hotbar]"
		}).apply { cachedInfo = this }
		else cachedInfo!!

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent?)
	{
		val thePlayer = mc.thePlayer ?: return
		val netHandler = mc.netHandler
		val inventory = thePlayer.inventory

		if (!InventoryUtils.CLICK_TIMER.hasTimePassed(nextDelay) || thePlayer.openContainer != null && thePlayer.openContainer!!.windowId != 0) return

		val provider = classProvider

		val itemDelay = itemDelayValue.get()

		// Find best armor
		val armorPieces = (0 until 36).mapNotNull { it to (inventory.getStackInSlot(it) ?: return@mapNotNull null) }.filter { (slot, itemStack) -> provider.isItemArmor(itemStack.item) && (slot < 9 || System.currentTimeMillis() - itemStack.itemDelay >= itemDelay) }.map { (slot, itemStack) -> ArmorPiece(itemStack, slot) }.groupBy(ArmorPiece::armorType)

		val bestArmor = arrayOfNulls<ArmorPiece>(4)
		for ((armorType, candidates) in armorPieces) bestArmor[armorType] = candidates.maxWith(ARMOR_COMPARATOR)

		// Swap armor
		if ((0..3).any { i ->
				val armorPiece = bestArmor[i] ?: return@any false
				val armorSlot = 3 - i
				val oldArmor = ArmorPiece(inventory.armorItemInSlot(armorSlot), -1)

				((ItemUtils.isStackEmpty(oldArmor.itemStack) || !provider.isItemArmor(oldArmor.itemStack?.item) || ARMOR_COMPARATOR.compare(oldArmor, armorPiece) < 0) && ((!ItemUtils.isStackEmpty(oldArmor.itemStack) && move(thePlayer, netHandler, 8 - armorSlot, true)) || ItemUtils.isStackEmpty(inventory.armorItemInSlot(armorSlot)) && move(thePlayer, netHandler, armorPiece.slot, false)))
			})
		{
			locked = true
			return
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
		val screen = mc.currentScreen
		val controller = mc.playerController

		val provider = classProvider

		if (!isArmorSlot && item < 9 && hotbarValue.get() && !provider.isGuiInventory(screen))
		{
			netHandler.addToSendQueue(provider.createCPacketHeldItemChange(item))
			netHandler.addToSendQueue(createUseItemPacket(thePlayer.inventoryContainer.getSlot(item).stack, WEnumHand.MAIN_HAND))
			netHandler.addToSendQueue(provider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))

			nextDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

			return true
		}

		if (!(noMoveValue.get() && isMoving(thePlayer)) && (!invOpenValue.get() || provider.isGuiInventory(screen)) && item != -1)
		{
			val openInventory = simulateInventory.get() && !provider.isGuiInventory(screen)

			if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

			var full = isArmorSlot

			if (full) full = thePlayer.inventory.mainInventory.none(ItemUtils::isStackEmpty)

			if (full) controller.windowClick(thePlayer.inventoryContainer.windowId, item, 1, 4, thePlayer) else controller.windowClick(thePlayer.inventoryContainer.windowId, if (isArmorSlot) item else if (item < 9) item + 36 else item, 0, 1, thePlayer)

			nextDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

			if (openInventory) netHandler.addToSendQueue(provider.createCPacketCloseWindow())

			return true
		}
		return false
	}

	override val tag: String
		get() = "${minDelayValue.get()} ~ ${maxDelayValue.get()}"

	companion object
	{
		val ARMOR_COMPARATOR: Comparator<ArmorPiece> = ArmorComparator()
	}
}
