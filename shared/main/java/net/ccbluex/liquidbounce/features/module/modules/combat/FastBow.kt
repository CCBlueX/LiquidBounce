/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.WorkerUtils
import net.ccbluex.liquidbounce.value.IntegerValue

@ModuleInfo(name = "FastBow", description = "Turns your bow into a machine gun.", category = ModuleCategory.COMBAT)
class FastBow : Module()
{
	val packetsValue = IntegerValue("Packets", 20, 3, 20)

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		val netHandler = mc.netHandler

		if (!thePlayer.isUsingItem) return

		val currentItem = thePlayer.inventory.getCurrentItemInHand()

		val provider = classProvider

		if (currentItem != null && provider.isItemBow(currentItem.item))
		{
			netHandler.addToSendQueue(provider.createCPacketPlayerBlockPlacement(WBlockPos.ORIGIN, 255, currentItem, 0F, 0F, 0F))

			val yaw = RotationUtils.targetRotation?.yaw ?: thePlayer.rotationYaw
			val pitch = RotationUtils.targetRotation?.pitch ?: thePlayer.rotationPitch

			WorkerUtils.workers.execute {
				repeat(packetsValue.get()) { netHandler.addToSendQueue(provider.createCPacketPlayerLook(yaw, pitch, true)) }
				netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM, WBlockPos.ORIGIN, provider.getEnumFacing(EnumFacingType.DOWN)))
				thePlayer.itemInUseCount = currentItem.maxItemUseDuration - 1
			}
		}
	}

	override val tag: String
		get() = "${packetsValue.get()}"
}
