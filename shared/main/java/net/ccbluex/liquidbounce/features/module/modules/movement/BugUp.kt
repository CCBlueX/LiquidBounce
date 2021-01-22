/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

@ModuleInfo(name = "BugUp", description = "Automatically setbacks you after falling a certain distance.", category = ModuleCategory.MOVEMENT)
class BugUp : Module()
{
	private val modeValue = ListValue("Mode", arrayOf("TeleportBack", "FlyFlag", "OnGroundSpoof", "MotionTeleport-Flag"), "FlyFlag")
	private val maxFallDistance = IntegerValue("MaxFallDistance", 10, 2, 255)
	private val maxVoidFallDistance = IntegerValue("MaxVoidFallDistance", 3, 1, 255)
	private val maxDistanceWithoutGround = FloatValue("MaxDistanceToSetback", 2.5f, 1f, 30f)

	private val flagMethodValue = ListValue("FlyFlag-Method", arrayOf("Packet", "Motion"), "Packet")
	private val flagYPacket = FloatValue("FlyFlag-PacketY", 5f, -10f, 10f)
	private val flagYMotion = FloatValue("FlyFlag-MotionY", 1f, -10f, 10f)

	private val onlyCatchVoid = BoolValue("OnlyVoid", true)
	private val indicator = BoolValue("Indicator", true)

	private var detectedLocation: WBlockPos? = null
	private var lastFound = 0F
	private var prevX = 0.0
	private var prevY = 0.0
	private var prevZ = 0.0

	private val flagTimer = TickTimer()
	private var tryingFlag = false

	override fun onDisable()
	{
		prevX = 0.0
		prevY = 0.0
		prevZ = 0.0
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		detectedLocation = null

		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.onGround && !classProvider.isBlockAir(
				BlockUtils.getBlock(
					WBlockPos(
						thePlayer.posX, thePlayer.posY - 1.0, thePlayer.posZ
					)
				)
			)
		)
		{
			prevX = thePlayer.prevPosX
			prevY = thePlayer.prevPosY
			prevZ = thePlayer.prevPosZ
		}

		if (!thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater)
		{
			val fallingPlayer = FallingPlayer(
				thePlayer.posX, thePlayer.posY, thePlayer.posZ, thePlayer.motionX, thePlayer.motionY, thePlayer.motionZ, thePlayer.rotationYaw, thePlayer.moveStrafing, thePlayer.moveForward
			)

			detectedLocation = fallingPlayer.findCollision(60)?.pos

			if (detectedLocation != null && (onlyCatchVoid.get() || abs(thePlayer.posY - detectedLocation!!.y) + thePlayer.fallDistance <= maxFallDistance.get())) lastFound = thePlayer.fallDistance

			if (detectedLocation == null && thePlayer.fallDistance <= maxVoidFallDistance.get()) lastFound = thePlayer.fallDistance

			if (thePlayer.fallDistance - lastFound > maxDistanceWithoutGround.get())
			{
				val mode = modeValue.get()

				when (mode.toLowerCase())
				{
					"teleportback" ->
					{
						thePlayer.setPositionAndUpdate(prevX, prevY, prevZ)
						thePlayer.fallDistance = 0F
						thePlayer.motionY = 0.0
					}

					"flyflag" ->
					{
						tryingFlag = true //						thePlayer.motionY += 0.1
						//						thePlayer.fallDistance = 0F
					}

					"ongroundspoof" -> mc.netHandler.addToSendQueue(classProvider.createCPacketPlayer(true))

					"motionteleport-flag" ->
					{
						thePlayer.setPositionAndUpdate(thePlayer.posX, thePlayer.posY + 1f, thePlayer.posZ)
						mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
						thePlayer.motionY = 0.1

						MovementUtils.strafe()
						thePlayer.fallDistance = 0f
					}
				}
			}
		}

		if (tryingFlag)
		{
			if (!flagTimer.hasTimePassed(10))
			{
				when (flagMethodValue.get().toLowerCase())
				{
					"motion" ->
					{
						thePlayer.motionY = flagYMotion.get().toDouble()
						thePlayer.fallDistance = 0F
					}

					"packet" -> mc.netHandler.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(thePlayer.posX, thePlayer.posY + flagYPacket.get(), thePlayer.posZ, false))
				}
			} else
			{
				tryingFlag = false
				flagTimer.reset()
			}
			flagTimer.update()
		} else flagTimer.reset()
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (detectedLocation == null || !indicator.get() || thePlayer.fallDistance + (thePlayer.posY - (detectedLocation!!.y + 1)) < 3) return

		val x = detectedLocation!!.x
		val y = detectedLocation!!.y
		val z = detectedLocation!!.z

		val renderManager = mc.renderManager

		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
		GL11.glEnable(GL11.GL_BLEND)
		GL11.glLineWidth(2f)
		GL11.glDisable(GL11.GL_TEXTURE_2D)
		GL11.glDisable(GL11.GL_DEPTH_TEST)
		GL11.glDepthMask(false)

		RenderUtils.glColor(Color(255, 0, 0, 90))
		RenderUtils.drawFilledBox(
			classProvider.createAxisAlignedBB(
				x - renderManager.renderPosX, y + 1 - renderManager.renderPosY, z - renderManager.renderPosZ, x - renderManager.renderPosX + 1.0, y + 1.2 - renderManager.renderPosY, z - renderManager.renderPosZ + 1.0
			)
		)

		GL11.glEnable(GL11.GL_TEXTURE_2D)
		GL11.glEnable(GL11.GL_DEPTH_TEST)
		GL11.glDepthMask(true)
		GL11.glDisable(GL11.GL_BLEND)

		val fallDist = floor(thePlayer.fallDistance + (thePlayer.posY - (y + 0.5))).toInt()

		RenderUtils.renderNameTag("${fallDist}m (~${max(0, fallDist - 3)} damage)", x + 0.5, y + 1.7, z + 0.5)

		classProvider.getGlStateManager().resetColor()
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val packet = event.packet
		if (classProvider.isSPacketPlayerPosLook(packet))
		{
			val mode = modeValue.get()

			if (mode.equals("FlyFlag", true) && tryingFlag)
			{

				// Automatically stop flagging after teleported back.
				ClientUtils.displayChatMessage("\u00a78[\u00a7c\u00a7lBugUp\u00a78] \u00a7cTeleported.")
				tryingFlag = false
			}
		}
	}

	override val tag: String
		get() = modeValue.get()
}
