/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.enums.ItemType
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.wrapAngleTo180_float
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import kotlin.math.hypot

@ModuleInfo(name = "Ignite", description = "Automatically sets targets around you on fire.", category = ModuleCategory.COMBAT)
class Ignite : Module()
{
	/**
	 * Options
	 */
	private val lighterValue = BoolValue("Lighter", true)
	private val lavaBucketValue = BoolValue("Lava", true)
	private val randomSlotValue = BoolValue("RandomSlot", false)
	private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 1000)

	/**
	 * Variables
	 */
	private val msTimer = MSTimer()

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		if (!msTimer.hasTimePassed(500L)) return

		val thePlayer = mc.thePlayer ?: return
		val theWorld = mc.theWorld ?: return

		val itemDelay = itemDelayValue.get()
		val randomSlot = randomSlotValue.get()
		val lighterInHotbar = if (lighterValue.get()) InventoryUtils.findItem(36, 45, classProvider.getItemEnum(ItemType.FLINT_AND_STEEL), itemDelay.toLong(), randomSlot) else -1
		val lavaInHotbar = if (lavaBucketValue.get()) InventoryUtils.findItem(26, 45, classProvider.getItemEnum(ItemType.LAVA_BUCKET), itemDelay.toLong(), randomSlot) else -1

		if (lighterInHotbar == -1 && lavaInHotbar == -1) return

		val fireInHotbar = if (lighterInHotbar == -1) lavaInHotbar else lighterInHotbar

		for (entity in theWorld.loadedEntityList) if (isSelected(entity, true) && !entity.burning)
		{
			val blockPos = entity.position

			if (thePlayer.getDistanceSq(blockPos) >= 22.3 || !isReplaceable(blockPos) || !classProvider.isBlockAir(getBlock(blockPos))) continue

			RotationUtils.keepCurrentRotation = true
			mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(fireInHotbar - 36))

			val itemStack = thePlayer.inventory.getStackInSlot(fireInHotbar)

			if (classProvider.isItemBucket(itemStack!!.item))
			{
				val diffX = blockPos.x + 0.5 - thePlayer.posX
				val diffY = blockPos.y + 0.5 - (thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight)
				val diffZ = blockPos.z + 0.5 - thePlayer.posZ
				val sqrt = hypot(diffX, diffZ)

				val yaw = (StrictMath.atan2(diffZ, diffX) * 180.0 / Math.PI).toFloat() - 90.0f
				val pitch = (-(StrictMath.atan2(diffY, sqrt) * 180.0 / Math.PI)).toFloat()

				mc.netHandler.addToSendQueue(
					classProvider.createCPacketPlayerLook(
						thePlayer.rotationYaw + wrapAngleTo180_float(yaw - thePlayer.rotationYaw), thePlayer.rotationPitch + wrapAngleTo180_float(pitch - thePlayer.rotationPitch), thePlayer.onGround
					)
				)

				mc.playerController.sendUseItem(thePlayer, theWorld, itemStack)
			}
			else for (enumFacingType in EnumFacingType.values())
			{
				val side = classProvider.getEnumFacing(enumFacingType)
				val neighbor = blockPos.offset(side)

				if (!canBeClicked(neighbor)) continue

				val diffX = neighbor.x + 0.5 - thePlayer.posX
				val diffY = neighbor.y + 0.5 - (thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight)
				val diffZ = neighbor.z + 0.5 - thePlayer.posZ
				val sqrt = hypot(diffX, diffZ)

				val yaw = (StrictMath.atan2(diffZ, diffX) * 180.0 / Math.PI).toFloat() - 90.0f
				val pitch = (-(StrictMath.atan2(diffY, sqrt) * 180.0 / Math.PI)).toFloat()

				mc.netHandler.addToSendQueue(
					classProvider.createCPacketPlayerLook(
						thePlayer.rotationYaw + wrapAngleTo180_float(yaw - thePlayer.rotationYaw), thePlayer.rotationPitch + wrapAngleTo180_float(pitch - thePlayer.rotationPitch), thePlayer.onGround
					)
				)
				if (mc.playerController.onPlayerRightClick(thePlayer, theWorld, itemStack, neighbor, side.opposite, WVec3(side.directionVec)))
				{
					thePlayer.swingItem()
					break
				}
			}

			mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))
			RotationUtils.keepCurrentRotation = false
			mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerLook(thePlayer.rotationYaw, thePlayer.rotationPitch, thePlayer.onGround))
			msTimer.reset()
			break
		}
	}

	override val tag: String
		get()
		{
			return if (lighterValue.get() && lavaBucketValue.get()) "Both"
			else when
			{
				lighterValue.get() -> "Lighter"
				lavaBucketValue.get() -> "Lava"
				else -> "Off"
			}
		}
}
