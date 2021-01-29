/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.hypot

class NCPTimerBHop : SpeedMode("SNCPBHop")
{
	private var step = 1
	private var moveSpeed = 0.2873
	private var lastDist = 0.0
	private var timerDelay = 0

	override fun onEnable()
	{
		mc.timer.timerSpeed = 1f
		lastDist = 0.0
		moveSpeed = 0.0
		step = 4
	}

	override fun onDisable()
	{
		mc.timer.timerSpeed = 1f
		moveSpeed = baseMoveSpeed
		step = 0
	}

	override fun onMotion(eventState: EventState)
	{
		val thePlayer = mc.thePlayer ?: return
		if (eventState != EventState.PRE) return

		val xDist = thePlayer.posX - thePlayer.prevPosX
		val zDist = thePlayer.posZ - thePlayer.prevPosZ
		lastDist = hypot(xDist, zDist)
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		++timerDelay
		timerDelay %= 5
		if (timerDelay != 0)
		{
			mc.timer.timerSpeed = 1f
		} else
		{
			if (MovementUtils.isMoving) mc.timer.timerSpeed = 32767f
			if (MovementUtils.isMoving)
			{
				mc.timer.timerSpeed = 1.3f
				thePlayer.motionX *= 1.0199999809265137
				thePlayer.motionZ *= 1.0199999809265137
			}
		}
		if (thePlayer.onGround && MovementUtils.isMoving) step = 2
		if (round(thePlayer.posY - thePlayer.posY.toInt().toDouble()) == round(0.138))
		{
			thePlayer.motionY -= 0.08
			event.y = event.y - 0.09316090325960147
			thePlayer.posY -= 0.09316090325960147
		}
		if (step == 1 && (thePlayer.moveForward != 0.0f || thePlayer.moveStrafing != 0.0f))
		{
			step = 2
			moveSpeed = 1.35 * baseMoveSpeed - 0.01
		} else if (step == 2)
		{
			step = 3
			thePlayer.motionY = 0.399399995803833
			event.y = 0.399399995803833
			moveSpeed *= 2.149
		} else if (step == 3)
		{
			step = 4
			val difference = 0.66 * (lastDist - baseMoveSpeed)
			moveSpeed = lastDist - difference
		} else if (step == 88)
		{
			moveSpeed = baseMoveSpeed
			lastDist = 0.0
			step = 89
		} else if (step == 89)
		{
			if (theWorld.getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox.offset(0.0, thePlayer.motionY, 0.0)).isNotEmpty() || thePlayer.isCollidedVertically) step = 1
			lastDist = 0.0
			moveSpeed = baseMoveSpeed
			return
		} else
		{
			if (theWorld.getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox.offset(0.0, thePlayer.motionY, 0.0)).isNotEmpty() || thePlayer.isCollidedVertically)
			{
				moveSpeed = baseMoveSpeed
				lastDist = 0.0
				step = 88
				return
			}
			moveSpeed = lastDist - lastDist / 159.0
		}
		moveSpeed = moveSpeed.coerceAtLeast(baseMoveSpeed)

		val movementInput = thePlayer.movementInput
		var forward: Float = movementInput.moveForward
		var strafe: Float = movementInput.moveStrafe
		var yaw = thePlayer.rotationYaw
		if (forward == 0.0f && strafe == 0.0f) event.zeroXZ() else if (forward != 0.0f)
		{
			if (strafe >= 1.0f)
			{
				yaw += if (forward > 0.0f) -45 else 45
				strafe = 0.0f
			} else if (strafe <= -1.0f)
			{
				yaw += if (forward > 0.0f) 45 else -45
				strafe = 0.0f
			}
			if (forward > 0.0f)
			{
				forward = 1.0f
			} else if (forward < 0.0f)
			{
				forward = -1.0f
			}
		}

		val yawRadians = WMathHelper.toRadians(yaw + 90.0f)
		val mx2 = functions.cos(yawRadians)
		val mz2 = functions.sin(yawRadians)
		event.x = forward.toDouble() * moveSpeed * mx2 + strafe.toDouble() * moveSpeed * mz2
		event.z = forward.toDouble() * moveSpeed * mz2 - strafe.toDouble() * moveSpeed * mx2
		thePlayer.stepHeight = 0.5f
		if (forward == 0.0f && strafe == 0.0f) event.zeroXZ()
	}

	private val baseMoveSpeed: Double
		get()
		{
			var baseSpeed = 0.2873
			if (mc.thePlayer!!.isPotionActive(classProvider.getPotionEnum(PotionType.MOVE_SPEED))) baseSpeed *= 1.0 + 0.2 * (mc.thePlayer!!.getActivePotionEffect(classProvider.getPotionEnum(PotionType.MOVE_SPEED))!!.amplifier + 1)
			return baseSpeed
		}

	private fun round(value: Double): Double
	{
		var bigDecimal = BigDecimal(value)
		bigDecimal = bigDecimal.setScale(3, RoundingMode.HALF_UP)
		return bigDecimal.toDouble()
	}
}
