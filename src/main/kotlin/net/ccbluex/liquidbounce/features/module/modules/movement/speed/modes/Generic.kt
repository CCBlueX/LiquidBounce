package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.TickJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.SpeedAntiCornerBump
import net.ccbluex.liquidbounce.utils.entity.downwards
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.entity.upwards

object SpeedSpeedYPort : Choice("YPort") {

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

object SpeedLegitHop : Choice("LegitHop") {

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

    val injectMovementEnforcement = handler<MovementInputEvent> {
        if (!player.isOnGround) {
            return@handler
        }

        it.jumping = false
    }
}
