/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.gametest.combat

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleFakeLag
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.world.entity.Mob

private const val SPRINT_TICKS = 6
private const val KNOCKBACK_TICKS = 12
private const val SETTLE_TICKS = 2

// Longer than the delay of one second
private const val DELAY_TICKS = 25

// More than a player that is not lagging gets ahead of the server while knocked back
private const val HELD = 1.5
private const val FLUSHED = 0.25

/**
 * FakeLag: the knockback mode does not lag until a hit knocks us back, and stops once the delay is over.
 */
fun CombatScene.checkFakeLagKnockback() {
    reset()
    context.onClient {
        ModuleFakeLag.set("Delay", "1000..1000")
        ModuleFakeLag.set("RecoilTime", "0")
        ModuleFakeLag.set("Mode", "Knockback")
        ModuleFakeLag.enabled = true
    }
    // Behind us, so that its hit knocks us forward
    val husk = spawnHusk(at(0.0, 0.0, -1.5))

    sprintForward(SPRINT_TICKS)
    context.waitTicks(SETTLE_TICKS)
    check(heldDistance() < FLUSHED) { "FakeLag: Knockback mode lagged without knockback" }

    val hit = onServer { server ->
        (serverEntity(server, husk) as Mob).doHurtTarget(server.overworld(), serverPlayer(server))
    }
    check(hit) { "The husk could not hit the player" }
    val held = sprintForward(KNOCKBACK_TICKS)
    check(held > HELD) { "FakeLag: Knockback mode held only $held blocks after knockback" }

    context.waitTicks(DELAY_TICKS)
    check(heldDistance() < FLUSHED) { "FakeLag: Knockback mode still lags after its delay" }

    context.onClient { ModuleFakeLag.enabled = false }
}

/**
 * Sprints forward for [ticks] and stops. Returns the largest [CombatScene.heldDistance] on the way.
 */
private fun CombatScene.sprintForward(ticks: Int): Double {
    var maxHeld = 0.0
    context.input.holdKey { options -> options.keyUp }
    context.input.holdKey { options -> options.keySprint }
    repeat(ticks) {
        context.waitTick()
        maxHeld = maxOf(maxHeld, heldDistance())
    }
    context.input.releaseKey { options -> options.keyUp }
    context.input.releaseKey { options -> options.keySprint }
    return maxHeld
}
