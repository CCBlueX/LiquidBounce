/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition
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
import net.ccbluex.liquidbounce.utils.extensions.canBeClicked
import net.ccbluex.liquidbounce.utils.extensions.distanceToCenter
import net.ccbluex.liquidbounce.utils.extensions.searchBlocks
import net.ccbluex.liquidbounce.utils.extensions.vec
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*

@ModuleInfo(name = "ChestAura", description = "Automatically opens chests around you.", category = ModuleCategory.WORLD)
object ChestAura : Module()
{
	private val chestGroup = ValueGroup("Chest")
	private val chestFirstValue = BlockValue("First", functions.getIdFromBlock(classProvider.getBlockEnum(BlockType.CHEST)), "Chest")
	private val chestSecondValue = BlockValue("Second", functions.getIdFromBlock(classProvider.getBlockEnum(BlockType.AIR)))
	private val chestThirdValue = BlockValue("Third", functions.getIdFromBlock(classProvider.getBlockEnum(BlockType.AIR)))

	private val rangeValue = FloatValue("Range", 5F, 1F, 6F)
	private val priorityValue = ListValue("Priority", arrayOf("Distance", "ServerDirection", "ClientDirection"), "Distance")
	private val delayValue = IntegerRangeValue("Delay", 100, 100, 50, 200, "MaxDelay" to "MinDelay")

	private val rotationGroup = ValueGroup("Rotation")
	private val rotationEnabledValue = BoolValue("Enabled", true, "Rotations")
	private val rotationTurnSpeedValue = FloatRangeValue("TurnSpeed", 180f, 180f, 1f, 180f, "MaxTurnSpeed" to "MinTurnSpeed")
	private val rotationResetSpeedValue = FloatRangeValue("RotationResetSpeed", 180f, 180f, 10f, 180f, "MaxRotationResetSpeed" to "MinRotationResetSpeed")

	private val rotationKeepRotationGroup = ValueGroup("KeepRotation")
	private val rotationKeepRotationEnabledValue = BoolValue("Enabled", true, "KeepRotation")
	private val rotationKeepRotationTicksValue = IntegerRangeValue("Ticks", 20, 30, 0, 60, "MaxKeepRotationTicks" to "MinKeepRotationTicks")

	private val throughWallsValue = BoolValue("ThroughWalls", true)
	private val visualSwing = BoolValue("VisualSwing", true)
	private val noHitValue = BoolValue("KillAuraBypass", true, "NoHit")

	var currentBlock: WBlockPos? = null

	private val timer = MSTimer()
	private var delay = delayValue.getRandomDelay()

	val clickedBlocks = mutableListOf<WBlockPos>()

	private var facesBlock = false

	init
	{
		chestGroup.addAll(chestFirstValue, chestSecondValue, chestThirdValue)
		rotationKeepRotationGroup.addAll(rotationKeepRotationEnabledValue, rotationKeepRotationTicksValue)
		rotationGroup.addAll(rotationEnabledValue, rotationTurnSpeedValue, rotationResetSpeedValue, rotationKeepRotationGroup)
	}

