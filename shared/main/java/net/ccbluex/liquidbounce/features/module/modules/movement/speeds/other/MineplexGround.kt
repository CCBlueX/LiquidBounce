/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MovementUtils

class MineplexGround : SpeedMode("Mineplex-Ground")
{
	private var spoofSlot = false
	private var moveSpeed = 0f

	override fun onMotion(eventState: EventState)
	{
		if (eventState != EventState.PRE) return

		val thePlayer = mc.thePlayer ?: return

		val inventory = thePlayer.inventory
		if (!MovementUtils.isMoving(thePlayer) || !thePlayer.onGround || inventory.getCurrentItemInHand() == null || thePlayer.isUsingItem) return

		spoofSlot = false

		(36..44).firstOrNull { inventory.getStackInSlot(it) == null }?.let {
			mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(it - 36))
			spoofSlot = true
		}
	}

	override fun onUpdate()
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (!MovementUtils.isMoving(thePlayer) || !thePlayer.onGround || thePlayer.isUsingItem)
		{
			moveSpeed = 0f
			return
		}

		if (!spoofSlot && thePlayer.inventory.getCurrentItemInHand() != null)
		{
			ClientUtils.displayChatMessage(thePlayer, "\u00A78[\u00A7c\u00A7lMineplex\u00A7aSpeed\u00A78] \u00A7cYou need one empty slot.")
			return
		}

		val blockPos = WBlockPos(thePlayer.posX, thePlayer.entityBoundingBox.minY - 1, thePlayer.posZ)
		val vec = WVec3(blockPos).addVector(0.4, 0.4, 0.4).add(WVec3(classProvider.getEnumFacing(EnumFacingType.UP).directionVec))

		mc.playerController.onPlayerRightClick(thePlayer, theWorld, null, blockPos, classProvider.getEnumFacing(EnumFacingType.UP), WVec3(vec.xCoord * 0.4f, vec.yCoord * 0.4f, vec.zCoord * 0.4f))

		val targetSpeed = (LiquidBounce.moduleManager[Speed::class.java] as Speed).mineplexGroundSpeedValue.get()

		if (targetSpeed > moveSpeed) moveSpeed += targetSpeed * 0.125f
		if (moveSpeed >= targetSpeed) moveSpeed = targetSpeed

		MovementUtils.strafe(thePlayer, moveSpeed)

		if (!spoofSlot) mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))
	}

	override fun onMove(event: MoveEvent)
	{
	}

	override fun onDisable()
	{
		moveSpeed = 0f
		mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange((mc.thePlayer ?: return).inventory.currentItem))
	}
}
