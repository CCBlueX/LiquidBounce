package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.enums.MaterialType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos.Companion.ORIGIN
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.EntityUtils.isEnemy
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getState
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.pathfinding.PathFinder
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.min

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author LeakedPvP
 * @game   Minecraft
 * // TODO: Maximum packets per ticks limit
 */
@ModuleInfo(name = "TpAura", description = "InfiniteAura from Sigma 4.1.", category = ModuleCategory.COMBAT)
class TpAura : Module()
{
	/**
	 * Options
	 */
	private val cpsValue: IntegerRangeValue = object : IntegerRangeValue("CPS", 3, 5, 1, 20, "MaxCPS" to "MinCPS")
	{
		override fun onMaxValueChanged(oldValue: Int, newValue: Int)
		{
			attackDelay = TimeUtils.randomClickDelay(getMin(), newValue)
		}

		override fun onMinValueChanged(oldValue: Int, newValue: Int)
		{
			attackDelay = TimeUtils.randomClickDelay(newValue, getMax())
		}
	}

	private val rangeValue = FloatValue("Range", 30.0f, 6.1f, 1000.0f)
	private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)
	val maxTargetsValue = IntegerValue("MaxTargets", 4, 1, 50)
	private val maxDashDistanceValue = IntegerValue("DashDistance", 5, 1, 10)
	private val autoBlockValue = ListValue("AutoBlock", arrayOf("Off", "Fake", "Packet", "AfterTick"), "Packet")
	private val swingValue = BoolValue("Swing", true)

	private val pathEspGroup = ValueGroup("PathESP")
	private val pathEspEnabledValue = BoolValue("Enabled", true, "PathESP")
	private val pathEspTagValue = BoolValue("Tag", false)
	private val pathEspLineWidthValue = FloatValue("LineWidth", 1f, 0.5f, 2f)
	private val pathEspTimeValue = IntegerValue("KeepLength", 1000, 100, 3000, "PathESPTime")

	private val pathEspColorValue = RGBAColorValue("Color", 255, 179, 72, 255, listOf("PathESP-Red", "PathESP-Green", "PathESP-Blue", "PathESP-Alpha"))
	private val pathEspColorRainbowGroup = ValueGroup("Rainbow")
	private val pathEspColorRainbowEnabledValue = BoolValue("Enabled", false, "PathESP-Rainbow")
	private val pathEspColorRainbowSpeedValue = IntegerValue("Speed", 10, 1, 10, "PathESP-RainbowSpeed")
	private val pathEspColorRainbowOffsetValue = IntegerValue("IndexOffset", 0, -100, 100, "PathESP-RainbowIndexOffset")
	private val pathEspColorRainbowSaturationValue = FloatValue("Saturation", 1.0f, 0.0f, 1.0f, "PathESP-RainbowHSB-Saturation")
	private val pathEspColorRainbowBrightnessValue = FloatValue("Brightness", 1.0f, 0.0f, 1.0f, "PathESP-RainbowHSB-Brightness")

	/**
	 * Variables
	 */

	// Attack Delay
	private val attackTimer = MSTimer()
	var attackDelay = cpsValue.getRandomClickDelay()

	// Paths
	private val targetPaths = mutableListOf<List<WVec3>>()

	// Targets
	private var currentTargets: MutableList<IEntityLivingBase> = CopyOnWriteArrayList()
	var currentTarget: IEntityLivingBase? = null
	private var currentPath = mutableListOf<WVec3>()

	// Blocking Status
	var clientSideBlockingStatus = false
	var serverSideBlockingStatus = false

	var debug: String? = null

	init
	{
		pathEspColorRainbowGroup.addAll(pathEspColorRainbowEnabledValue, pathEspColorRainbowSpeedValue, pathEspColorRainbowOffsetValue, pathEspColorRainbowSaturationValue, pathEspColorRainbowBrightnessValue)
		pathEspGroup.addAll(pathEspEnabledValue, pathEspTagValue, pathEspLineWidthValue, pathEspTimeValue, pathEspColorValue, pathEspColorRainbowGroup)
	}

	override fun onEnable()
	{
		currentTargets.clear()
	}

	override fun onDisable()
	{
		currentTargets.clear()

		if (serverSideBlockingStatus)
		{
			val provider = classProvider

			mc.netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM, ORIGIN, provider.getEnumFacing(EnumFacingType.DOWN)))
			serverSideBlockingStatus = false
		}

		clientSideBlockingStatus = false
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return
		val netHandler = mc.netHandler
		val networkManager = netHandler.networkManager

		currentTargets = getTargets(theWorld, thePlayer)

		if (attackTimer.hasTimePassed(attackDelay))
		{
			val provider = classProvider

			if (currentTargets.isNotEmpty())
			{
				targetPaths.clear()
				if (canBlock(thePlayer) && (thePlayer.isBlocking || !autoBlockValue.get().equals("Off", ignoreCase = true))) clientSideBlockingStatus = true

				val from = WVec3(thePlayer.posX, thePlayer.posY, thePlayer.posZ)
				var targetIndex = 0

				val targetCount = if (currentTargets.size > maxTargetsValue.get()) maxTargetsValue.get() else currentTargets.size
				val single = targetCount == 1

				while (targetIndex < targetCount)
				{
					val currentTarget = currentTargets[targetIndex]

					this.currentTarget = currentTarget

					val to = WVec3(currentTarget.posX, currentTarget.posY, currentTarget.posZ)

					currentPath = computePath(theWorld, from, to)
					targetPaths.add(currentPath) // Used for path esp

					if (single) debug = "pathCount: ${currentPath.size}"

					// Unblock before attack
					if (thePlayer.isBlocking || autoBlockValue.get().equals("Packet", ignoreCase = true) || serverSideBlockingStatus)
					{
						netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM, ORIGIN, provider.getEnumFacing(EnumFacingType.DOWN)))
						serverSideBlockingStatus = false
					}

					// Travel to the target
					currentPath.forEach { path ->
						networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(path.xCoord, path.yCoord, path.zCoord, true))
					}

					currentPath.reverse()

					LiquidBounce.eventManager.callEvent(AttackEvent(currentTarget, currentPath.firstOrNull() ?: from))

					CPSCounter.registerClick(CPSCounter.MouseButton.LEFT)

					if (swingValue.get()) thePlayer.swingItem()

					// Make AutoWeapon compatible
					var sendAttack = true
					val attackPacket: IPacket = provider.createCPacketUseEntity(currentTarget, ICPacketUseEntity.WAction.ATTACK)
					val autoWeapon = LiquidBounce.moduleManager[AutoWeapon::class.java] as AutoWeapon

					if (autoWeapon.state)
					{
						val packetEvent = PacketEvent(attackPacket)
						autoWeapon.onPacket(packetEvent)
						if (packetEvent.isCancelled) sendAttack = false
					}
					if (sendAttack) netHandler.addToSendQueue(attackPacket)

					// Block after attack
					if (canBlock(thePlayer) && !serverSideBlockingStatus && (thePlayer.isBlocking || autoBlockValue.get().equals("Packet", ignoreCase = true)))
					{
						netHandler.addToSendQueue(provider.createCPacketPlayerBlockPlacement(thePlayer.inventory.getCurrentItemInHand()))
						serverSideBlockingStatus = true
					}

					// Travel back to the original position
					currentPath.forEach { path ->
						networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(path.xCoord, path.yCoord, path.zCoord, true))
					}
					targetIndex++
				}

				attackTimer.reset()
				attackDelay = cpsValue.getRandomClickDelay()
			}
			else
			{
				if (serverSideBlockingStatus)
				{
					netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM, ORIGIN, provider.getEnumFacing(EnumFacingType.DOWN)))
					serverSideBlockingStatus = false
				}

				clientSideBlockingStatus = false
				currentTarget = null
			}
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent?)
	{
		val thePlayer = mc.thePlayer ?: return

		val renderManager = mc.renderManager
		val viewerPosX = renderManager.viewerPosX
		val viewerPosY = renderManager.viewerPosY
		val viewerPosZ = renderManager.viewerPosZ

		if (currentPath.isNotEmpty() && pathEspEnabledValue.get())
		{
			val rainbow = pathEspColorRainbowEnabledValue.get()
			val saturation = pathEspColorRainbowSaturationValue.get()
			val brightness = pathEspColorRainbowBrightnessValue.get()
			val pathEspAlpha = pathEspColorValue.getAlpha()
			val rainbowOffsetVal = 400000000L + 40000000L * pathEspColorRainbowOffsetValue.get()
			val rainbowSpeed = pathEspColorRainbowSpeedValue.get()
			val customColor = pathEspColorValue.get()

			val entityRenderer = mc.entityRenderer

			targetPaths.forEachIndexed { index, targetPath ->
				val color = if (rainbow) ColorUtils.rainbowRGB(pathEspAlpha, index * rainbowOffsetVal, rainbowSpeed, saturation, brightness) else customColor

				glPushMatrix()
				glDisable(GL_TEXTURE_2D)
				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
				glEnable(GL_LINE_SMOOTH)
				glEnable(GL_BLEND)
				glDisable(GL_DEPTH_TEST)
				entityRenderer.disableLightmap()

				RenderUtils.glColor(color)
				glLineWidth(pathEspLineWidthValue.get())
				glBegin(GL_LINE_STRIP)

				targetPath.forEach { path ->
					glVertex3d(path.xCoord - viewerPosX, path.yCoord - viewerPosY, path.zCoord - viewerPosZ)
				}

				RenderUtils.resetColor()
				glEnd()

				glEnable(GL_DEPTH_TEST)
				glDisable(GL_LINE_SMOOTH)
				glDisable(GL_BLEND)
				glEnable(GL_TEXTURE_2D)
				glPopMatrix()

				if (pathEspTagValue.get()) targetPath.firstOrNull()?.let { path ->
					targetPath.lastOrNull()?.let { lastPath ->
						RenderUtils.renderNameTag("${StringUtils.DECIMALFORMAT_2.format(WVec3(lastPath.xCoord, lastPath.yCoord, lastPath.zCoord).distanceTo(WVec3(path.xCoord, path.yCoord, path.zCoord)))}m in ${targetPath.size} moves", path.xCoord + 0.5, path.yCoord + 0.2, path.zCoord + 0.5)
					}
				}
			}
		}

		if (attackTimer.hasTimePassed(pathEspTimeValue.get().toLong()))
		{
			targetPaths.clear()
			currentPath.clear()
		}
	}

	private fun computePath(theWorld: IWorld, from: WVec3, to: WVec3): MutableList<WVec3>
	{
		var fromPos = from

		if (!canPassThrough(theWorld, WBlockPos(fromPos.xCoord, fromPos.yCoord, fromPos.zCoord))) fromPos = fromPos.addVector(0.0, 1.0, 0.0)

		val pathfinder = PathFinder(fromPos, to)
		pathfinder.compute(theWorld)

		var lastPath: WVec3? = null
		var lastEndPath: WVec3? = null
		val path = mutableListOf<WVec3>()
		val pathFinderPath = pathfinder.path

		pathFinderPath.forEachIndexed { i, currentPathFinderPath ->
			if (i == 0 || i == pathFinderPath.size - 1)
			{
				// If the current path node is start or end node
				if (lastPath != null) path.add((lastPath ?: return@forEachIndexed).addVector(0.5, 0.0, 0.5))

				path.add(currentPathFinderPath.addVector(0.5, 0.0, 0.5))
				lastEndPath = currentPathFinderPath
			}
			else
			{
				var canContinueSearching = true
				val maxDashDistance = maxDashDistanceValue.get().toFloat()
				val lastEndPathChecked = lastEndPath ?: return@forEachIndexed

				if (currentPathFinderPath.squareDistanceTo(lastEndPathChecked) > maxDashDistance * maxDashDistance) canContinueSearching = false
				else
				{
					val minX = min(lastEndPathChecked.xCoord, currentPathFinderPath.xCoord)
					val minY = min(lastEndPathChecked.yCoord, currentPathFinderPath.yCoord)
					val minZ = min(lastEndPathChecked.zCoord, currentPathFinderPath.zCoord)
					val maxX = max(lastEndPathChecked.xCoord, currentPathFinderPath.xCoord)
					val maxY = max(lastEndPathChecked.yCoord, currentPathFinderPath.yCoord)
					val maxZ = max(lastEndPathChecked.zCoord, currentPathFinderPath.zCoord)
					var x = minX.toInt()
					cordsLoop@ while (x <= maxX)
					{
						var y = minY.toInt()
						while (y <= maxY)
						{
							var z = minZ.toInt()
							while (z <= maxZ)
							{
								if (!PathFinder.checkPositionValidity(theWorld, x, y, z, false))
								{
									canContinueSearching = false
									break@cordsLoop
								}
								z++
							}
							y++
						}
						x++
					}
				}

				if (!canContinueSearching)
				{
					path.add((lastPath ?: return@forEachIndexed).addVector(0.5, 0.0, 0.5))
					lastEndPath = lastPath
				}
			}
			lastPath = currentPathFinderPath
		}

		return path
	}

	private fun getTargets(theWorld: IWorld, thePlayer: IEntity): MutableList<IEntityLivingBase>
	{
		val range = rangeValue.get().toDouble()
		val hurtTime = hurtTimeValue.get()
		return EntityUtils.getEntitiesInRadius(theWorld, thePlayer, range).asSequence().filter { isEnemy(it, false) }.map(IEntity::asEntityLivingBase).filter { it.hurtTime <= hurtTime }.filter { thePlayer.getDistanceToEntityBox(it) <= range }.sortedBy { it.getDistanceToEntity(thePlayer) * 1000 }.toMutableList()
	}

	fun isTarget(entity: IEntity?): Boolean = currentTargets.isNotEmpty() && (0 until if (currentTargets.size > maxTargetsValue.get()) maxTargetsValue.get() else currentTargets.size).any { currentTargets[it] == entity }

	override val tag: String
		get() = "${maxDashDistanceValue.get()}"

	companion object
	{
		private fun canBlock(thePlayer: IEntityPlayer): Boolean = thePlayer.heldItem != null && classProvider.isItemSword(thePlayer.heldItem?.item)

		private fun canPassThrough(theWorld: IWorld, pos: WBlockPos): Boolean
		{
			val state = getState(theWorld, WBlockPos(pos.x, pos.y, pos.z))
			val block = state.block

			val provider = classProvider

			return provider.getMaterialEnum(MaterialType.AIR) == block.getMaterial(state) || provider.getMaterialEnum(MaterialType.PLANTS) == block.getMaterial(state) || provider.getMaterialEnum(MaterialType.VINE) == block.getMaterial(state) || provider.getBlockEnum(BlockType.LADDER) == block || provider.getBlockEnum(BlockType.WATER) == block || provider.getBlockEnum(BlockType.FLOWING_WATER) == block || provider.getBlockEnum(BlockType.WALL_SIGN) == block || provider.getBlockEnum(BlockType.STANDING_SIGN) == block
		}
	}
}
