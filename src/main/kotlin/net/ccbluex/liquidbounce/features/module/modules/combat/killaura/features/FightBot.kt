package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.clickScheduler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.targetTracker
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput

/**
 * A fight bot, fights for you, probably better than you. Lol.
 */
object FightBot : ToggleableConfigurable(ModuleKillAura, "FightBot", false) {

    val safeRange by float("SafeRange", 4f, 0.1f..5f)
    val tickToAttack by int("TickToAttack", 10, 1..20)

    var sideToGo = false

    val repeatable = repeatable {
        sideToGo = !sideToGo
        waitTicks((20..60).random())
    }

    val inputHandler = handler<MovementInputEvent>(priority = 1000) { ev ->
        val enemy = targetTracker.lockedOnTarget ?: return@handler
        val distance = enemy.boxedDistanceTo(player)

        if (clickScheduler.isClickOnNextTick() && distance < ModuleKillAura.range) {
            ev.directionalInput = DirectionalInput.NONE
            sideToGo = !sideToGo
        } else if (clickScheduler.isClickOnNextTick(tickToAttack)) {
            ev.directionalInput = DirectionalInput.FORWARDS
        } else if (distance < safeRange) {
            ev.directionalInput = DirectionalInput.BACKWARDS
        } else {
            ev.directionalInput = DirectionalInput.NONE
        }

        // We are now in range of the player, so try to circle around him
        if (distance < ModuleKillAura.range) {
            ev.directionalInput = ev.directionalInput.copy(left = !sideToGo, right = sideToGo)
        }

    }

}
