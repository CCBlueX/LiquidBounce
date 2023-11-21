package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.StateUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.client.QuickAccess.player
import net.ccbluex.liquidbounce.utils.client.mc

object ScaffoldDownFeature : ToggleableConfigurable(ModuleScaffold, "Down", false) {
    val repeatable =
        handler<StateUpdateEvent>(priority = -10) {
            if (shouldFallOffBlock()) {
                it.state.enforceEagle = false
            }
        }

    val shouldGoDown: Boolean
        get() = enabled && mc.options.sneakKey.isPressed

    /**
     * When we are using the down scaffold we want to jump down on the next block in some situations
     */
    internal fun shouldFallOffBlock() = shouldGoDown && player.blockPos.add(0, -2, 0).canStandOn()
}
