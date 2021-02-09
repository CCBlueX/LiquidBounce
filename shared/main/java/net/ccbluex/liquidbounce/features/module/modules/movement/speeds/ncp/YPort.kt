/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.api.enums.MaterialType
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.floor
import kotlin.math.hypot

class YPort : SpeedMode("YPort")
{
	private var moveSpeed = 0.2873
	private var level = 1
	private var lastDist = 0.0
	private var timerDelay = 0
	private var safeJump = false

	override fun onMotion(eventState: EventState)
	{
		val thePlayer = mc.thePlayer ?: return
		if (eventState != EventState.PRE) return

		if (!safeJump && !mc.gameSettings.keyBindJump.isKeyDown && !thePlayer.isOnLadder && !thePlayer.isInsideOfMaterial(classProvider.getMaterialEnum(MaterialType.WATER)) && !thePlayer.isInsideOfMaterial(
				classProvider.getMaterialEnum(
					MaterialType.LAVA
				)
			) && !thePlayer.isInWater && (!classProvider.isBlockAir(this.getBlock(-1.1)) && !classProvider.isBlockAir(this.getBlock(-1.1)) || !classProvider.isBlockAir(this.getBlock(-0.1)) && thePlayer.motionX != 0.0 && thePlayer.motionZ != 0.0 && !thePlayer.onGround && thePlayer.fallDistance < 3.0f && thePlayer.fallDistance > 0.05) && level == 3) thePlayer.motionY = -0.3994

		val xDist = thePlayer.posX - thePlayer.prevPosX
		val zDist = thePlayer.posZ - thePlayer.prevPosZ
		lastDist = hypot(xDist, zDist)

		if (!MovementUtils.isMoving(thePlayer)) safeJump = true else if (thePlayer.onGround) safeJump = false
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		timerDelay += 1
		timerDelay %= 5
		if (timerDelay != 0)
		{
			mc.timer.timerSpeed = 1f
		}
		else
		{
			if (MovementUtils.hasMotion(thePlayer)) mc.timer.timerSpeed = 32767f
			if (MovementUtils.hasMotion(thePlayer))
			{
				mc.timer.timerSpeed = 1.3f
				thePlayer.motionX *= 1.0199999809265137
				thePlayer.motionZ *= 1.0199999809265137
			}
		}
		if (thePlayer.onGround && MovementUtils.hasMotion(thePlayer)) level = 2
		if (round(thePlayer.posY - thePlayer.posY.toInt()) == round(0.138))
		{
			thePlayer.motionY -= 0.08
			event.y = event.y - 0.09316090325960147
			thePlayer.posY -= 0.09316090325960147
		}
		if (level == 1 && (thePlayer.moveForward != 0.0f || thePlayer.moveStrafing != 0.0f))
		{
			level = 2
			moveSpeed = 1.38 * baseMoveSpeed - 0.01
		}
		else if (level == 2)
		{
			level = 3
			thePlayer.motionY = 0.399399995803833
			event.y = 0.399399995803833
			moveSpeed *= 2.149
		}
		else if (level == 3)
		{
			level = 4
			val difference = 0.66 * (lastDist - baseMoveSpeed)
			moveSpeed = lastDist - difference
		}
		else
		{
			if ((mc.theWorld ?: return).getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox.offset(0.0, thePlayer.motionY, 0.0)).isNotEmpty() || thePlayer.isCollidedVertically) level = 1
			moveSpeed = lastDist - lastDist / 159.0
		}
		moveSpeed = moveSpeed.coerceAtLeast(baseMoveSpeed)
		var forward: Float = thePlayer.movementInput.moveForward
		var strafe: Float = thePlayer.movementInput.moveStrafe
		var yaw = thePlayer.rotationYaw
		if (forward == 0f && strafe == 0f) event.zeroXZ()
		else if (forward != 0f)
		{
			if (strafe >= 1f)
			{
				yaw += if (forward > 0f) -45 else 45
				strafe = 0f
			}
			else if (strafe <= -1.0f)
			{
				yaw += if (forward > 0f) 45 else -45
				strafe = 0f
			}
			if (forward > 0f) forward = 1f else if (forward < 0f) forward = -1f
		}

		val yawRadians = WMathHelper.toRadians(yaw + 90.0f)
		val mx = functions.cos(yawRadians)
		val mz = functions.sin(yawRadians)
		event.x = forward * moveSpeed * mx + strafe * moveSpeed * mz
		event.z = forward * moveSpeed * mz - strafe * moveSpeed * mx

		thePlayer.stepHeight = 0.5f
		if (forward == 0f && strafe == 0f) event.zeroXZ()
	}

	private val baseMoveSpeed: Double
		get()
		{
			val thePlayer = mc.thePlayer!!

			var baseSpeed = 0.2873

			if (thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.MOVE_SPEED)))
			{
				val amplifier: Int = MovementUtils.getSpeedEffectAmplifier(thePlayer)
				baseSpeed *= 1.0 + 0.2 * amplifier
			}
			return baseSpeed
		}

	private fun getBlock(axisAlignedBB: IAxisAlignedBB): IBlock?
	{
		for (x in floor(axisAlignedBB.minX).toInt() until floor(axisAlignedBB.maxX).toInt() + 1) for (z in floor(axisAlignedBB.minZ).toInt() until floor(axisAlignedBB.maxZ).toInt() + 1) return (mc.theWorld ?: return null).getBlockState(
			WBlockPos(
				x, axisAlignedBB.minY.toInt(), z
			)
		).block
		return null
	}

	private fun getBlock(offset: Double): IBlock?
	{
		return this.getBlock((mc.thePlayer ?: return null).entityBoundingBox.offset(0.0, offset, 0.0))
	}

	private fun round(value: Double): Double
	{
		var bd = BigDecimal(value)
		bd = bd.setScale(3, RoundingMode.HALF_UP)
		return bd.toDouble()
	}
}
