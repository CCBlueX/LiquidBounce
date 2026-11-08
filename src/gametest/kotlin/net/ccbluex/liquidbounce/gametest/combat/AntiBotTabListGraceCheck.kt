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

import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import java.util.UUID

private const val TAB_LIST_GRACE = 20

/**
 * AntiBot: a player may be missing from the tab list for a grace period after spawning.
 */
fun CombatScene.checkAntiBotTabListGrace() {
    startCustomAntiBot {
        ModuleAntiBot.set("NotInTabList", "true")
        ModuleAntiBot.set("TabListGrace", TAB_LIST_GRACE.toString())
    }
    val fresh = spawnFakePlayer(UUID.randomUUID(), FAKE_PLAYER_ID, "Fresh")
    context.waitTicks(TAB_LIST_GRACE / 2)
    check(!isBot(fresh)) { "AntiBot: missing from the tab list within the grace period" }
    context.waitTicks(TAB_LIST_GRACE)
    check(isBot(fresh)) { "AntiBot: missing from the tab list after the grace period" }
    stopAntiBot(fresh)
}
