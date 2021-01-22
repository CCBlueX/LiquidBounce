package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@ModuleInfo(name = "Strafe", description = "Allows you to freely move in mid air without any friction.", category = ModuleCategory.MOVEMENT)
class Strafe : Module()
{
	private var strengthValue = FloatValue("Strength", 0.5F, 0F, 1F)
	private var noMoveStopValue = BoolValue("NoMoveStop", false)
	private var onGroundStrafeValue = BoolValue("OnGroundStrafe", false)
	private var allDirectionsJumpValue = BoolValue("AllDirectionsJump", false)

	private var wasDown: Boolean = false
	private var jump: Boolean = false

	@EventTarget
	fun onJump(event: JumpEvent)
	{
		if (jump) event.cancelEvent()
	}

	override fun onEnable()
	{
		wasDown = false
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.onGround && mc.gameSettings.keyBindJump.isKeyDown && allDirectionsJumpValue.get() && (thePlayer.movementInput.moveForward != 0F || thePlayer.movementInput.moveStrafe != 0F) && !(thePlayer.isInWater || thePlayer.isInLava || thePlayer.isOnLadder || thePlayer.isInWeb))
		{
			if (mc.gameSettings.keyBindJump.isKeyDown)
			{
				mc.gameSettings.keyBindJump.pressed = false
				wasDown = true
			}
			val yaw = thePlayer.rotationYaw
			thePlayer.rotationYaw = getMoveYaw(thePlayer)
			thePlayer.jump()
			thePlayer.rotationYaw = yaw
			jump = true
			if (wasDown)
			{
				mc.gameSettings.keyBindJump.pressed = true
				wasDown = false
			}
		} else
		{
			jump = false
		}
	}

	@EventTarget
	fun onStrafe(@Suppress("UNUSED_PARAMETER") event: StrafeEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val shotSpeed = sqrt((thePlayer.motionX * thePlayer.motionX) + (thePlayer.motionZ * thePlayer.motionZ))
		val speed = (shotSpeed * strengthValue.get())
		val motionX = (thePlayer.motionX * (1 - strengthValue.get()))
		val motionZ = (thePlayer.motionZ * (1 - strengthValue.get()))
		if (!(thePlayer.movementInput.moveForward != 0F || thePlayer.movementInput.moveStrafe != 0F))
		{
			if (noMoveStopValue.get())
			{
				thePlayer.motionX = 0.0
				thePlayer.motionZ = 0.0
			}
			return
		}
		if (!thePlayer.onGround || onGroundStrafeValue.get())
		{
			val yaw = getMoveYaw(thePlayer)
			thePlayer.motionX = (((-sin(Math.toRadians(yaw.toDouble())) * speed) + motionX))
			thePlayer.motionZ = (((cos(Math.toRadians(yaw.toDouble())) * speed) + motionZ))
		}
	}

	private fun getMoveYaw(thePlayer: IEntityPlayerSP): Float
	{
		var moveYaw = thePlayer.rotationYaw
		if (thePlayer.moveForward != 0F && thePlayer.moveStrafing == 0F)
		{
			moveYaw += if (thePlayer.moveForward > 0) 0 else 180
		} else if (thePlayer.moveForward != 0F && thePlayer.moveStrafing != 0F)
		{
			if (thePlayer.moveForward > 0)
			{
				moveYaw += if (thePlayer.moveStrafing > 0) -45 else 45
			} else
			{
				moveYaw -= if (thePlayer.moveStrafing > 0) -45 else 45
			}
			moveYaw += if (thePlayer.moveForward > 0) 0 else 180
		} else if (thePlayer.moveStrafing != 0F && thePlayer.moveForward == 0F)
		{
			moveYaw += if (thePlayer.moveStrafing > 0) -90 else 90
		}
		return moveYaw
	}
}
