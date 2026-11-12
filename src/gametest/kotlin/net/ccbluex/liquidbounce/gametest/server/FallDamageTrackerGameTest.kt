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
package net.ccbluex.liquidbounce.gametest.server

import net.ccbluex.gametest.harness.action.teleport
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import net.ccbluex.liquidbounce.utils.network.LocalPlayerFallDamageTracker
import net.minecraft.client.Minecraft

// Past the HURT_TICKS window, and past the vanilla damage cooldown the attack would be swallowed by
private const val DAMAGE_COOLDOWN_TICKS = 20

/**
 * A hurt cycle from fall damage must be told apart from one from an attack, see [LocalPlayerFallDamageTracker].
 * Both are caused by commands on a player that stays alive.
 */
class FallDamageTrackerGameTest : LiquidBounceGameTest({
    // 8 blocks, hurting for 5
    teleport(origin.above(8))
    awaitClient("the landing") { it.hurt() }
    check(client { LocalPlayerFallDamageTracker.isCurrentFallDamage }) { "fall damage is not tracked as such" }

    ticks(DAMAGE_COOLDOWN_TICKS)
    check(!client { LocalPlayerFallDamageTracker.isCurrentFallDamage }) {
        "the hurt window outlives the hurt cycle it belongs to"
    }

    run("damage $playerName 1 minecraft:player_attack")
    awaitClient("the attack") { it.hurt() }
    check(!client { LocalPlayerFallDamageTracker.isCurrentFallDamage }) { "an attack is tracked as fall damage" }
})

private fun Minecraft.hurt() = (player?.hurtTime ?: 0) > 0
