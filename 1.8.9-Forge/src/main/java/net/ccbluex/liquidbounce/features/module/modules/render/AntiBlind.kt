package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "AntiBlind", description = "Cancels blindness effects.", category = ModuleCategory.RENDER)
class AntiBlind : Module() {
    val confusionEffect = BoolValue("Confusion", true)
    val pumpkinEffect = BoolValue("Pumpkin", true)
    val fireEffect = BoolValue("Fire", false)
}