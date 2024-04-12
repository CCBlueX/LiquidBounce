package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold

object ScaffoldAutoBlockFeature : ToggleableConfigurable(ModuleScaffold, "AutoBlock", true) {
    val alwaysHoldBlock by boolean("Always", false)
    val slotResetDelay by int("SlotResetDelay", 5, 0..40, "ticks")
}
