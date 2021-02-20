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

@ModuleInfo(name = "Bobbing", description = "Modify the view bobbing effect multiplier.", category = ModuleCategory.RENDER)
class Bobbing : Module()
{
	val multiplierValue = FloatValue("Multiplier", .6F, 0F, 10F)
	val cameraYawIncMultiplierValue = FloatValue("CameraYawIncrementMultiplier", 1F, 0.2F, 5F)
	val cameraPitchIncMultiplierValue = FloatValue("CameraPitchIncrementMultiplier", 1F, 0.2F, 5F)
	val cameraYawMultiplierValue = FloatValue("CameraYawMultiplier", .4F, 0F, 2F)
	val cameraPitchMultiplierValue = FloatValue("CameraPitchMultiplier", .8F, 0F, 2F)
	val checkGroundValue = BoolValue("CheckGround", true)

	override val tag: String
		get() = "${multiplierValue.get()}${if (checkGroundValue.get()) "" else ", Always"}"
}
