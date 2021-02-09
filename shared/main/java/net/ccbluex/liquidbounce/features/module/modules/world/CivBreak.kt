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
import java.awt.Color

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
		if (classProvider.isBlockBedrock(event.clickedBlock?.let(BlockUtils::getBlock))) return

		val netHandler = mc.netHandler

		blockPos = event.clickedBlock ?: return
		enumFacing = event.WEnumFacing ?: return

		// Break
		netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.START_DESTROY_BLOCK, blockPos!!, enumFacing!!))
		netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.STOP_DESTROY_BLOCK, blockPos!!, enumFacing!!))
	}

	@EventTarget
	fun onUpdate(event: MotionEvent)
	{
		val pos = blockPos ?: return

		if (airResetValue.get() && classProvider.isBlockAir(BlockUtils.getBlock(pos)) || rangeResetValue.get() && BlockUtils.getCenterDistance(pos) > range.get())
		{
			blockPos = null
			return
		}

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return
		val netHandler = mc.netHandler

		if (classProvider.isBlockAir(BlockUtils.getBlock(pos)) || BlockUtils.getCenterDistance(pos) > range.get()) return

		when (event.eventState)
		{
			EventState.PRE -> if (rotationsValue.get()) RotationUtils.setTargetRotation((RotationUtils.faceBlock(theWorld, thePlayer, pos) ?: return).rotation)

			EventState.POST ->
			{
				val facing = enumFacing!!

				if (visualSwingValue.get()) thePlayer.swingItem()
				else netHandler.addToSendQueue(classProvider.createCPacketAnimation())

				// Break
				netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.START_DESTROY_BLOCK, pos, facing))
				netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.STOP_DESTROY_BLOCK, pos, facing))
				mc.playerController.clickBlock(pos, facing)
			}
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		RenderUtils.drawBlockBox(blockPos ?: return, Color.RED, true)
	}

	override val tag: String
		get() = "${range.get()}"
}
