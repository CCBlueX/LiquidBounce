/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.extensions.getVec
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.*

@ModuleInfo(name = "ChestAura", description = "Automatically opens chests around you.", category = ModuleCategory.WORLD)
object ChestAura : Module()
{
	private val chestValue = BlockValue("Chest", functions.getIdFromBlock(classProvider.getBlockEnum(BlockType.CHEST)))
	private val rangeValue = FloatValue("Range", 5F, 1F, 6F)
	private val maxDelayValue = IntegerValue("MaxDelay", 100, 50, 200)
	private val minDelayValue = IntegerValue("MinDelay", 100, 50, 200)
	private val rotationsValue = BoolValue("Rotations", true)
	private val throughWallsValue = BoolValue("ThroughWalls", true)
	private val visualSwing = BoolValue("VisualSwing", true)

	var currentBlock: WBlockPos? = null

	private val timer = MSTimer()
	private var delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

	val clickedBlocks = mutableListOf<WBlockPos>()

	@EventTarget
	fun onMotion(event: MotionEvent)
	{
		if (LiquidBounce.moduleManager[Blink::class.java].state || (LiquidBounce.moduleManager[KillAura::class.java] as KillAura).hasTarget) return

		val thePlayer = mc.thePlayer ?: return
		val theWorld = mc.theWorld ?: return

		when (event.eventState)
		{
			EventState.PRE ->
			{
				if (classProvider.isGuiContainer(mc.currentScreen)) timer.reset() // No delay re-randomize code here because the performance impact is more than your think.

				val radius = rangeValue.get() + 1
				val eyesPos = WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)

				currentBlock = BlockUtils.searchBlocks(radius.toInt()).asSequence().filter {
					functions.getIdFromBlock(it.value) == chestValue.get() && !clickedBlocks.contains(it.key) && BlockUtils.getCenterDistance(it.key) < rangeValue.get()
				}.filter {
					if (throughWallsValue.get()) return@filter true

					val blockPos = it.key
					val movingObjectPosition = theWorld.rayTraceBlocks(eyesPos, blockPos.getVec(), stopOnLiquid = false, ignoreBlockWithoutBoundingBox = true, returnLastUncollidableBlock = false)

					movingObjectPosition != null && movingObjectPosition.blockPos == blockPos
				}.minBy { BlockUtils.getCenterDistance(it.key) }?.key

				if (rotationsValue.get()) RotationUtils.setTargetRotation((RotationUtils.faceBlock(currentBlock ?: return) ?: return).rotation)
			}

			EventState.POST -> if (currentBlock != null && timer.hasTimePassed(delay))
			{
				val currentBlock = currentBlock ?: return
				if (mc.playerController.onPlayerRightClick(thePlayer, theWorld, thePlayer.heldItem, currentBlock, classProvider.getEnumFacing(EnumFacingType.DOWN), currentBlock.getVec()))
				{
					if (visualSwing.get()) thePlayer.swingItem()
					else mc.netHandler.addToSendQueue(classProvider.createCPacketAnimation())

					clickedBlocks.add(currentBlock)
					this.currentBlock = null
					timer.reset()
					delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
				}
			}
		}
	}

	override fun onDisable() = clickedBlocks.clear()

	override val tag: String
		get() = "${rangeValue.get()}"
}
