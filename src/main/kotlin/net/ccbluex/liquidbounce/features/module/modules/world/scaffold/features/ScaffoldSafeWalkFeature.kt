package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold

object ScaffoldSafeWalkFeature : ToggleableConfigurable(ModuleScaffold, "SafeWalk", true) {
    val safeWalkHandler =
        handler<PlayerSafeWalkEvent> { event ->
            event.isSafeWalk = !ScaffoldDownFeature.shouldFallOffBlock()
        }
}
