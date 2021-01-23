/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.*

class OldNCPBHop : SpeedMode("OldNCPBHop")
{
	private var level = 1
	private var moveSpeed = 0.2873
	private var lastDist = 0.0
	private var timerDelay = 0
	override fun onEnable()
	{
		val thePlayer = mc.thePlayer ?: return

		mc.timer.timerSpeed = 1f
		level = if ((mc.theWorld ?: return).getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox.offset(0.0, thePlayer.motionY, 0.0)).isNotEmpty() || thePlayer.isCollidedVertically) 1 else 4
	}

	override fun onDisable()
	{
		mc.timer.timerSpeed = 1f
		moveSpeed = baseMoveSpeed
		level = 0
	}

	override fun onMotion()
	{
		val thePlayer = mc.thePlayer ?: return

		val xDist = thePlayer.posX - thePlayer.prevPosX
		val zDist = thePlayer.posZ - thePlayer.prevPosZ
		lastDist = sqrt(xDist * xDist + zDist * zDist)
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
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
		if (thePlayer.onGround && MovementUtils.isMoving) level = 2
		if (round(thePlayer.posY - thePlayer.posY.roundToInt().toDouble()) == round(0.138))
		{
			thePlayer.motionY -= 0.08
			event.y = event.y - 0.09316090325960147
			thePlayer.posY -= 0.09316090325960147
		}
		if (level == 1 && (thePlayer.moveForward != 0.0f || thePlayer.moveStrafing != 0.0f))
		{
			level = 2
			moveSpeed = 1.35 * baseMoveSpeed - 0.01
		} else if (level == 2)
		{
			level = 3
			thePlayer.motionY = 0.399399995803833
			event.y = 0.399399995803833
			moveSpeed *= 2.149
		} else if (level == 3)
		{
			level = 4
			val difference = 0.66 * (lastDist - baseMoveSpeed)
			moveSpeed = lastDist - difference
		} else
		{
			if (mc.theWorld!!.getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox.offset(0.0, thePlayer.motionY, 0.0)).isNotEmpty() || thePlayer.isCollidedVertically) level = 1
			moveSpeed = lastDist - lastDist / 159.0
		}
		moveSpeed = max(moveSpeed, baseMoveSpeed)
		val movementInput = thePlayer.movementInput
		var forward: Float = movementInput.moveForward
		var strafe: Float = movementInput.moveStrafe
		var yaw = thePlayer.rotationYaw
		if (forward == 0.0f && strafe == 0.0f)
		{
			event.x = 0.0
			event.z = 0.0
		} else if (forward != 0.0f)
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
		event.x = forward * moveSpeed * mx2 + strafe * moveSpeed * mz2
		event.z = forward * moveSpeed * mz2 - strafe * moveSpeed * mx2
		thePlayer.stepHeight = 0.5f
		if (forward == 0.0f && strafe == 0.0f)
		{
			event.x = 0.0
			event.z = 0.0
		}
	}

	private val baseMoveSpeed: Double
		get()
		{
			var baseSpeed = 0.2873
			if (mc.thePlayer!!.isPotionActive(classProvider.getPotionEnum(PotionType.MOVE_SPEED))) baseSpeed *= 1.0 + 0.2 * (mc.thePlayer!!.getActivePotionEffect(classProvider.getPotionEnum(PotionType.MOVE_SPEED)))!!.amplifier + 1
			return baseSpeed
		}

	private fun round(value: Double): Double
	{
		var bigDecimal = BigDecimal(value)
		bigDecimal = bigDecimal.setScale(3, RoundingMode.HALF_UP)
		return bigDecimal.toDouble()
	}
}
