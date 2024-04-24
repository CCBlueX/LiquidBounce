package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.special

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority

object BlocksMCSpeed : Choice("BlocksMC Speed") {
    private var Timerinc by float("HorizontalJumpOff", 4.25f, +0.01f..8f)

    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleSpeed.modes

    val JumpEvent = handler<PlayerJumpEvent> {
        player.strafe(speed = player.sqrtSpeed.coerceAtLeast(0.281))
        //Timer.requestTimerSpeed(0.5f, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
    }
    val moveHandler = handler<MovementInputEvent> {
        if(Timerinc < 1f)
        {
            Timerinc = 1f;
        }
        if(!player.moving)
        {
            return@handler
        }
        if (!player.isOnGround) {
            //Timer.requestTimerSpeed(Timerinc, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
            return@handler
        }

        if (ModuleSpeed.shouldDelayJump())
            return@handler

        it.jumping = true
        player.velocity.x *= 1.001
        player.velocity.z *= 1.001
    }

    val timerRepeatable = repeatable {
        Timer.requestTimerSpeed(Timerinc, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
        waitTicks(30)
    }




}
