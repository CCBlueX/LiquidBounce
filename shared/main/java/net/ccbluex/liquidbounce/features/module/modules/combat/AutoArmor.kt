/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.network.IINetHandlerPlayClient
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.createOpenInventoryPacket
import net.ccbluex.liquidbounce.utils.createUseItemPacket
import net.ccbluex.liquidbounce.utils.extensions.isEmpty
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.item.ArmorComparator
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.ccbluex.liquidbounce.utils.timer.Cooldown
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerRangeValue
import net.ccbluex.liquidbounce.value.IntegerValue

@ModuleInfo(name = "AutoArmor", description = "Automatically equips the best armor in your inventory.", category = ModuleCategory.COMBAT)
class AutoArmor : Module()
{
	/**
	 * Options
	 */
	private val delayValue = IntegerRangeValue("Delay", 100, 200, 0, 1000, "MaxDelay" to "MinDelay")
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
		get()
		{
			val cache = cachedInfo

			return if (cache == null || infoUpdateCooldown.attemptReset()) (if (!state) "AutoArmor is not active"
			else
			{
				val minDelay = delayValue.getMin()
				val maxDelay = delayValue.getMax()
				val noMove = noMoveValue.get()
				val hotbar = hotbarValue.get()
				val itemDelay = itemDelayValue.get()

				"AutoArmor active [delay: ($minDelay ~ $maxDelay), itemdelay: $itemDelay, nomove: $noMove, hotbar: $hotbar]"
			}).apply { cachedInfo = this }
			else cache
		}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		val netHandler = mc.netHandler
		val inventory = thePlayer.inventory
		val openContainer = thePlayer.openContainer

		if (!InventoryUtils.CLICK_TIMER.hasTimePassed(nextDelay) || openContainer != null && openContainer.windowId != 0) return

		val provider = classProvider

		val itemDelay = itemDelayValue.get()

		val currentTime = System.currentTimeMillis()

		// Find best armor
		val armorPieces = (0 until 36).mapNotNull { it to (inventory.getStackInSlot(it) ?: return@mapNotNull null) }.filter { (slot, stack) -> provider.isItemArmor(stack.item) && (slot < 9 || currentTime - stack.itemDelay >= itemDelay) }.map { (slot, stack) -> ArmorPiece(stack, slot) }.groupBy(ArmorPiece::armorType)

		val bestArmor = arrayOfNulls<ArmorPiece>(4)
		for ((armorType, candidates) in armorPieces) bestArmor[armorType] = candidates.maxWith(ARMOR_COMPARATOR)

		// Swap armor
		if ((0..3).mapNotNull { i ->
				val armorSlot = 3 - i
				Triple(bestArmor[i] ?: return@mapNotNull null, armorSlot, ArmorPiece(inventory.armorItemInSlot(armorSlot), -1))
			}.filter { (armorPiece, _, oldArmor) ->
				val oldArmorStack = oldArmor.itemStack
				oldArmorStack.isEmpty || !provider.isItemArmor(oldArmorStack?.item) || ARMOR_COMPARATOR.compare(oldArmor, armorPiece) < 0
			}.any { (armorPiece, armorSlot, oldArmor) -> if (oldArmor.itemStack.isEmpty) move(thePlayer, netHandler, armorPiece.slot, false) else move(thePlayer, netHandler, 8 - armorSlot, true) })
		{
			locked = true
			return
		}

		locked = false
	}

	val isLocked: Boolean
		get() = state && locked // TODO: Check this workaround really works

	/**
	 * Shift+Left clicks the specified item
	 *
	 * @param  item
	 * Slot of the item to click
	 * @param  isArmorSlot
	 * @return             False if it is unable to move the item
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

			nextDelay = delayValue.getRandomDelay()

			return true
		}

		if (!(noMoveValue.get() && thePlayer.isMoving) && (!invOpenValue.get() || provider.isGuiInventory(screen)) && item != -1)
		{
			val openInventory = simulateInventory.get() && !provider.isGuiInventory(screen)

			if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

			var full = isArmorSlot

			if (full) full = thePlayer.inventory.mainInventory.none(IItemStack?::isEmpty)

			if (full) controller.windowClick(thePlayer.inventoryContainer.windowId, item, 1, 4, thePlayer) else controller.windowClick(thePlayer.inventoryContainer.windowId, if (isArmorSlot) item else if (item < 9) item + 36 else item, 0, 1, thePlayer)

			nextDelay = delayValue.getRandomDelay()

			if (openInventory) netHandler.addToSendQueue(provider.createCPacketCloseWindow())

			return true
		}
		return false
	}

	override val tag: String
		get() = "${delayValue.getMin()} ~ ${delayValue.getMax()}"

	companion object
	{
		val ARMOR_COMPARATOR: Comparator<ArmorPiece> = ArmorComparator()
	}
}
