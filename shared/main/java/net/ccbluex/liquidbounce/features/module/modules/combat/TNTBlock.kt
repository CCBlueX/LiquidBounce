/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue

@ModuleInfo(name = "TNTBlock", description = "Automatically blocks with your sword when TNT around you explodes.", category = ModuleCategory.COMBAT)
class TNTBlock : Module()
{
	private val fuseValue = IntegerValue("Fuse", 10, 0, 80)
	private val rangeValue = FloatValue("Range", 9F, 1F, 20F)
	private val autoSwordValue = BoolValue("AutoSword", true)

	private var blocked = false

	@EventTarget
	fun onMotionUpdate(@Suppress("UNUSED_PARAMETER") event: MotionEvent?)
	{
		val thePlayer = mc.thePlayer ?: return
		val theWorld = mc.theWorld ?: return

		val range = rangeValue.get()
		val fuse = fuseValue.get()

		if (theWorld.loadedEntityList.any { classProvider.isEntityTNTPrimed(it) && thePlayer.getDistanceToEntity(it) <= range && it.asEntityTNTPrimed().fuse <= fuse })
		{
			if (autoSwordValue.get())
			{
				val inventory = thePlayer.inventory
				val slot = (0..8).mapNotNull { it to (inventory.getStackInSlot(it) ?: return@mapNotNull null) }.filter { (_, itemStack) -> classProvider.isItemSword(itemStack.item) }.maxBy { (_, itemStack) -> itemStack.item!!.asItemSword().damageVsEntity + 4f }?.first ?: -1

				if (slot != -1 && slot != inventory.currentItem)
				{
					inventory.currentItem = slot
					mc.playerController.updateController()
				}
			}

			if (thePlayer.heldItem != null && classProvider.isItemSword(thePlayer.heldItem!!.item))
			{
				mc.gameSettings.keyBindUseItem.pressed = true
				blocked = true
			}

			return
		}

		if (blocked && !mc.gameSettings.isKeyDown(mc.gameSettings.keyBindUseItem))
		{
			mc.gameSettings.keyBindUseItem.pressed = false
			blocked = false
		}
	}

	override val tag: String
		get() = "${fuseValue.get()}"
}
