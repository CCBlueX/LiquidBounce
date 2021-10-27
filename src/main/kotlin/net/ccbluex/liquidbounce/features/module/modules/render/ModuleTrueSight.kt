package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object ModuleTrueSight : Module("TrueSight", Category.RENDER) {
    val barriers by boolean("Barriers", true)
    // val entities by boolean("Entities", true)
}