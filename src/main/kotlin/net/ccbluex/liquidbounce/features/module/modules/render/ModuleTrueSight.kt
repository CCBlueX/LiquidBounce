package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

/**
 * TrueSight module
 *
 * Allows you to see invisible objects and entities.
 */

object ModuleTrueSight : Module("TrueSight", Category.RENDER) {
    val barriers by boolean("Barriers", true)
    val entities by boolean("Entities", true)
}
