package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.value.BoolValue

object NoFluid : Module("NoFluid", Category.MOVEMENT) {

    val water by BoolValue("Water", true)
    val lava by BoolValue("Lava", true)
}