/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.util.IEnumFacing
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue

@ModuleInfo(name = "CivBreak", description = "Allows you to break blocks instantly.", category = ModuleCategory.WORLD)
class CivBreak : Module()
{
	private val range = FloatValue("Range", 5F, 1F, 6F)

	private val rotationsValue = BoolValue("Rotations", true)
	private val visualSwingValue = BoolValue("VisualSwing", true)
	private val airResetValue = BoolValue("Air-Reset", true)

	private val rangeResetValue = BoolValue("Range-Reset", true)

	var blockPos: WBlockPos? = null
	private var enumFacing: IEnumFacing? = null

	@EventTarget
	fun onBlockClick(event: ClickBlockEvent)
	{
		val theWorld = mc.theWorld ?: return

		val provider = classProvider

		if (provider.isBlockBedrock(event.clickedBlock?.let { BlockUtils.getBlock(theWorld, it) })) return

		val netHandler = mc.netHandler

		val enumFacing = (event.WEnumFacing ?: return).also { enumFacing = it }
		val blockPos = (event.clickedBlock ?: return).also { blockPos = it }

		// Break
		netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.START_DESTROY_BLOCK, blockPos, enumFacing))
		netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.STOP_DESTROY_BLOCK, blockPos, enumFacing))
	}

	@EventTarget
	fun onUpdate(event: MotionEvent)
	{
		val pos = blockPos ?: return

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val provider = classProvider

		if (airResetValue.get() && provider.isBlockAir(BlockUtils.getBlock(theWorld, pos)) || rangeResetValue.get() && BlockUtils.getCenterDistance(thePlayer, pos) > range.get())
		{
			blockPos = null
			return
		}

		val netHandler = mc.netHandler

		if (provider.isBlockAir(BlockUtils.getBlock(theWorld, pos)) || BlockUtils.getCenterDistance(thePlayer, pos) > range.get()) return

		when (event.eventState)
		{
			EventState.PRE -> if (rotationsValue.get()) RotationUtils.setTargetRotation((RotationUtils.faceBlock(theWorld, thePlayer, pos) ?: return).rotation)

			EventState.POST ->
			{
				val facing = enumFacing ?: return

				if (visualSwingValue.get()) thePlayer.swingItem()
				else netHandler.addToSendQueue(provider.createCPacketAnimation())

				// Break
				netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.START_DESTROY_BLOCK, pos, facing))
				netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.STOP_DESTROY_BLOCK, pos, facing))
				mc.playerController.clickBlock(pos, facing)
			}
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		RenderUtils.drawBlockBox(mc.theWorld ?: return, mc.thePlayer ?: return, blockPos ?: return, -65536, 0, hydraESP = false)
	}

	override val tag: String
		get() = "${range.get()}"
}
