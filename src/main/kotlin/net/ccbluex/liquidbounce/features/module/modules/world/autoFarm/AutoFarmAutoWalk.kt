package net.ccbluex.liquidbounce.features.module.modules.world.autoFarm

import net.ccbluex.liquidbounce.config.ToggleableConfigurable

object AutoFarmAutoWalk : ToggleableConfigurable(ModuleAutoFarm, "AutoWalk", false){

    // Makes the player move to farmland blocks where there is a need for crop replacement
    val toPlace by boolean("ToPlace", true)

    val toItems by boolean("ToItems", true)
}

