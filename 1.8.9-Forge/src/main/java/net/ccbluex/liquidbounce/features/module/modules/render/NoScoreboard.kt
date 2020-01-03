package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "NoScoreboard", description = "Disables the scoreboard.", category = ModuleCategory.RENDER)
class NoScoreboard : Module() {
    public val disable = BoolValue("Disable", true)
    public val yOffset = FloatValue("YOffset", 0.0f, -1.0f, 1.0f);
}
