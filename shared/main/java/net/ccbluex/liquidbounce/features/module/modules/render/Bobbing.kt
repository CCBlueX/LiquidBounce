/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ValueGroup

@ModuleInfo(name = "Bobbing", description = "Modify the view bobbing effect multiplier.", category = ModuleCategory.RENDER)
class Bobbing : Module()
{
	val multiplierValue = FloatValue("Multiplier", .6F, 0F, 10F)
	val checkGroundValue = BoolValue("CheckGround", true)

	val cameraMultiplierGroup = ValueGroup("Camera")
	val cameraMultiplierYawValue = FloatValue("Yaw", .4F, 0F, 2F, "CameraYawMultiplier")
	val cameraMultiplierPitchValue = FloatValue("Pitch", .8F, 0F, 2F, "CameraPitchMultiplier")

	val cameraIncrementMultiplierGroup = ValueGroup("Increment")
	val cameraIncrementMultiplierYawValue = FloatValue("Yaw", 1F, 0.2F, 5F, "CameraYawIncrementMultiplier")
	val cameraIncrementMultiplierPitchValue = FloatValue("Pitch", 1F, 0.2F, 5F, "CameraPitchIncrementMultiplier")

	init
	{
		cameraIncrementMultiplierGroup.addAll(cameraIncrementMultiplierYawValue, cameraIncrementMultiplierPitchValue)
		cameraMultiplierGroup.addAll(cameraMultiplierYawValue, cameraMultiplierPitchValue, cameraIncrementMultiplierGroup)
	}

	override val tag: String
		get() = "${multiplierValue.get()}${if (checkGroundValue.get()) "" else ", Always"}"
}
