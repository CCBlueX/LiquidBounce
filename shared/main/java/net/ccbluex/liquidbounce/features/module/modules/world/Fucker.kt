/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.player.AutoTool
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isFullBlock
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*

@ModuleInfo(name = "Fucker", description = "Destroys selected blocks around you. (a.k.a.  IDNuker, BedNuker, EggNuker, BedDestroyer, etc.)", category = ModuleCategory.WORLD)
object Fucker : Module()
{

	/**
	 * SETTINGS
	 */

	private val blockValue = BlockValue("Block", 26)
	private val throughWallsValue = ListValue("ThroughWalls", arrayOf("None", "Raycast", "Around"), "None")
	private val rangeValue = FloatValue("Range", 5F, 1F, 7F)
	private val actionValue = ListValue("Action", arrayOf("Destroy", "Use"), "Destroy")
	private val instantValue = BoolValue("Instant", false)
	private val switchValue = IntegerValue("SwitchDelay", 250, 0, 1000)
	private val swingValue = BoolValue("Swing", true)
	private val rotationsValue = BoolValue("Rotations", true)
	private val surroundingsValue = BoolValue("Surroundings", true)
	private val noHitValue = BoolValue("NoHit", false)

	/**
	 * VALUES
	 */

	var currentPos: WBlockPos? = null
	private var oldPos: WBlockPos? = null
	private var blockHitDelay = 0
	private val switchTimer = MSTimer()
	var currentDamage = 0F

	@JvmStatic
	private val facings = EnumFacingType.values().map(classProvider::getEnumFacing)

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val moduleManager = LiquidBounce.moduleManager

		if (noHitValue.get() && (moduleManager[KillAura::class.java] as KillAura).hasTarget) return

		val targetId = blockValue.get()

		val currentPos = currentPos

		if (currentPos == null || functions.getIdFromBlock(getBlock(theWorld, currentPos)) != targetId || getCenterDistance(thePlayer, currentPos) > rangeValue.get()) this.currentPos = find(theWorld, thePlayer, targetId)

		// Reset current breaking when there is no target block
		if (currentPos == null)
		{
			currentDamage = 0F
			return
		}

		var newPos = currentPos
		var rotations = RotationUtils.faceBlock(theWorld, thePlayer, newPos) ?: return

		// Surroundings
		var surroundings = false

		val provider = classProvider

		if (surroundingsValue.get())
		{
			val eyes = thePlayer.getPositionEyes(1F)
			val blockPos = theWorld.rayTraceBlocks(eyes, rotations.vec, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = false, returnLastUncollidableBlock = true)?.blockPos

			if (blockPos != null && !provider.isBlockAir(blockPos))
			{
				if (newPos.x != blockPos.x || newPos.y != blockPos.y || newPos.z != blockPos.z) surroundings = true

				this.currentPos = blockPos
				newPos = currentPos
				rotations = RotationUtils.faceBlock(theWorld, thePlayer, newPos) ?: return
			}
		}

		// Reset switch timer when position changed
		if (currentPos != newPos)
		{
			currentDamage = 0F
			switchTimer.reset()
		}

		this.currentPos = newPos

		if (!switchTimer.hasTimePassed(switchValue.get().toLong())) return

		// Block hit delay
		if (blockHitDelay > 0)
		{
			blockHitDelay--
			return
		}

		// Face block
		if (rotationsValue.get()) RotationUtils.setTargetRotation(rotations.rotation)

