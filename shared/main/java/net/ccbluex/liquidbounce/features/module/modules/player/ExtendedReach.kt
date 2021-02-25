package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.MaterialType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoWeapon
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.RaycastUtils.EntityFilter
import net.ccbluex.liquidbounce.utils.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getState
import net.ccbluex.liquidbounce.utils.pathfinding.PathFinder
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@ModuleInfo(name = "ExtendedReach", description = "Upgraded combat and block reach over 100+ blocks.", category = ModuleCategory.PLAYER)
class ExtendedReach : Module()
{
	/**
	 * Options
	 */
	private val combatReach = FloatValue("CombatReach", 100F, 6F, 128F)

	@JvmField
	val buildReach = FloatValue("BuildReach", 100F, 6F, 128F)
	private val maxDashDistanceValue = IntegerValue("DashDistance", 5, 1, 10)

	private val pathEspValue = BoolValue("PathESP", true)
	private val pathEspTime = IntegerValue("PathESPTime", 1000, 100, 3000)

	private val colorRedValue = IntegerValue("PathESP-Red", 255, 0, 255)
	private val colorGreenValue = IntegerValue("PathESP-Green", 179, 0, 255)
	private val colorBlueValue = IntegerValue("PathESP-Blue", 72, 0, 255)

	private val colorRainbow = BoolValue("PathESP-Rainbow", false)
	private val rainbowSpeedValue = IntegerValue("PathESP-RainbowSpeed", 10, 1, 10)
	private val saturationValue = FloatValue("PathESP-RainbowHSB-Saturation", 1.0f, 0.0f, 1.0f)
	private val brightnessValue = FloatValue("PathESP-RainbowHSB-Brightness", 1.0f, 0.0f, 1.0f)

	/**
	 * Variables
	 */
	private val pathESPTimer = MSTimer()
	private var path = mutableListOf<WVec3>()

	override fun onEnable()
	{
		path.clear()
	}

	override fun onDisable()
	{
		path.clear()
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent?)
	{
		val renderManager = mc.renderManager
		val viewerPosX = renderManager.viewerPosX
		val viewerPosY = renderManager.viewerPosY
		val viewerPosZ = renderManager.viewerPosZ

		if (pathEspValue.get() && path.isNotEmpty() && !pathESPTimer.hasTimePassed(pathEspTime.get().toLong()))
		{
			val customColor = Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())
			val color = if (colorRainbow.get()) ColorUtils.rainbow(speed = rainbowSpeedValue.get(), saturation = saturationValue.get(), brightness = brightnessValue.get()) else customColor

			glPushMatrix()
			glDisable(GL_TEXTURE_2D)
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
			glEnable(GL_LINE_SMOOTH)
			glEnable(GL_BLEND)
			glDisable(GL_DEPTH_TEST)
			mc.entityRenderer.disableLightmap()

			glBegin(GL_LINE_STRIP)
			RenderUtils.glColor(color)

			for (path in path) glVertex3d(path.xCoord - viewerPosX, path.yCoord - viewerPosY, path.zCoord - viewerPosZ)

			glColor4d(1.0, 1.0, 1.0, 1.0)
			glEnd()

			glEnable(GL_DEPTH_TEST)
			glDisable(GL_LINE_SMOOTH)
			glDisable(GL_BLEND)
			glEnable(GL_TEXTURE_2D)
			glPopMatrix()
		}
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		val playerPosVec = WVec3(thePlayer.posX, thePlayer.posY, thePlayer.posZ)

		val packet = event.packet
		val networkManager = mc.netHandler.networkManager

		val provider = classProvider

		if (provider.isCPacketPlayerBlockPlacement(packet))
		{
			val blockPlacement = packet.asCPacketPlayerBlockPlacement()
			val pos = blockPlacement.position
			val stack = blockPlacement.stack
			val distance = sqrt(thePlayer.getDistanceSq(pos))

			if (distance > 6.0 && pos.y != -1 && (stack != null || provider.isBlockContainer(getState(pos)!!.block)))
			{
				val to = WVec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
				path = computePath(playerPosVec, to)

				// Travel to the target block.
				for (pathElm in path) networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
				pathESPTimer.reset()
				networkManager.sendPacketWithoutEvent(packet)

				// Go back to the home.
				path.reverse()
				for (pathElm in path) networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
				event.cancelEvent()
			}
		}

