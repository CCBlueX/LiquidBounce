package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.MaterialType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
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
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import org.lwjgl.opengl.GL11
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
	private val pathEspValue = BoolValue("PathESP", true)
	private val pathEspTimeValue = IntegerValue("PathESPTime", 1000, 100, 3000)
	private val maxDashDistanceValue = IntegerValue("DashDistance", 5, 1, 10)

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
		val thePlayer = mc.thePlayer ?: return
		if (pathEspValue.get() && path.isNotEmpty() && !pathESPTimer.hasTimePassed(pathEspTimeValue.get().toLong())) for (pos in path) drawPath(thePlayer, pos)
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		val playerPosVec = WVec3(thePlayer.posX, thePlayer.posY, thePlayer.posZ)

		val packet = event.packet
		val networkManager = mc.netHandler.networkManager

		if (classProvider.isCPacketPlayerBlockPlacement(packet))
		{
			val blockPlacement = packet.asCPacketPlayerBlockPlacement()
			val pos = blockPlacement.position
			val stack = blockPlacement.stack
			val distance = sqrt(thePlayer.getDistanceSq(pos))

			if (distance > 6.0 && pos.y != -1 && (stack != null || classProvider.isBlockContainer(getState(pos)!!.block)))
			{
				val to = WVec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
				path = computePath(playerPosVec, to)

				// Travel to the target block.
				for (pathElm in path) networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
				pathESPTimer.reset()
				networkManager.sendPacketWithoutEvent(packet)

				// Go back to the home.
				path.reverse()
				for (pathElm in path) networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
				event.cancelEvent()
			}
		}
		if (classProvider.isCPacketPlayerDigging(packet))
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
				for (pathElm in path) networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
				pathESPTimer.reset()
				val end = classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.STOP_DESTROY_BLOCK, pos, face)
				networkManager.sendPacketWithoutEvent(packet)
				networkManager.sendPacketWithoutEvent(end)

				// Go back to the home.
				path.reverse()
				for (pathElm in path) networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
				event.cancelEvent()
			}
			else if (action == ICPacketPlayerDigging.WAction.ABORT_DESTROY_BLOCK) event.cancelEvent()
		}
	}

	@EventTarget
	fun onMotion(event: MotionEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val netHandler = mc.netHandler
		val networkManager = netHandler.networkManager

		if (event.eventState == EventState.PRE)
		{
			val facedEntity = raycastEntity(combatReach.get().toDouble(), object : EntityFilter
			{
				override fun canRaycast(entity: IEntity?): Boolean = classProvider.isEntityLivingBase(entity)
			})

			var targetEntity: IEntityLivingBase? = null

			if (mc.gameSettings.keyBindAttack.isKeyDown && isSelected(facedEntity, true) && thePlayer.getDistanceSqToEntity(facedEntity!!) >= 1) targetEntity = facedEntity.asEntityLivingBase()
			if (targetEntity != null)
			{
				val from = WVec3(thePlayer.posX, thePlayer.posY, thePlayer.posZ)
				val to = WVec3(targetEntity.posX, targetEntity.posY, targetEntity.posZ)
				path = computePath(from, to)

				// Travel to the target entity.

				for (pathElm in path) networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
				pathESPTimer.reset()
				thePlayer.swingItem()

				// Make AutoWeapon compatible
				var sendAttack = true
				val attackPacket: IPacket = classProvider.createCPacketUseEntity(targetEntity, ICPacketUseEntity.WAction.ATTACK)
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
				for (pathElm in path) networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(pathElm.xCoord, pathElm.yCoord, pathElm.zCoord, true))
			}
		}
	}

	private fun computePath(topFrom: WVec3, to: WVec3): MutableList<WVec3>
	{
		var topFromPos = topFrom

		if (!canPassThrough(WBlockPos(topFromPos.xCoord, topFromPos.yCoord, topFromPos.zCoord))) topFromPos = topFromPos.addVector(0.0, 1.0, 0.0)

		val pathfinder = PathFinder(topFromPos, to)
		pathfinder.compute()

		var lastLoc: WVec3? = null
		var lastDashLoc: WVec3? = null
		val path = mutableListOf<WVec3>()
		val pathFinderPath = pathfinder.path

		pathFinderPath.forEachIndexed { i, pathElm ->
			if (i == 0 || i == pathFinderPath.size - 1)
			{
				if (lastLoc != null) path.add(lastLoc!!.addVector(0.5, 0.0, 0.5))
				path.add(pathElm.addVector(0.5, 0.0, 0.5))
				lastDashLoc = pathElm
			}
			else
			{
				var stop = false
				val maxDashDistance = maxDashDistanceValue.get().toFloat()
				val lastDashLocChecked = lastDashLoc!!

				if (pathElm.squareDistanceTo(lastDashLocChecked) > maxDashDistance * maxDashDistance) stop = true
				else
				{
					val minX = min(lastDashLocChecked.xCoord, pathElm.xCoord)
					val minY = min(lastDashLocChecked.yCoord, pathElm.yCoord)
					val minZ = min(lastDashLocChecked.zCoord, pathElm.zCoord)
					val maxX = max(lastDashLocChecked.xCoord, pathElm.xCoord)
					val maxY = max(lastDashLocChecked.yCoord, pathElm.yCoord)
					val maxZ = max(lastDashLocChecked.zCoord, pathElm.zCoord)

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
					path.add(lastLoc!!.addVector(0.5, 0.0, 0.5))
					lastDashLoc = lastLoc
				}
			}

			lastLoc = pathElm
		}
		return path
	}

	override val tag: String
		get() = java.lang.String.valueOf(maxDashDistanceValue.get())

	companion object
	{
		private fun canPassThrough(pos: WBlockPos): Boolean
		{
			val state = getState(WBlockPos(pos.x, pos.y, pos.z))
			val block = state!!.block
			return classProvider.getMaterialEnum(MaterialType.AIR) == block.getMaterial(state) || classProvider.getMaterialEnum(MaterialType.PLANTS) == block.getMaterial(state) || classProvider.getMaterialEnum(MaterialType.VINE) == block.getMaterial(
				state
			) || classProvider.getBlockEnum(BlockType.LADDER) == block || classProvider.getBlockEnum(BlockType.WATER) == block || classProvider.getBlockEnum(BlockType.FLOWING_WATER) == block || classProvider.getBlockEnum(BlockType.WALL_SIGN) == block || classProvider.getBlockEnum(
				BlockType.STANDING_SIGN
			) == block
		}

		private fun drawPath(thePlayer: IEntityPlayerSP, vec: WVec3)
		{
			val height = thePlayer.eyeHeight.toDouble()

			val renderManager = mc.renderManager
			val x = vec.xCoord - renderManager.renderPosX
			val y = vec.yCoord - renderManager.renderPosY
			val z = vec.zCoord - renderManager.renderPosZ

			// RenderUtils.pre3D()
			GL11.glPushMatrix()
			GL11.glEnable(GL11.GL_BLEND)
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
			GL11.glShadeModel(GL11.GL_SMOOTH)
			GL11.glDisable(GL11.GL_TEXTURE_2D)
			GL11.glEnable(GL11.GL_LINE_SMOOTH)
			GL11.glDisable(GL11.GL_DEPTH_TEST)
			GL11.glDisable(GL11.GL_LIGHTING)
			GL11.glDepthMask(false)
			GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
			GL11.glLoadIdentity()

			mc.entityRenderer.setupCameraTransform(mc.timer.renderPartialTicks, 2)

			val colors = arrayOf(
				Color.black, Color.white
			)
			val width = 0.3

			for (i in 0..1)
			{
				RenderUtils.glColor(colors[i])
				GL11.glLineWidth((3 - (i shl 1)).toFloat())
				GL11.glBegin(GL11.GL_LINE_STRIP)
				GL11.glVertex3d(x - width, y, z - width)
				GL11.glVertex3d(x - width, y, z - width)
				GL11.glVertex3d(x - width, y + height, z - width)
				GL11.glVertex3d(x + width, y + height, z - width)
				GL11.glVertex3d(x + width, y, z - width)
				GL11.glVertex3d(x - width, y, z - width)
				GL11.glVertex3d(x - width, y, z + width)
				GL11.glEnd()
				GL11.glBegin(GL11.GL_LINE_STRIP)
				GL11.glVertex3d(x + width, y, z + width)
				GL11.glVertex3d(x + width, y + height, z + width)
				GL11.glVertex3d(x - width, y + height, z + width)
				GL11.glVertex3d(x - width, y, z + width)
				GL11.glVertex3d(x + width, y, z + width)
				GL11.glVertex3d(x + width, y, z - width)
				GL11.glEnd()
				GL11.glBegin(GL11.GL_LINE_STRIP)
				GL11.glVertex3d(x + width, y + height, z + width)
				GL11.glVertex3d(x + width, y + height, z - width)
				GL11.glEnd()
				GL11.glBegin(GL11.GL_LINE_STRIP)
				GL11.glVertex3d(x - width, y + height, z + width)
				GL11.glVertex3d(x - width, y + height, z - width)
				GL11.glEnd()
			}

			// RenderUtils.post3D()
			GL11.glDepthMask(true)
			GL11.glEnable(GL11.GL_DEPTH_TEST)
			GL11.glDisable(GL11.GL_LINE_SMOOTH)
			GL11.glEnable(GL11.GL_TEXTURE_2D)
			GL11.glDisable(GL11.GL_BLEND)
			GL11.glPopMatrix()
			GL11.glColor4f(1f, 1f, 1f, 1f)
		}
	}
}
