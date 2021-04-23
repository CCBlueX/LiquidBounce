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
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.extensions.getVec
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BlockValue
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue

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
		val moduleManager = LiquidBounce.moduleManager
		if (moduleManager[Blink::class.java].state || (moduleManager[KillAura::class.java] as KillAura).hasTarget) return

		val thePlayer = mc.thePlayer ?: return
		val theWorld = mc.theWorld ?: return

		val provider = classProvider
		val func = functions

		when (event.eventState)
		{
			EventState.PRE ->
			{
				if (provider.isGuiContainer(mc.currentScreen)) timer.reset() // No delay re-randomize code here because the performance impact is more than your think.

				val chestID = chestValue.get()
				val range = rangeValue.get()
				val radius = range + 1
				val eyesPos = WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)

				val throughWalls = throughWallsValue.get()


				currentBlock = BlockUtils.searchBlocks(theWorld, thePlayer, radius.toInt()).asSequence().filter { func.getIdFromBlock(it.value) == chestID }.filter { !clickedBlocks.contains(it.key) }.filter { BlockUtils.getCenterDistance(thePlayer, it.key) < range }.run {
					if (throughWalls) this
					else filter { (pos, _) -> (theWorld.rayTraceBlocks(eyesPos, pos.getVec(), stopOnLiquid = false, ignoreBlockWithoutBoundingBox = true, returnLastUncollidableBlock = false) ?: return@filter false).blockPos == pos }
				}.minBy { BlockUtils.getCenterDistance(thePlayer, it.key) }?.key

				if (rotationsValue.get()) RotationUtils.setTargetRotation((RotationUtils.faceBlock(theWorld, thePlayer, currentBlock ?: return) ?: return).rotation)
			}

			EventState.POST -> if (currentBlock != null && timer.hasTimePassed(delay))
			{
				val currentBlock = currentBlock ?: return

				CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)

				if (mc.playerController.onPlayerRightClick(thePlayer, theWorld, thePlayer.heldItem, currentBlock, provider.getEnumFacing(EnumFacingType.DOWN), currentBlock.getVec()))
				{
					if (visualSwing.get()) thePlayer.swingItem()
					else mc.netHandler.addToSendQueue(provider.createCPacketAnimation())

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
