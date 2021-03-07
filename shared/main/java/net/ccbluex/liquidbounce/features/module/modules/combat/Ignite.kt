/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.enums.ItemType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.wrapAngleTo180_float
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import java.util.*
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

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return
		val netHandler = mc.netHandler
		val controller = mc.playerController

		val inventoryContainer = thePlayer.inventoryContainer

		val provider = classProvider

		val itemDelay = itemDelayValue.get()
		val randomSlot = randomSlotValue.get()

		val lighterInHotbar = if (lighterValue.get()) InventoryUtils.findItem(inventoryContainer, 36, 45, provider.getItemEnum(ItemType.FLINT_AND_STEEL), itemDelay.toLong(), randomSlot) else -1
		val lavaInHotbar = if (lavaBucketValue.get()) InventoryUtils.findItem(inventoryContainer, 26, 45, provider.getItemEnum(ItemType.LAVA_BUCKET), itemDelay.toLong(), randomSlot) else -1

		if (lighterInHotbar == -1 && lavaInHotbar == -1) return

		val fireInHotbar = if (lighterInHotbar == -1) lavaInHotbar else lighterInHotbar

		EntityUtils.getEntitiesInRadius(theWorld, thePlayer, 8.0).filterNot(IEntity::burning).filter { isSelected(it, true) }.map(IEntity::position).filter { thePlayer.getDistanceSq(it) < 22.3 }.filter(::isReplaceable).firstOrNull { provider.isBlockAir(getBlock(theWorld, it)) }?.let { blockPos ->
			RotationUtils.keepCurrentRotation = true
			netHandler.addToSendQueue(provider.createCPacketHeldItemChange(fireInHotbar - 36))

			val itemStack = thePlayer.inventory.getStackInSlot(fireInHotbar)

			if (itemStack != null && provider.isItemBucket(itemStack.item))
			{
				val diffX = blockPos.x + 0.5 - thePlayer.posX
				val diffY = blockPos.y + 0.5 - (thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight)
				val diffZ = blockPos.z + 0.5 - thePlayer.posZ
				val sqrt = hypot(diffX, diffZ)

				val yaw = WMathHelper.toDegrees(StrictMath.atan2(diffZ, diffX).toFloat()) - 90.0f
				val pitch = -WMathHelper.toDegrees(StrictMath.atan2(diffY, sqrt).toFloat())

				netHandler.addToSendQueue(provider.createCPacketPlayerLook(thePlayer.rotationYaw + wrapAngleTo180_float(yaw - thePlayer.rotationYaw), thePlayer.rotationPitch + wrapAngleTo180_float(pitch - thePlayer.rotationPitch), thePlayer.onGround))

				controller.sendUseItem(thePlayer, theWorld, itemStack)
			}
			else run {
				EnumFacingType.values().map(provider::getEnumFacing).forEach { side ->
					val neighbor = blockPos.offset(side)

					if (!canBeClicked(theWorld, neighbor)) return@forEach

					val diffX = neighbor.x + 0.5 - thePlayer.posX
					val diffY = neighbor.y + 0.5 - (thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight)
					val diffZ = neighbor.z + 0.5 - thePlayer.posZ
					val sqrt = hypot(diffX, diffZ)

					val yaw = WMathHelper.toDegrees(StrictMath.atan2(diffZ, diffX).toFloat()) - 90.0f
					val pitch = -WMathHelper.toDegrees(StrictMath.atan2(diffY, sqrt).toFloat())

					netHandler.addToSendQueue(provider.createCPacketPlayerLook(thePlayer.rotationYaw + wrapAngleTo180_float(yaw - thePlayer.rotationYaw), thePlayer.rotationPitch + wrapAngleTo180_float(pitch - thePlayer.rotationPitch), thePlayer.onGround))

					if (controller.onPlayerRightClick(thePlayer, theWorld, itemStack, neighbor, side.opposite, WVec3(side.directionVec)))
					{
						thePlayer.swingItem()
						return@run
					}
				}
			}

			netHandler.addToSendQueue(provider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))
			RotationUtils.keepCurrentRotation = false
			netHandler.addToSendQueue(provider.createCPacketPlayerLook(thePlayer.rotationYaw, thePlayer.rotationPitch, thePlayer.onGround))
			msTimer.reset()
		}
	}

	override val tag: String
		get()
		{
			val tagBuilder = StringJoiner(" and ")
			if (lighterValue.get()) tagBuilder.add("Lighter")
			if (lavaBucketValue.get()) tagBuilder.add("Lava")

			return "$tagBuilder"
		}
}
