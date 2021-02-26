/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.SlowDownEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.TpAura
import net.ccbluex.liquidbounce.injection.backend.Backend
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue

@ModuleInfo(name = "NoSlow", description = "Cancels slowness effects caused by soulsand and using items.", category = ModuleCategory.MOVEMENT)
class NoSlow : Module()
{
	// Highly customizable values
	private val blockForwardMultiplier = FloatValue("BlockForwardMultiplier", 1.0F, 0.2F, 1.0F)
	private val blockStrafeMultiplier = FloatValue("BlockStrafeMultiplier", 1.0F, 0.2F, 1.0F)

	private val consumeForwardMultiplier = FloatValue("ConsumeForwardMultiplier", 1.0F, 0.2F, 1.0F)
	private val consumeStrafeMultiplier = FloatValue("ConsumeStrafeMultiplier", 1.0F, 0.2F, 1.0F)

	private val bowForwardMultiplier = FloatValue("BowForwardMultiplier", 1.0F, 0.2F, 1.0F)
	private val bowStrafeMultiplier = FloatValue("BowStrafeMultiplier", 1.0F, 0.2F, 1.0F)

	// Bypass for NCP
	private val packetValue = BoolValue("Packet", true)
	private val packetSpamDelayValue = IntegerValue("Packet-PacketsDelay", 0, 0, 3)

	// Blocks
	val soulsandValue = BoolValue("Soulsand", true)
	val liquidPushValue = BoolValue("LiquidPush", true)

	private var ncpDelay = TickTimer()

	@EventTarget
	fun onMotion(event: MotionEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		val heldItem = thePlayer.heldItem ?: return

		val provider = classProvider

		if (!provider.isItemSword(heldItem.item) || !MovementUtils.isMoving(thePlayer)) return

		val moduleManager = LiquidBounce.moduleManager

		val aura = moduleManager[KillAura::class.java] as KillAura
		val tpaura = moduleManager[TpAura::class.java] as TpAura

		if (!thePlayer.isBlocking && !aura.serverSideBlockingStatus && !tpaura.serverSideBlockingStatus) return

		if (packetValue.get() && Backend.MINECRAFT_VERSION_MINOR == 8 && ncpDelay.hasTimePassed(packetSpamDelayValue.get()))
		{
			val netHandler = mc.netHandler

			when (event.eventState)
			{
				EventState.PRE -> netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM, WBlockPos(0, 0, 0), provider.getEnumFacing(EnumFacingType.DOWN)))
				EventState.POST -> netHandler.addToSendQueue(provider.createCPacketPlayerBlockPlacement(WBlockPos(-1, -1, -1), 255, thePlayer.inventory.getCurrentItemInHand(), 0.0F, 0.0F, 0.0F))
			}

			ncpDelay.reset()
		}
		ncpDelay.update()
	}

	@EventTarget
	fun onSlowDown(event: SlowDownEvent)
	{
		val heldItem = (mc.thePlayer ?: return).heldItem?.item

		event.forward = getMultiplier(heldItem, isForward = true)
		event.strafe = getMultiplier(heldItem, isForward = false)
	}

	private fun getMultiplier(item: IItem?, isForward: Boolean): Float
	{
		val provider = classProvider

		return when
		{
			provider.isItemFood(item) || provider.isItemPotion(item) || provider.isItemBucketMilk(item) -> if (isForward) consumeForwardMultiplier.get() else consumeStrafeMultiplier.get()
			provider.isItemSword(item) -> if (isForward) blockForwardMultiplier.get() else blockStrafeMultiplier.get()
			provider.isItemBow(item) -> if (isForward) bowForwardMultiplier.get() else bowStrafeMultiplier.get()
			else -> 0.2F
		}
	}

	override val tag: String?
		get() = if (packetValue.get()) "Packet" else null
}
