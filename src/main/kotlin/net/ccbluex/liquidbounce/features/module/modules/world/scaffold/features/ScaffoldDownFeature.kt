package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention

object ScaffoldDownFeature : ToggleableConfigurable(ModuleScaffold, "Down", false) {

    val handleMovementInput =
        handler<MovementInputEvent>(priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING) {
            if (shouldFallOffBlock()) {
                it.sneaking = false
            }
        }

    val handleSafeWalk =
        handler<PlayerSafeWalkEvent>(priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING) {
            if (shouldFallOffBlock()) {
                it.isSafeWalk = false
            }
        }

    val shouldGoDown: Boolean
        get() = enabled && mc.options.sneakKey.isPressed

    /**
     * When we are using the down scaffold we want to jump down on the next block in some situations
     */
    internal fun shouldFallOffBlock() = shouldGoDown && player.blockPos.add(0, -2, 0).canStandOn()
}
