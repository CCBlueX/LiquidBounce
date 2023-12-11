package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.player

object ScaffoldSlowFeature : ToggleableConfigurable(ModuleScaffold, "Slow", false) {
    private val slowSpeed by float("SlowSpeed", 0.6f, 0.1f..3f)

    val stateUpdateHandler =
        repeatable {
            player.velocity.x *= slowSpeed
            player.velocity.z *= slowSpeed
        }
}
