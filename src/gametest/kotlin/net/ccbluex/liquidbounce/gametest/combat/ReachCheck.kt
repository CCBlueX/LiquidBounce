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

import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReach
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient

/**
 * Reach Random: a successful roll reaches a husk beyond vanilla reach, and the hit rolls again.
 */
fun CombatScene.checkReachRandom() {
    reset()
    val husk = spawnHusk(at(0.0, 0.0, 5.0))
    context.waitTicks(PICK_TICKS)
    check(context.fromClient { crosshairEntity(it) } == null) { "Reach: husk in vanilla reach" }

    context.onClient {
        ModuleReach.set("Entity/RangeIncrease", "0")
        ModuleReach.set("Random/Increase", "2..2")
        ModuleReach.set("Random/Chance", "100")
        ModuleReach.set("Random/Enabled", "true")
        ModuleReach.enabled = true
    }
    context.waitTicks(PICK_TICKS)
    check(context.fromClient { crosshairEntity(it) } == husk) { "Reach: the rolled increase did not apply" }

    context.onClient { ModuleReach.set("Random/Chance", "0") }
    context.input.pressKey { options -> options.keyAttack }
    context.waitTicks(ATTACK_TICKS)
    check(serverHealth(husk) < HUSK_HEALTH) { "Reach: the server did not take the hit" }
    check(context.fromClient { crosshairEntity(it) } == null) { "Reach: the hit did not roll a failed chance" }

    context.onClient {
        ModuleReach.set("Random/Chance", "100")
        ModuleReach.set("Random/Enabled", "false")
        ModuleReach.set("Random/Enabled", "true")
    }
    context.waitTicks(PICK_TICKS)
    check(context.fromClient { crosshairEntity(it) } == husk) { "Reach: re-enabling did not roll the increase" }

    context.onClient { ModuleReach.enabled = false }
}
