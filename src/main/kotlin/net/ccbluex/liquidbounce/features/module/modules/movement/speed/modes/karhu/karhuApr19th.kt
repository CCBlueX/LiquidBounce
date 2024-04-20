package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.karhu

import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.kotlin.Priority

object karhuApr19th : SpeedBHopBase("karhuSpeed") {
    private val timerSpeedinAir by float("TimerSpeedInAir", 8f, 0.1f..15f)
    private val timerSpeedfalling by float("TimerSpeedFalling", 2f, 0.1f..15f)

    val JumpEvent = sequenceHandler<PlayerJumpEvent> {
        if (timerSpeedinAir != 1f) {
            Timer.requestTimerSpeed(timerSpeedinAir, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
        }

    }
    val repeatable = repeatable {
        when {
            player.isOnGround -> {
                player.velocity = player.velocity.multiply(1.0 + 0.02, 1.0 + 0.0007, 1.0 + 0.02)
            }
            !player.isOnGround -> {
                println("test")
                Timer.requestTimerSpeed(timerSpeedfalling, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
            }
        }
        return@repeatable
    }


}
