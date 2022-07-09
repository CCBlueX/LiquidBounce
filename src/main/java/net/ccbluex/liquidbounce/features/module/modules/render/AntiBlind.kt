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

@ModuleInfo(name = "AntiBlind", description = "Cancels blindness effects.", category = ModuleCategory.RENDER)
class AntiBlind : Module()
{
    val blindnessEffect = BoolValue("Blindness", true)
    val confusionEffect = BoolValue("Confusion", true)
    val pumpkinEffect = BoolValue("Pumpkin", true)
    val fireEffect = BoolValue("Fire", false)

    override val tag: String
        get()
        {
            var tag = ""

            if (blindnessEffect.get()) tag += 'B'
            if (confusionEffect.get()) tag += 'C'
            if (pumpkinEffect.get()) tag += 'P'
            if (fireEffect.get()) tag += 'F'

            return tag
        }
}
