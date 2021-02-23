/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.api.enums.ItemType
import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo

import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.createUseItemPacket
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue

@ModuleInfo(name = "KeepAlive", description = "Tries to prevent you from dying.", category = ModuleCategory.PLAYER)
class KeepAlive : Module()
{

	val modeValue = ListValue("Mode", arrayOf("/heal", "Soup"), "/heal")
	private val randomSlotValue = BoolValue("RandomSlot", false)
	private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 1000)

	private var runOnce = false

	@EventTarget
	fun onMotion(@Suppress("UNUSED_PARAMETER") event: MotionEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		val netHandler = mc.netHandler

		if (thePlayer.isDead || thePlayer.health <= 0)
		{
			if (runOnce) return

			when (modeValue.get().toLowerCase())
			{
				"/heal" -> thePlayer.sendChatMessage("/heal")

				"soup" ->
				{
					val provider = classProvider

					val soupInHotbar = InventoryUtils.findItem(thePlayer.inventoryContainer, 36, 45, provider.getItemEnum(ItemType.MUSHROOM_STEW), itemDelayValue.get().toLong(), randomSlotValue.get())

					if (soupInHotbar != -1)
					{

						netHandler.addToSendQueue(provider.createCPacketHeldItemChange(soupInHotbar - 36))
						netHandler.addToSendQueue(createUseItemPacket(thePlayer.inventory.getStackInSlot(soupInHotbar), WEnumHand.MAIN_HAND))
						netHandler.addToSendQueue(provider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))
					}
				}
			}

			runOnce = true
		}
		else runOnce = false
	}
}
