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
import net.ccbluex.liquidbounce.value.RGBAColorValue
import net.ccbluex.liquidbounce.value.ValueGroup

@ModuleInfo(name = "HurtCam", description = "You can disables screen shaking when getting hurt and customizes hurt effect colors.", category = ModuleCategory.RENDER)
class HurtCam : Module()
{
    val noHurtCam = BoolValue("NoHurtCam", false)

    private val customHurtEffectGroup = ValueGroup("CustomHurtEffect")
    val customHurtEffectEnabledValue = BoolValue("CustomHurtEffectColor", false)
    val customHurtEffectColorValue = RGBAColorValue("Color", 255, 0, 0, 77, listOf("CustomHurtEffectColor-Red", "CustomHurtEffectColor-Green", "CustomHurtEffectColor-Blue", "CustomHurtEffectColor-Alpha"))

    init
    {
        customHurtEffectGroup.addAll(customHurtEffectEnabledValue, customHurtEffectColorValue)
    }

    override val tag: String?
        get() = if (noHurtCam.get()) "NoHurtCam" else null
}
