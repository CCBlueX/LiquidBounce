package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.QuickAccess.player
import net.ccbluex.liquidbounce.utils.client.StateUpdateEvent
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge

object ScaffoldEagleFeature : ToggleableConfigurable(ModuleScaffold, "Eagle", false) {
    private val blocksToEagle by int("BlocksToEagle", 0, 0..10)

    private val edgeDistance by float("EagleEdgeDistance", 0.01f, 0.01f..1.3f)

    // Makes you sneak until first block placed, so with eagle enabled you won't fall off, when enabled
    private var placedBlocks = 0

    val stateUpdateHandler =
        handler<StateUpdateEvent> {
            val player = player

            if (ScaffoldDownFeature.shouldFallOffBlock()) {
                return@handler
            }

            if (!player.abilities.flying && player.isCloseToEdge(edgeDistance.toDouble()) && placedBlocks == 0) {
                it.state.enforceEagle = true
            }
        }

    fun onBlockPlacement() {
        if (!enabled) {
            return
        }

        placedBlocks += 1

        if (placedBlocks > blocksToEagle) {
            placedBlocks = 0
        }
    }
}
