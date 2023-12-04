package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.TickJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.SpeedAntiCornerBump
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.enforced
import net.ccbluex.liquidbounce.utils.entity.downwards
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.entity.upwards
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.zeroXZ


object Custom : Choice("Custom") {
    override val parent: ChoiceConfigurable
        get() = ModuleSpeed.modes

    private val horizontalSpeed by float("HorizontalSpeed", 1f, 0.1f..10f)
    private val resetHorizontalSpeed by boolean("ResetHorizontalSpeed", true)
    private val customStrafe by boolean("CustomStrafe", false)
    private val strafe by float("Strafe", 1f, 0.1f..10f)
    private val verticalSpeed by float("VerticalSpeed", 0.42f, 0.0f..3f)
    private val resetVerticalSpeed by boolean("ResetVerticalSpeed", true)
    private val timerSpeed by float("TimerSpeed", 1f, 0.1f..10f)

    val repeatable = repeatable {
        if (!player.moving) {
            return@repeatable
        }

        Timer.requestTimerSpeed(timerSpeed, priority = Priority.IMPORTANT_FOR_USAGE)

        when {
            player.isOnGround -> {
                player.strafe(speed = horizontalSpeed.toDouble())
                if (verticalSpeed > 0) player.velocity.y = verticalSpeed.toDouble()
            }

            customStrafe -> player.strafe(speed = strafe.toDouble())
            else -> player.strafe()
        }

    }

    override fun enable() {
        if (resetHorizontalSpeed) {
            player.zeroXZ()
        }

        if (resetVerticalSpeed) player.velocity.y = 0.0
    }
}

object SpeedYPort : Choice("YPort") {

    override val parent: ChoiceConfigurable
        get() = ModuleSpeed.modes

    val repeatable = repeatable {
        if (player.isOnGround && player.moving) {
            player.strafe(speed = 0.4)
            player.upwards(0.42f)
            waitTicks(1)
            player.downwards(-1f)
        }
    }

}

object LegitHop : Choice("LegitHop") {

    override val parent: ChoiceConfigurable
        get() = ModuleSpeed.modes

    private val optimizeForCriticals by boolean("OptimizeForCriticals", true)
    // Avoids running into edges which loses speed
    private val avoidEdgeBump by boolean("AvoidEdgeBump", true)

    val tickJumpHandler = handler<TickJumpEvent> {
        if (!player.isOnGround || !player.moving) {
            return@handler
        }

        // We want the player to be able to jump if he wants to
        if (!mc.options.jumpKey.isPressed && doOptimizationsPreventJump())
            return@handler

        player.jump()
    }

    private fun doOptimizationsPreventJump(): Boolean {
        if (optimizeForCriticals && ModuleCriticals.shouldWaitForJump(0.42f)) {
            return true
        }

        if (avoidEdgeBump && SpeedAntiCornerBump.shouldDelayJump()) {
            return true
        }

        return false
    }

    val injectMovementEnforcement = repeatable {
        if (!player.isOnGround) {
            return@repeatable
        }

        mc.options.jumpKey.enforced = false
    }
}
