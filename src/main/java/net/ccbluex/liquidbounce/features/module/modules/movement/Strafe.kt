package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.moveDirectionDegrees
import net.ccbluex.liquidbounce.utils.extensions.zeroXZ
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import kotlin.math.hypot

@ModuleInfo(name = "Strafe", description = "Allows you to freely move in mid air without any friction.", category = ModuleCategory.MOVEMENT)
class Strafe : Module()
{
	private var strengthValue = FloatValue("Strength", 0.5F, 0F, 1F)
	private var noMoveStopValue = BoolValue("NoMoveStop", false)
	private var onGroundStrafeValue = BoolValue("OnGroundStrafe", false)

	@EventTarget
	fun onStrafe(@Suppress("UNUSED_PARAMETER") event: StrafeEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val strength = strengthValue.get()

		val speed = hypot(thePlayer.motionX, thePlayer.motionZ) * strength
		val motionX = thePlayer.motionX * (1 - strength)
		val motionZ = thePlayer.motionZ * (1 - strength)

		if (!thePlayer.isMoving)
		{
			if (noMoveStopValue.get()) thePlayer.zeroXZ()
			return
		}

		if (!thePlayer.onGround || onGroundStrafeValue.get())
		{
			val yaw = WMathHelper.toRadians(thePlayer.moveDirectionDegrees)
			thePlayer.motionX = -functions.sin(yaw) * speed + motionX
			thePlayer.motionZ = functions.cos(yaw) * speed + motionZ
		}
	}
}
