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

private const val SPRINT_TICKS = 6
private const val LIMIT_SPRINT_TICKS = 12
private const val SETTLE_TICKS = 2
private const val HELD = 0.6
private const val FLUSHED = 0.25
private const val ENEMY_YAW = -45f
private const val UNLIMITED = 2.0
private val LIMITED = 0.5..1.7

/**
 * FakeLag: the dynamic mode only lags for an enemy within the FOV, and the distance limit caps how far it lags.
 */
fun CombatScene.checkFakeLagFilters() {
    reset()
    context.onClient {
        ModuleFakeLag.set("Delay", "1000..1000")
        ModuleFakeLag.set("RecoilTime", "0")
        ModuleFakeLag.set("Range", "0..10")
        ModuleFakeLag.set("Mode", "Dynamic")
    }
    // Diagonally ahead: sprinting along +Z gets closer to it while it stays 40° and more off the crosshair
    spawnHusk(at(4.0, 0.0, 4.0))

    lagAtOrigin()
    sprintForward()
    check(heldDistance() > HELD) { "FakeLag: no lag while approaching an enemy" }

    context.onClient { ModuleFakeLag.set("FOV", "30") }
    lagAtOrigin()
    sprintForward()
    check(heldDistance() < FLUSHED) { "FakeLag: lagged with the enemy outside the FOV" }

    lagAtOrigin(ENEMY_YAW)
    sprintForward()
    check(heldDistance() > HELD) { "FakeLag: no lag with the enemy inside the FOV" }

    context.onClient {
        ModuleFakeLag.set("FOV", "180")
        ModuleFakeLag.set("Mode", "Constant")
    }
    lagAtOrigin()
    val unlimited = sprintForward(LIMIT_SPRINT_TICKS)
    check(unlimited > UNLIMITED) { "FakeLag: held only $unlimited blocks without a distance limit" }

    context.onClient {
        ModuleFakeLag.set("DistanceLimit/MaxDistance", "1")
        ModuleFakeLag.set("DistanceLimit/Enabled", "true")
    }
    lagAtOrigin()
    val limited = sprintForward(LIMIT_SPRINT_TICKS)
    check(limited in LIMITED) { "FakeLag: held up to $limited blocks with a distance limit of 1 block" }

    context.onClient { ModuleFakeLag.enabled = false }
}

/**
 * Restarts FakeLag with the player back at the origin facing [yaw], which sends what it held.
 */
private fun CombatScene.lagAtOrigin(yaw: Float = 0f) {
    context.onClient { ModuleFakeLag.enabled = false }
    world.server.runCommand("tp @a ${origin.x} ${origin.y} ${origin.z} $yaw 0")
    context.waitTicks(CombatScene.RESET_TICKS)
    context.onClient { ModuleFakeLag.enabled = true }
    context.waitTicks(SETTLE_TICKS)
}

/**
 * Sprints forward for [ticks] and stops. Returns the largest [CombatScene.heldDistance] on the way.
 */
private fun CombatScene.sprintForward(ticks: Int = SPRINT_TICKS): Double {
    var maxHeld = 0.0
    context.input.holdKey { options -> options.keyUp }
    context.input.holdKey { options -> options.keySprint }
    repeat(ticks) {
        context.waitTick()
        maxHeld = maxOf(maxHeld, heldDistance())
    }
    context.input.releaseKey { options -> options.keyUp }
    context.input.releaseKey { options -> options.keySprint }
    context.waitTicks(SETTLE_TICKS)
    return maxHeld
}