		if (provider.isCPacketPlayerDigging(packet))
		{
			val digging = packet.asCPacketPlayerDigging()
			val action = digging.status
			val pos = digging.position
			val face = digging.facing
			val distance = sqrt(thePlayer.getDistanceSq(pos))

			if (distance > 6 && action == ICPacketPlayerDigging.WAction.START_DESTROY_BLOCK)
			{
				val to = WVec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
				path = computePath(playerPosVec, to)

				// Travel to the target.
				for (pathElm in path) networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
				pathESPTimer.reset()
				val end = provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.STOP_DESTROY_BLOCK, pos, face)
				networkManager.sendPacketWithoutEvent(packet)
				networkManager.sendPacketWithoutEvent(end)

				// Go back to the home.
				path.reverse()
				for (pathElm in path) networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
				event.cancelEvent()
			}
			else if (action == ICPacketPlayerDigging.WAction.ABORT_DESTROY_BLOCK) event.cancelEvent()
		}
	}

	@EventTarget
	fun onMotion(event: MotionEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val netHandler = mc.netHandler
		val networkManager = netHandler.networkManager

		if (event.eventState == EventState.PRE)
		{
			val provider = classProvider

			val facedEntity = raycastEntity(theWorld, thePlayer, combatReach.get().toDouble(), object : EntityFilter
			{
				override fun canRaycast(entity: IEntity?): Boolean = provider.isEntityLivingBase(entity)
			})

			var targetEntity: IEntityLivingBase? = null
			val from = WVec3(thePlayer.posX, thePlayer.posY, thePlayer.posZ)

			if (mc.gameSettings.keyBindAttack.isKeyDown && isSelected(facedEntity, true) && thePlayer.getDistanceSqToEntity(facedEntity!!) >= 1) targetEntity = facedEntity.asEntityLivingBase()

			if (targetEntity != null)
			{
				val to = WVec3(targetEntity.posX, targetEntity.posY, targetEntity.posZ)

				// Compute the path
				path = computePath(from, to)

				// Travel to the target entity.
				for (pathElm in path) networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))

				pathESPTimer.reset()
				thePlayer.swingItem()

				// Make AutoWeapon compatible
				var sendAttack = true
				val attackPacket: IPacket = provider.createCPacketUseEntity(targetEntity, ICPacketUseEntity.WAction.ATTACK)

				val autoWeapon = LiquidBounce.moduleManager[AutoWeapon::class.java] as AutoWeapon
				if (autoWeapon.state)
				{
					val packetEvent = PacketEvent(attackPacket)
					autoWeapon.onPacket(packetEvent)

					if (packetEvent.isCancelled) sendAttack = false
				}

				if (sendAttack) netHandler.addToSendQueue(attackPacket)

				thePlayer.onCriticalHit(targetEntity)

				// Go back to the home.
				path.reverse()
				for (pathElm in path) networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
			}
		}
	}

	private fun computePath(topFrom: WVec3, to: WVec3): MutableList<WVec3>
	{
		var topFromPos = topFrom

		if (!canPassThrough(WBlockPos(topFromPos.xCoord, topFromPos.yCoord, topFromPos.zCoord))) topFromPos = topFromPos.addVector(0.0, 1.0, 0.0)

		val pathfinder = PathFinder(topFromPos, to)
		pathfinder.compute()

		var lastPos: WVec3? = null
		var lastDashPos: WVec3? = null
		val path = mutableListOf<WVec3>()
		val pathFinderPath = pathfinder.path

		pathFinderPath.forEachIndexed { i, pathElm ->
			if (i == 0 || i == pathFinderPath.size - 1)
			{
				if (lastPos != null) path.add(lastPos!!.addVector(0.5, 0.0, 0.5))

				path.add(pathElm.addVector(0.5, 0.0, 0.5))
				lastDashPos = pathElm
			}
			else
			{
				var stop = false
				val maxDashDistance = maxDashDistanceValue.get().toFloat()
				val lastDashPosChecked = lastDashPos!!

				if (pathElm.squareDistanceTo(lastDashPosChecked) > maxDashDistance * maxDashDistance) stop = true
				else
				{
					val minX = min(lastDashPosChecked.xCoord, pathElm.xCoord)
					val minY = min(lastDashPosChecked.yCoord, pathElm.yCoord)
					val minZ = min(lastDashPosChecked.zCoord, pathElm.zCoord)
					val maxX = max(lastDashPosChecked.xCoord, pathElm.xCoord)
					val maxY = max(lastDashPosChecked.yCoord, pathElm.yCoord)
					val maxZ = max(lastDashPosChecked.zCoord, pathElm.zCoord)

					var x = minX.toInt()
					coordsLoop@ while (x <= maxX)
					{
						var y = minY.toInt()
						while (y <= maxY)
						{
							var z = minZ.toInt()
							while (z <= maxZ)
							{
								if (!PathFinder.checkPositionValidity(x, y, z, false))
								{
									stop = true
									break@coordsLoop
								}
								z++
							}
							y++
						}
						x++
					}
				}

				if (stop)
				{
					path.add(lastPos!!.addVector(0.5, 0.0, 0.5))
					lastDashPos = lastPos
				}
			}

			lastPos = pathElm
		}
		return path
	}

	override val tag: String
		get() = "${maxDashDistanceValue.get()}"

	companion object
	{
		private fun canPassThrough(pos: WBlockPos): Boolean
		{
			val state = getState(WBlockPos(pos.x, pos.y, pos.z))
			val block = state!!.block

			val provider = classProvider

			return provider.getMaterialEnum(MaterialType.AIR) == block.getMaterial(state) || provider.getMaterialEnum(MaterialType.PLANTS) == block.getMaterial(state) || provider.getMaterialEnum(MaterialType.VINE) == block.getMaterial(state) || provider.getBlockEnum(BlockType.LADDER) == block || provider.getBlockEnum(BlockType.WATER) == block || provider.getBlockEnum(BlockType.FLOWING_WATER) == block || provider.getBlockEnum(BlockType.WALL_SIGN) == block || provider.getBlockEnum(BlockType.STANDING_SIGN) == block
		}
	}
}
