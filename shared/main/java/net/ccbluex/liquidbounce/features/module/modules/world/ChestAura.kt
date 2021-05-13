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
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.extensions.getVec
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.*
import kotlin.random.Random

@ModuleInfo(name = "ChestAura", description = "Automatically opens chests around you.", category = ModuleCategory.WORLD)
object ChestAura : Module()
{
	private val chestValue = BlockValue("Chest", functions.getIdFromBlock(classProvider.getBlockEnum(BlockType.CHEST)))
	private val rangeValue = FloatValue("Range", 5F, 1F, 6F)
	private val priorityValue = ListValue("Priority", arrayOf("Distance", "ServerDirection", "ClientDirection"), "Distance")
	private val maxDelayValue = IntegerValue("MaxDelay", 100, 50, 200)
	private val minDelayValue = IntegerValue("MinDelay", 100, 50, 200)
	private val rotationsValue = BoolValue("Rotations", true)
	private val keepRotationValue = BoolValue("KeepRotation", true)

	private val minKeepRotationTicksValue: IntegerValue = object : IntegerValue("MinKeepRotationTicks", 20, 0, 50)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxKeepRotationTicksValue.get()
			if (i < newValue) this.set(i)
		}
	}
	private val maxKeepRotationTicksValue: IntegerValue = object : IntegerValue("MaxKeepRotationTicks", 30, 0, 50)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minKeepRotationTicksValue.get()
			if (i > newValue) this.set(i)
		}
	}

	// Reset Turn Speed
	private val maxResetTurnSpeed: FloatValue = object : FloatValue("MaxRotationResetSpeed", 180f, 20f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minResetTurnSpeed.get()
			if (v > newValue) this.set(v)
		}
	}
	private val minResetTurnSpeed: FloatValue = object : FloatValue("MinRotationResetSpeed", 180f, 20f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxResetTurnSpeed.get()
			if (v < newValue) this.set(v)
		}
	}

	// Turn Speed
	private val maxTurnSpeedValue: FloatValue = object : FloatValue("MaxTurnSpeed", 180f, 1f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minTurnSpeedValue.get()
			if (v > newValue) set(v)
			if (maximum < newValue)
			{
				set(maximum)
			}
			else if (minimum > newValue)
			{
				set(minimum)
			}
		}
	}
	private val minTurnSpeedValue: FloatValue = object : FloatValue("MinTurnSpeed", 180f, 1f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxTurnSpeedValue.get()
			if (v < newValue) set(v)
			if (maximum < newValue)
			{
				set(maximum)
			}
			else if (minimum > newValue)
			{
				set(minimum)
			}
		}
	}

	private val throughWallsValue = BoolValue("ThroughWalls", true)
	private val visualSwing = BoolValue("VisualSwing", true)
	private val noHitValue = BoolValue("NoHit", true)

	var currentBlock: WBlockPos? = null

	private val timer = MSTimer()
	private var delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

	val clickedBlocks = mutableListOf<WBlockPos>()

	private var facesBlock = false

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

				val chestID = chestValue.get()
				val range = rangeValue.get()
				val radius = range + 1
				val eyesPos = WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)

				val throughWalls = throughWallsValue.get()

				val prioritySelector = { blockPos: WBlockPos ->
					when (priorityValue.get().toLowerCase())
					{
						"serverdirection" -> RotationUtils.getServerRotationDifference(thePlayer, blockPos, false, 0f, 0f)
						"clientdirection" -> RotationUtils.getClientRotationDifference(thePlayer, blockPos, false, 0f, 0f)
						else -> BlockUtils.getCenterDistance(thePlayer, blockPos)
					}
				}

				currentBlock = BlockUtils.searchBlocks(theWorld, thePlayer, radius.toInt()).asSequence().filter { func.getIdFromBlock(it.value) == chestID }.filter { !clickedBlocks.contains(it.key) }.filter { BlockUtils.getCenterDistance(thePlayer, it.key) < range }.run {
					if (throughWalls) this else filter { (pos, _) -> (theWorld.rayTraceBlocks(eyesPos, pos.getVec(), stopOnLiquid = false, ignoreBlockWithoutBoundingBox = true, returnLastUncollidableBlock = false) ?: return@filter false).blockPos == pos }
				}.minBy { prioritySelector(it.key) }?.key

				if (rotationsValue.get())
				{
					val currentBlock = currentBlock ?: return
					val vecRotation = RotationUtils.faceBlock(theWorld, thePlayer, currentBlock) ?: return
					val rotation = vecRotation.rotation
					val posVec = vecRotation.vec

					val keepRotationTicks = if (keepRotationValue.get()) if (maxKeepRotationTicksValue.get() == minKeepRotationTicksValue.get()) maxKeepRotationTicksValue.get() else minKeepRotationTicksValue.get() + Random.nextInt(maxKeepRotationTicksValue.get() - minKeepRotationTicksValue.get()) else 0

					if (minTurnSpeedValue.get() < 180)
					{
						val limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, rotation, (Random.nextFloat() * (maxTurnSpeedValue.get() - minTurnSpeedValue.get()) + minTurnSpeedValue.get()), 0.0F)
						RotationUtils.setTargetRotation(limitedRotation, keepRotationTicks)
						RotationUtils.setNextResetTurnSpeed(minResetTurnSpeed.get().coerceAtLeast(20F), maxResetTurnSpeed.get().coerceAtLeast(20F))

						facesBlock = false

						if (!BlockUtils.canBeClicked(theWorld, currentBlock)) return

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
						RotationUtils.setNextResetTurnSpeed(minResetTurnSpeed.get().coerceAtLeast(20F), maxResetTurnSpeed.get().coerceAtLeast(20F))

						facesBlock = true
					}
				}
			}

			EventState.POST -> if (currentBlock != null && (!rotationsValue.get() || facesBlock) && timer.hasTimePassed(delay))
			{
				val currentBlock = currentBlock ?: return

				CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)

				if (mc.playerController.onPlayerRightClick(thePlayer, theWorld, thePlayer.heldItem, currentBlock, provider.getEnumFacing(EnumFacingType.DOWN), currentBlock.getVec()))
				{
					if (visualSwing.get()) thePlayer.swingItem() else mc.netHandler.addToSendQueue(provider.createCPacketAnimation())

					clickedBlocks.add(currentBlock)
					this.currentBlock = null
					timer.reset()
					delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
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
