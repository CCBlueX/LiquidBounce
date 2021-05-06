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

@ModuleInfo(name = "EatAnimation", description = "Customize eat/drink animation.", category = ModuleCategory.RENDER)
class EatAnimation : Module()
{
	val verticalShakeSpeedValue = FloatValue("VerticalShakeSpeed", 4F, 0.1F, 10F)
	val verticalShakeIntensityValue = FloatValue("VerticalShakeIntensity", 0.1F, 0.02F, 1F)

	val shakeStartTime = FloatValue("ShakeStartTime", 0.8F, 0.4F, 0.8F)

	val horizontalShakeValue = BoolValue("HorizontalShake", false)
	val horizontalShakeSpeedValue = FloatValue("HorizontalShakeSpeed", 8F, 0.1F, 10F)
	val horizontalShakeIntensityValue = FloatValue("HorizontalShakeIntensity", 0.05F, 0.02F, 1F)
}
