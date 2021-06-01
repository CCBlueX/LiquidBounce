package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object ModuleNoSwing: Module("NoSwing", Category.RENDER) {
    val serverSide by boolean("ServerSide", false)
}
