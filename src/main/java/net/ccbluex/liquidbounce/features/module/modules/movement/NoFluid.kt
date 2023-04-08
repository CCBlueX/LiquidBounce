package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue

@ModuleInfo(name = "NoFluid", description = "Ignore fluids like water and lava.", category = ModuleCategory.MOVEMENT)
object NoFluid : Module() {

    val waterValue = BoolValue("Water", true)
    val lavaValue = BoolValue("Lava", true)
}