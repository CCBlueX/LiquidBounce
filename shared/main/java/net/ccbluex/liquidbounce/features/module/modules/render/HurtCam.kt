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
import net.ccbluex.liquidbounce.value.IntegerValue

@ModuleInfo(name = "HurtCam", description = "You can disables screen shaking when getting hurt and customizes hurt effect colors.", category = ModuleCategory.RENDER)
class HurtCam : Module()
{
	val noHurtCam = BoolValue("NoHurtCam", false)

	val customHurtEffect = BoolValue("CustomHurtEffectColor", false)
	val customHurtEffectR = IntegerValue("CustomHurtEffectColor-Red", 255, 0, 255)
	val customHurtEffectG = IntegerValue("CustomHurtEffectColor-Green", 0, 0, 255)
	val customHurtEffectB = IntegerValue("CustomHurtEffectColor-Blue", 0, 0, 255)
	val customHurtEffectAlpha = IntegerValue("CustomHurtEffectColor-Alpha", 77, 0, 255)
}
