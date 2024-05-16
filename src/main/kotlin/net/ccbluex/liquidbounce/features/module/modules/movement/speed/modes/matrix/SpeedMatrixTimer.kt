package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.matrix

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.kotlin.Priority

object SpeedMatrixTimer : Choice("MatrixTimer") {

    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleSpeed.modes

    private val timerSpeed by float("Timer", 5.0F, 0.1F..10.0F, "x")

    val repeatable = repeatable {
        if (!player.moving) {
            return@repeatable
        }

        Timer.requestTimerSpeed(timerSpeed, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)

        player.velocity = player.velocity.multiply(0.8);
    }

}
