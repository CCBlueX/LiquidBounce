package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.matrix

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.kotlin.Priority
class SpeedMatrixTimer(override val parent: ChoiceConfigurable<*>) : Choice("SpeedTimer") {

    private val boostSpeed by float("BoostSpeed", 1.4f, 0.1f..10f)
    private val slowSpeed by float("SlowSpeed", 0.3f, 0.1f..10f)

    val repeatable = repeatable {
        if (player.isOnGround && player.moving) {
            Timer.requestTimerSpeed(slowSpeed, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
        } else if (!player.isOnGround && player.moving) {
            Timer.requestTimerSpeed(boostSpeed, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
        }
    }

}
