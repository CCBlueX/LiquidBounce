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
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isFullBlock
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import java.awt.Color

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

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val moduleManager = LiquidBounce.moduleManager

		if (noHitValue.get())
		{
			val killAura = moduleManager[KillAura::class.java] as KillAura

			if (killAura.state && killAura.target != null) return
		}

		val targetId = blockValue.get()

		if (currentPos == null || functions.getIdFromBlock(getBlock(theWorld, currentPos!!)) != targetId || getCenterDistance(thePlayer, currentPos!!) > rangeValue.get()) currentPos = find(theWorld, thePlayer, targetId)

		// Reset current breaking when there is no target block
		if (currentPos == null)
		{
			currentDamage = 0F
			return
		}

		var currentPos = currentPos ?: return
		var rotations = RotationUtils.faceBlock(theWorld, thePlayer, currentPos) ?: return

		// Surroundings
		var surroundings = false

		val provider = classProvider

		if (surroundingsValue.get())
		{
			val eyes = thePlayer.getPositionEyes(1F)
			val blockPos = theWorld.rayTraceBlocks(eyes, rotations.vec, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = false, returnLastUncollidableBlock = true)?.blockPos

			if (blockPos != null && !provider.isBlockAir(blockPos))
			{
				if (currentPos.x != blockPos.x || currentPos.y != blockPos.y || currentPos.z != blockPos.z) surroundings = true

				this.currentPos = blockPos
				currentPos = this.currentPos ?: return
				rotations = RotationUtils.faceBlock(theWorld, thePlayer, currentPos) ?: return
			}
		}

		// Reset switch timer when position changed
		if (oldPos != null && oldPos != currentPos)
		{
			currentDamage = 0F
			switchTimer.reset()
		}

		oldPos = currentPos

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

				if (autoTool.state) autoTool.switchSlot(currentPos)

				// Break block
				if (instantValue.get())
				{
					// CivBreak style block breaking
					netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.START_DESTROY_BLOCK, currentPos, provider.getEnumFacing(EnumFacingType.DOWN)))

					if (swingValue.get()) thePlayer.swingItem()

					netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.STOP_DESTROY_BLOCK, currentPos, provider.getEnumFacing(EnumFacingType.DOWN)))
					currentDamage = 0F
					return
				}

				// Minecraft block breaking
				val block = currentPos.getBlock(theWorld)

				if (currentDamage == 0F)
				{
					netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.START_DESTROY_BLOCK, currentPos, provider.getEnumFacing(EnumFacingType.DOWN)))

					if (thePlayer.capabilities.isCreativeMode || block.getPlayerRelativeBlockHardness(thePlayer, theWorld, this.currentPos!!) >= 1.0F)
					{
						if (swingValue.get()) thePlayer.swingItem()
						mc.playerController.onPlayerDestroyBlock(this.currentPos!!, provider.getEnumFacing(EnumFacingType.DOWN))

						currentDamage = 0F
						this.currentPos = null
						return
					}
				}

				if (swingValue.get()) thePlayer.swingItem()

				currentDamage += block.getPlayerRelativeBlockHardness(thePlayer, theWorld, currentPos)
				theWorld.sendBlockBreakProgress(thePlayer.entityId, currentPos, (currentDamage * 10F).toInt() - 1)

				if (currentDamage >= 1F)
				{
					netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.STOP_DESTROY_BLOCK, currentPos, provider.getEnumFacing(EnumFacingType.DOWN)))
					mc.playerController.onPlayerDestroyBlock(currentPos, provider.getEnumFacing(EnumFacingType.DOWN))
					blockHitDelay = 4
					currentDamage = 0F
					this.currentPos = null
				}
			}

			// Use block
			actionValue.get().equals("use", true) -> if (mc.playerController.onPlayerRightClick(thePlayer, theWorld, thePlayer.heldItem!!, this.currentPos!!, provider.getEnumFacing(EnumFacingType.DOWN), WVec3(currentPos.x.toDouble(), currentPos.y.toDouble(), currentPos.z.toDouble())))
			{
				if (swingValue.get()) thePlayer.swingItem()

				blockHitDelay = 4
				currentDamage = 0F
				this.currentPos = null
			}
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		RenderUtils.drawBlockBox(mc.theWorld ?: return, mc.thePlayer ?: return, currentPos ?: return, Color.RED, true)
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

			"around" -> !isFullBlock(theWorld, blockPos.down()) || !isFullBlock(theWorld, blockPos.up()) || !isFullBlock(theWorld, blockPos.north()) || !isFullBlock(theWorld, blockPos.east()) || !isFullBlock(theWorld, blockPos.south()) || !isFullBlock(theWorld, blockPos.west())
			else -> true
		}
	}

	override val tag: String
		get() = getBlockName(blockValue.get())
}