	@EventTarget
	fun onMotion(event: MotionEvent)
	{
		val moduleManager = LiquidBounce.moduleManager
		if (moduleManager[Blink::class.java].state || (noHitValue.get() && (moduleManager[KillAura::class.java] as KillAura).hasTarget)) return

		val thePlayer = mc.thePlayer ?: return
		val theWorld = mc.theWorld ?: return

		val provider = classProvider
		val func = functions

		when (event.eventState)
		{
			EventState.PRE ->
			{
				if (provider.isGuiContainer(mc.currentScreen)) timer.reset() // No delay re-randomize code here because the performance impact is more than your think.

				val range = rangeValue.get()
				val radius = range + 1
				val eyesPos = WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)

				val throughWalls = throughWallsValue.get()

				val prioritySelector = { blockPos: WBlockPos ->
					when (priorityValue.get().toLowerCase())
					{
						"serverdirection" -> RotationUtils.getServerRotationDifference(thePlayer, blockPos, false, RotationUtils.MinMaxPair.ZERO)
						"clientdirection" -> RotationUtils.getClientRotationDifference(thePlayer, blockPos, false, RotationUtils.MinMaxPair.ZERO)
						else -> thePlayer.distanceToCenter(blockPos)
					}
				}

				currentBlock = theWorld.searchBlocks(thePlayer, radius.toInt()).asSequence().filter { (_, block) ->
					val id = func.getIdFromBlock(block)
					arrayOf(chestFirstValue.get(), chestSecondValue.get(), chestThirdValue.get()).filterNot { it == 0 }.any { it == id }
				}.filter { !clickedBlocks.contains(it.key) }.filter { thePlayer.distanceToCenter(it.key) < range }.run {
					if (!throughWalls) this else filter { (pos, _) -> (theWorld.rayTraceBlocks(eyesPos, pos.vec, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = true, returnLastUncollidableBlock = false) ?: return@filter false).blockPos == pos }
				}.minBy { prioritySelector(it.key) }?.key

				if (rotationEnabledValue.get())
				{
					val currentBlock = currentBlock ?: return
					val vecRotation = RotationUtils.faceBlock(theWorld, thePlayer, currentBlock) ?: return
					val rotation = vecRotation.rotation
					val posVec = vecRotation.vec

					val keepRotationTicks = if (rotationKeepRotationEnabledValue.get()) rotationKeepRotationTicksValue.getRandom() else 0

					if (rotationTurnSpeedValue.getMin() < 180)
					{
						val limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, rotation, rotationTurnSpeedValue.getRandomStrict(), 0.0F)
						RotationUtils.setTargetRotation(limitedRotation, keepRotationTicks)
						RotationUtils.setNextResetTurnSpeed(rotationResetSpeedValue.getMin().coerceAtLeast(10F), rotationResetSpeedValue.getMax().coerceAtLeast(10F))

						facesBlock = false

						if (!theWorld.canBeClicked(currentBlock)) return

						if (!throughWalls && (eyesPos.squareDistanceTo(posVec) > 18.0 || run {
								val rayTrace = theWorld.rayTraceBlocks(eyesPos, posVec, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = true, returnLastUncollidableBlock = false)

								rayTrace == null || rayTrace.typeOfHit != IMovingObjectPosition.WMovingObjectType.BLOCK || rayTrace.blockPos != currentBlock
							})) return

						val rotationVector = RotationUtils.getVectorForRotation(limitedRotation)
						val vector = eyesPos.addVector(rotationVector.xCoord * range, rotationVector.yCoord * range, rotationVector.zCoord * range)
						val rayTrace = theWorld.rayTraceBlocks(eyesPos, vector, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = false, returnLastUncollidableBlock = true)

						if (rayTrace != null && (rayTrace.typeOfHit != IMovingObjectPosition.WMovingObjectType.BLOCK || rayTrace.blockPos != currentBlock)) return

						facesBlock = true
					}
					else
					{
						RotationUtils.setTargetRotation(rotation, keepRotationTicks)
						RotationUtils.setNextResetTurnSpeed(rotationResetSpeedValue.getMin().coerceAtLeast(10F), rotationResetSpeedValue.getMax().coerceAtLeast(10F))

						facesBlock = true
					}
				}
			}

			EventState.POST -> if (currentBlock != null && (!rotationEnabledValue.get() || facesBlock) && timer.hasTimePassed(delay))
			{
				val currentBlock = currentBlock ?: return

				CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)

				if (mc.playerController.onPlayerRightClick(thePlayer, theWorld, thePlayer.heldItem, currentBlock, provider.getEnumFacing(EnumFacingType.DOWN), currentBlock.vec))
				{
					if (visualSwing.get()) thePlayer.swingItem() else mc.netHandler.addToSendQueue(provider.createCPacketAnimation())

					clickedBlocks.add(currentBlock)
					this.currentBlock = null
					timer.reset()
					delay = delayValue.getRandomDelay()
				}
			}
		}
	}

	override fun onDisable()
	{
		clickedBlocks.clear()
		facesBlock = false
	}

	override val tag: String
		get() = "${rangeValue.get()}"
}
