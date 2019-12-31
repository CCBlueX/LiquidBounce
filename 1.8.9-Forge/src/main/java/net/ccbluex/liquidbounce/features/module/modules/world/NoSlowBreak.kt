package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue

@ModuleInfo(name = "NoSlowBreak", description = "", category = ModuleCategory.WORLD)
class NoSlowBreak : Module() {

    val airValue = BoolValue("Air", true)
    val waterValue = BoolValue("Water", false)

}