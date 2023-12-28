package net.ccbluex.liquidbounce.features.module.modules.player.autoplay

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.autoplay.modes.GommeDuels

/**
 *
 */
object ModuleAutoPlay : Module("AutoPlay", Category.PLAYER) {
    val modes = choices("Mode", GommeDuels, arrayOf(GommeDuels))
}
