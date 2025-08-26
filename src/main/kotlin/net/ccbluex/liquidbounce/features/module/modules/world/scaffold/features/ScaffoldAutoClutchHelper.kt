package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold

object ScaffoldAutoClutchHelper : ToggleableConfigurable(ModuleScaffold, "AutoClutchHelper", true) {
    val scaffoldOnlyDuringCombat by boolean("ScaffoldOnlyDuringCombat", true)
}
