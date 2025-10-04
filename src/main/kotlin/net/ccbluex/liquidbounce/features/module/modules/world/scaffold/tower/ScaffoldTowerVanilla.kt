package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower

import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE

object ScaffoldTowerVanilla : ScaffoldTower("Vanilla") {
    @Suppress("unused")
    private val jumpHandler = sequenceHandler<PlayerJumpEvent>(priority = READ_FINAL_STATE) { event ->
        tickUntil { !player.isOnGround }
    }
}
