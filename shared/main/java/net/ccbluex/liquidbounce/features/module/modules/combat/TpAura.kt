package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.enums.MaterialType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos.Companion.ORIGIN
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.EntityUtils.isEnemy
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getState
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.pathfinding.PathFinder
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.stream.IntStream
import kotlin.math.max
import kotlin.math.min

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author LeakedPvP
 * @game   Minecraft
 * // TODO: Maximum packets per ticks limit
 * // FIXME: Compatibility with Criticals
 */
@ModuleInfo(name = "TpAura", description = "InfiniteAura from Sigma 4.1.", category = ModuleCategory.COMBAT)
class TpAura : Module()
{
	/**
	 * Options
	 */
	val maxCPS: IntegerValue = object : IntegerValue("MaxCPS", 6, 1, 20)
	{
		public override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minCPS.get()
			if (i > newValue) set(i)
			attackDelay = TimeUtils.randomClickDelay(i, get())
		}
	}
	val minCPS: IntegerValue = object : IntegerValue("MinCPS", 6, 1, 20)
	{
		public override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxCPS.get()
			if (i < newValue) set(i)
			attackDelay = TimeUtils.randomClickDelay(get(), i)
		}
	}

	private val rangeValue = FloatValue("Range", 30.0f, 6.1f, 1000.0f)
	private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)
	val maxTargetsValue = IntegerValue("MaxTargets", 4, 1, 50)
	private val maxDashDistanceValue = IntegerValue("DashDistance", 5, 1, 10)
	private val autoBlockValue = ListValue(
		"AutoBlock", arrayOf(
			"Off", "Fake", "Packet", "AfterTick"
		), "Packet"
	)
	private val swingValue = BoolValue("Swing", true)

	private val pathEspValue = BoolValue("PathESP", true)
	private val pathEspTime = IntegerValue("PathESPTime", 1000, 100, 3000)

	private val colorRedValue = IntegerValue("PathESP-Red", 255, 0, 255)
	private val colorGreenValue = IntegerValue("PathESP-Green", 179, 0, 255)
	private val colorBlueValue = IntegerValue("PathESP-Blue", 72, 0, 255)

	private val colorRainbow = BoolValue("PathESP-Rainbow", false)
	private val rainbowSpeedValue = IntegerValue("PathESP-RainbowSpeed", 10, 1, 10)
	private val rainbowOffsetValue = IntegerValue("PathESP-RainbowIndexOffset", 0, -100, 100)
	private val saturationValue = FloatValue("PathESP-RainbowHSB-Saturation", 1.0f, 0.0f, 1.0f)
	private val brightnessValue = FloatValue("PathESP-RainbowHSB-Brightness", 1.0f, 0.0f, 1.0f)

	/**
	 * Variables
	 */

	// Attack Delay
	private val attackTimer = MSTimer()
	var attackDelay = TimeUtils.randomClickDelay(minCPS.get(), maxCPS.get())

	// Paths
	private val targetPaths = mutableListOf<List<WVec3>>()

	// Targets
	private var currentTargets: MutableList<IEntityLivingBase> = CopyOnWriteArrayList()
	var currentTarget: IEntityLivingBase? = null
	private var currentPath = mutableListOf<WVec3>()

	// Blocking Status
	var clientSideBlockingStatus = false
	var serverSideBlockingStatus = false

	override fun onEnable()
	{
		currentTargets.clear()
	}

	override fun onDisable()
	{
		currentTargets.clear()
		clientSideBlockingStatus = false
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val thePlayer = mc.thePlayer ?: return

		currentTargets = targets

		if (attackTimer.hasTimePassed(attackDelay)) if (currentTargets.isNotEmpty())
		{
			targetPaths.clear()
			if (canBlock() && (thePlayer.isBlocking || !autoBlockValue.get().equals("Off", ignoreCase = true))) clientSideBlockingStatus = true

			val from = WVec3(thePlayer.posX, thePlayer.posY, thePlayer.posZ)
			var targetIndex = 0

			val targetCount = if (currentTargets.size > maxTargetsValue.get()) maxTargetsValue.get() else currentTargets.size

			while (targetIndex < targetCount)
			{
				currentTarget = currentTargets[targetIndex]
				val to = WVec3(currentTarget!!.posX, currentTarget!!.posY, currentTarget!!.posZ)

				currentPath = computePath(from, to)
				targetPaths.add(currentPath) // Used for path esp

				// Unblock before attack
				if (thePlayer.isBlocking || autoBlockValue.get().equals("Packet", ignoreCase = true) || serverSideBlockingStatus)
				{
					mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM, ORIGIN, classProvider.getEnumFacing(EnumFacingType.DOWN)))
					serverSideBlockingStatus = false
				}

				// Travel to the target
				for (path in currentPath) mc.netHandler.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(path.xCoord, path.yCoord, path.zCoord, true))

				LiquidBounce.eventManager.callEvent(AttackEvent(currentTarget))
				CPSCounter.registerClick(CPSCounter.MouseButton.LEFT)
				if (swingValue.get()) thePlayer.swingItem()

				// Make AutoWeapon compatible
				var sendAttack = true
				val attackPacket: IPacket = classProvider.createCPacketUseEntity(currentTarget!!, ICPacketUseEntity.WAction.ATTACK)
				val autoWeapon = LiquidBounce.moduleManager[AutoWeapon::class.java] as AutoWeapon

				if (autoWeapon.state)
				{
					val packetEvent = PacketEvent(attackPacket)
					autoWeapon.onPacket(packetEvent)
					if (packetEvent.isCancelled) sendAttack = false
				}
				if (sendAttack) mc.netHandler.addToSendQueue(attackPacket)

				// Block after attack
				if (canBlock() && !serverSideBlockingStatus && (thePlayer.isBlocking || autoBlockValue.get().equals("Packet", ignoreCase = true)))
				{
					mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerBlockPlacement(thePlayer.inventory.getCurrentItemInHand()))
					serverSideBlockingStatus = true
				}

				// Travel back to the original position
				currentPath.reverse()
				for (path in currentPath) mc.netHandler.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(path.xCoord, path.yCoord, path.zCoord, true))
				targetIndex++
			}

			attackTimer.reset()
			attackDelay = TimeUtils.randomClickDelay(minCPS.get(), maxCPS.get())
		}
		else
		{
			clientSideBlockingStatus = false
			currentTarget = null
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent?)
	{
		val renderManager = mc.renderManager
		val viewerPosX = renderManager.viewerPosX
		val viewerPosY = renderManager.viewerPosY
		val viewerPosZ = renderManager.viewerPosZ

		if (currentPath.isNotEmpty() && pathEspValue.get())
		{
			val rainbow = colorRainbow.get()
			val saturation = saturationValue.get()
			val brightness = brightnessValue.get()
			val rainbowOffsetVal = 400000000L + 40000000L * rainbowOffsetValue.get()
			val rainbowSpeed = rainbowSpeedValue.get()
			val customColor = Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())

			val entityRenderer = mc.entityRenderer

			targetPaths.forEachIndexed { index, targetPath ->
				val color = if (rainbow) ColorUtils.rainbow(offset = index * rainbowOffsetVal, speed = rainbowSpeed, saturation = saturation, brightness = brightness) else customColor

				GL11.glPushMatrix()
				GL11.glDisable(GL11.GL_TEXTURE_2D)
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
				GL11.glEnable(GL11.GL_LINE_SMOOTH)
				GL11.glEnable(GL11.GL_BLEND)
				GL11.glDisable(GL11.GL_DEPTH_TEST)
				entityRenderer.disableLightmap()

				GL11.glBegin(GL11.GL_LINE_STRIP)
				RenderUtils.glColor(color)

				targetPath.asSequence().filterNotNull().forEach { GL11.glVertex3d(it.xCoord - viewerPosX, it.yCoord - viewerPosY, it.zCoord - viewerPosZ) }

				GL11.glColor4d(1.0, 1.0, 1.0, 1.0)
				GL11.glEnd()

				GL11.glEnable(GL11.GL_DEPTH_TEST)
				GL11.glDisable(GL11.GL_LINE_SMOOTH)
				GL11.glDisable(GL11.GL_BLEND)
				GL11.glEnable(GL11.GL_TEXTURE_2D)
				GL11.glPopMatrix()
			}

			if (attackTimer.hasTimePassed(pathEspTime.get().toLong()))
			{
				targetPaths.clear()
				currentPath.clear()
			}
		}
	}

	private fun computePath(from: WVec3, to: WVec3): MutableList<WVec3>
	{
		var fromPos = from

		if (!canPassThrough(WBlockPos(fromPos.xCoord, fromPos.yCoord, fromPos.zCoord))) fromPos = fromPos.addVector(0.0, 1.0, 0.0)

		val pathfinder = PathFinder(fromPos, to)
		pathfinder.compute()

		var lastPath: WVec3? = null
		var lastEndPath: WVec3? = null
		val path = mutableListOf<WVec3>()
		val pathFinderPath = pathfinder.path

		pathFinderPath.forEachIndexed { i, currentPathFinderPath ->
			if (i == 0 || i == pathFinderPath.size - 1) // If the current path node is start or end node
			{
				if (lastPath != null) path.add(lastPath!!.addVector(0.5, 0.0, 0.5))
				path.add(currentPathFinderPath.addVector(0.5, 0.0, 0.5))
				lastEndPath = currentPathFinderPath
			}
			else
			{
				var canContinueSearching = true
				val maxDashDistance = maxDashDistanceValue.get().toFloat()
				val lastEndPathChecked = lastEndPath!!

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
								if (!PathFinder.checkPositionValidity(x, y, z, false))
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
					path.add(lastPath!!.addVector(0.5, 0.0, 0.5))
					lastEndPath = lastPath
				}
			}
			lastPath = currentPathFinderPath
		}
		return path
	}

	private val targets: MutableList<IEntityLivingBase>
		get() = mc.theWorld!!.loadedEntityList.asSequence().filter(classProvider::isEntityLivingBase).map(IEntity::asEntityLivingBase).filter { entity: IEntityLivingBase -> mc.thePlayer!!.getDistanceToEntityBox(entity) <= rangeValue.get() && isEnemy(entity, false) && entity.hurtTime <= hurtTimeValue.get() }.sortedBy { it.getDistanceToEntity(mc.thePlayer!!) * 1000 }.toMutableList()

	fun isTarget(entity: IEntity?): Boolean = currentTargets.isNotEmpty() && IntStream.range(0, if (currentTargets.size > maxTargetsValue.get()) maxTargetsValue.get() else currentTargets.size).anyMatch { i: Int -> currentTargets[i].isEntityEqual(entity) }

	override val tag: String
		get() = "${maxDashDistanceValue.get()}"

	companion object
	{
		private fun canBlock(): Boolean = mc.thePlayer != null && mc.thePlayer!!.heldItem != null && classProvider.isItemSword(mc.thePlayer!!.heldItem!!.item)

		private fun canPassThrough(pos: WBlockPos): Boolean
		{
			val state = getState(WBlockPos(pos.x, pos.y, pos.z))
			val block = state!!.block

			return classProvider.getMaterialEnum(MaterialType.AIR) == block.getMaterial(state) || classProvider.getMaterialEnum(MaterialType.PLANTS) == block.getMaterial(state) || classProvider.getMaterialEnum(MaterialType.VINE) == block.getMaterial(state) || classProvider.getBlockEnum(BlockType.LADDER) == block || classProvider.getBlockEnum(BlockType.WATER) == block || classProvider.getBlockEnum(BlockType.FLOWING_WATER) == block || classProvider.getBlockEnum(BlockType.WALL_SIGN) == block || classProvider.getBlockEnum(BlockType.STANDING_SIGN) == block
		}
	}
}
