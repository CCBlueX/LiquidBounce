package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold

object ScaffoldAutoClutchHelper : ToggleableConfigurable(ModuleScaffold, "AutoClutchHelper", true) {
    val scaffoldOnlyReceiveHit by boolean("scaffoldOnlyReceiveHit", false)
    val scaffoldOnlyDuringCombat by boolean("ScaffoldOnlyDuringCombat", false)

}
