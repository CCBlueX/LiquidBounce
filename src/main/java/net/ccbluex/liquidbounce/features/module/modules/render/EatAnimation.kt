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

@ModuleInfo(name = "EatAnimation", description = "Customize eat/drink animation.", category = ModuleCategory.RENDER)
class EatAnimation : Module()
{
    private val verticalGroup = ValueGroup("Vertical")
    val verticalSpeedValue = FloatValue("Speed", 4F, 0.1F, 10F, "VerticalShakeSpeed")
    val verticalIntensityValue = FloatValue("Intensity", 0.1F, 0.02F, 1F, "VerticalShakeIntensity")

    private val horizontalGroup = ValueGroup("Horizontal")
    val horizontalEnabledValue = BoolValue("Enabled", false, "HorizontalShake")
    val horizontalSpeedValue = FloatValue("Speed", 8F, 0.1F, 10F, "HorizontalShakeSpeed")
    val horizontalIntensityValue = FloatValue("Intensity", 0.05F, 0.02F, 1F, "HorizontalShakeIntensity")

    val shakeStartTime = FloatValue("ShakeStartTime", 0.8F, 0.4F, 0.8F)

    init
    {
        verticalGroup.addAll(verticalSpeedValue, verticalIntensityValue)
        horizontalGroup.addAll(horizontalEnabledValue, horizontalSpeedValue, horizontalIntensityValue)
    }
}