		when
		{

			// Destory block
			actionValue.get().equals("destroy", true) || surroundings ->
			{

				// Auto Tool
				val autoTool = moduleManager[AutoTool::class.java] as AutoTool
				val netHandler = mc.netHandler

				if (autoTool.state) autoTool.switchSlot(newPos)

				CPSCounter.registerClick(CPSCounter.MouseButton.LEFT)

				// Break block
				if (instantValue.get())
				{
					// CivBreak style block breaking
					netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.START_DESTROY_BLOCK, newPos, provider.getEnumFacing(EnumFacingType.DOWN)))

					if (swingValue.get()) thePlayer.swingItem()

					netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.STOP_DESTROY_BLOCK, newPos, provider.getEnumFacing(EnumFacingType.DOWN)))
					currentDamage = 0F
					return
				}

				// Minecraft block breaking
				val block = newPos.getBlock(theWorld)

				if (currentDamage == 0F)
				{
					netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.START_DESTROY_BLOCK, newPos, provider.getEnumFacing(EnumFacingType.DOWN)))

					if (thePlayer.capabilities.isCreativeMode || block.getPlayerRelativeBlockHardness(thePlayer, theWorld, currentPos) >= 1.0F)
					{
						if (swingValue.get()) thePlayer.swingItem()
						mc.playerController.onPlayerDestroyBlock(currentPos, provider.getEnumFacing(EnumFacingType.DOWN))

						currentDamage = 0F
						this.currentPos = null
						return
					}
				}

				if (swingValue.get()) thePlayer.swingItem()

				currentDamage += block.getPlayerRelativeBlockHardness(thePlayer, theWorld, newPos)
				theWorld.sendBlockBreakProgress(thePlayer.entityId, newPos, (currentDamage * 10F).toInt() - 1)

				if (currentDamage >= 1F)
				{
					netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.STOP_DESTROY_BLOCK, newPos, provider.getEnumFacing(EnumFacingType.DOWN)))
					mc.playerController.onPlayerDestroyBlock(newPos, provider.getEnumFacing(EnumFacingType.DOWN))
					blockHitDelay = 4
					currentDamage = 0F
					this.currentPos = null
				}
			}

			// Use block
			actionValue.get().equals("use", true) ->
			{
				CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)

				if (mc.playerController.onPlayerRightClick(thePlayer, theWorld, thePlayer.heldItem, currentPos, provider.getEnumFacing(EnumFacingType.DOWN), WVec3(newPos.x.toDouble(), newPos.y.toDouble(), newPos.z.toDouble())))
				{
					if (swingValue.get()) thePlayer.swingItem()

					blockHitDelay = 4
					currentDamage = 0F
					this.currentPos = null
				}
			}
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		RenderUtils.drawBlockBox(mc.theWorld ?: return, mc.thePlayer ?: return, currentPos ?: return, -65536, 0, hydraESP = false)
	}

	/*/**
	 * Find new target block by [targetID]
	 */private fun find(targetID: Int) =
        searchBlocks(rangeValue.get().toInt() + 1).filter {
                    Block.getIdFromBlock(it.value) == targetID && getCenterDistance(it.key) <= rangeValue.get()
                            && (isHitable(it.key) || surroundingsValue.get())
                }.minBy { getCenterDistance(it.key) }?.key*/

	//Removed triple iteration of blocks to improve speed

	/**
	 * Find new target block by [targetID]
	 */
	private fun find(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, targetID: Int): WBlockPos?
	{
		val func = functions

		val surroundings = surroundingsValue.get()
		val range = rangeValue.get()

		val radius = range.toInt() + 1

		var nearestBlockDistance = Double.MAX_VALUE
		var nearestBlock: WBlockPos? = null

		val posXI = thePlayer.posX.toInt()
		val posYI = thePlayer.posY.toInt()
		val posZI = thePlayer.posZ.toInt()

		(radius downTo -radius + 1).forEach { x ->
			(radius downTo -radius + 1).forEach { y ->
				(radius downTo -radius + 1).asSequence().map { z -> WBlockPos(posXI + x, posYI + y, posZI + z) }.filter { blockPos -> func.getIdFromBlock(getBlock(theWorld, blockPos)) == targetID }.map { it to getCenterDistance(thePlayer, it) }.filter { it.second <= range }.filter { it.second <= nearestBlockDistance }.filter { surroundings || isHitable(theWorld, thePlayer, it.first) }.toList().forEach {
					nearestBlockDistance = it.second
					nearestBlock = it.first
				}
			}
		}

		return nearestBlock
	}

	/**
	 * Check if block is hitable (or allowed to hit through walls)
	 */
	private fun isHitable(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, blockPos: WBlockPos): Boolean
	{
		return when (throughWallsValue.get().toLowerCase())
		{
			"raycast" ->
			{
				val eyesPos = WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)
				val movingObjectPosition = theWorld.rayTraceBlocks(eyesPos, WVec3(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5), stopOnLiquid = false, ignoreBlockWithoutBoundingBox = true, returnLastUncollidableBlock = false)

				movingObjectPosition != null && movingObjectPosition.blockPos == blockPos
			}

			"around" -> facings.any { !isFullBlock(theWorld, blockPos.offset(it)) }
			else -> true
		}
	}

	override val tag: String
		get() = getBlockName(blockValue.get())
}
