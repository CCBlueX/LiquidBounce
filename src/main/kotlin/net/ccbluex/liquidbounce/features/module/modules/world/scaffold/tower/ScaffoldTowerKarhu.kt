package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower

import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.isBlockBelow
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.ccbluex.liquidbounce.utils.kotlin.Priority

object ScaffoldTowerKarhu : ScaffoldTower("Karhu") {

    private val timerSpeed by float("Timer", 5f, 0.1f..10f)
    private val triggerMotion by float("Trigger", 0.06f, 0.0f..0.2f, "Y/v")
    private val pulldown by boolean("Pulldown", true)

    @Suppress("unused")
    private val jumpHandler = sequenceHandler<PlayerJumpEvent>(priority = READ_FINAL_STATE) { event ->
        if (event.motion == 0f || event.isCancelled) {
            return@sequenceHandler
        }

        waitUntil { !player.isOnGround }
        Timer.requestTimerSpeed(timerSpeed, Priority.IMPORTANT_FOR_USAGE_1, ModuleScaffold)

        if (pulldown) {
            waitUntil { !player.isOnGround && player.velocity.y < triggerMotion }

            if (!isBlockBelow) {
                return@sequenceHandler
            }

            player.velocity.y -= 1f
        }
    }

}
