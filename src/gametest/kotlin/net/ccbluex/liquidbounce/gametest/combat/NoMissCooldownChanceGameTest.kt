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

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleNoMissCooldown
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.world.phys.HitResult

/**
 * NoMissCooldown changes a click into the air by its Chance. A miss it leaves alone is a vanilla one: its cooldown
 * swallows the next click of a player who clicks faster than a tick sees the attack button released.
 */
class NoMissCooldownChanceGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.combatTest(ModuleNoMissCooldown) { server, y ->
        face(context, server, y, 0f, -90f)
        context.waitFor({ client -> client.hitResult?.type == HitResult.Type.MISS }, WAIT_TICKS)
        assertPunches(context, "vanilla", chances = listOf(100, 100), punches = listOf(true, false))

        context.onClient { ModuleNoMissCooldown.enabled = true }
        assertPunches(context, "RemoveAttackCooldown", chances = listOf(100, 100), punches = listOf(true, true))
        assertPunches(context, "RemoveAttackCooldown", chances = listOf(0, 0), punches = listOf(true, false))
        assertPunches(context, "RemoveAttackCooldown", chances = listOf(0, 100), punches = listOf(true, false))

        context.onClient { ModuleNoMissCooldown.set("CancelAttackOnMiss", "true") }
        assertPunches(context, "CancelAttackOnMiss", chances = listOf(100, 100), punches = listOf(false, false))
        assertPunches(context, "CancelAttackOnMiss", chances = listOf(0, 0), punches = listOf(true, false))
    }

    /**
     * Clicks into the air once with each of [chances], all within the cooldown a vanilla miss has, and checks
     * which of the clicks punch.
     */
    private fun assertPunches(
        context: ClientGameTestContext,
        case: String,
        chances: List<Int>,
        punches: List<Boolean>,
    ) {
        val punched = chances.map { clickPunches(context, it) }
        check(punched == punches) { "$case with Chance $chances: punches $punched" }

        context.input.releaseKey { it.keyAttack }
        context.waitTicks(SETTLE_TICKS)
    }

    /**
     * Presses the attack button with [chance] and keeps it down, after releasing it within the same tick when it
     * was down. Returns whether the player punched.
     */
    private fun clickPunches(context: ClientGameTestContext, chance: Int): Boolean {
        context.onClient {
            ModuleNoMissCooldown.set("Chance", chance.toString())
            CombatEvents.reset()
        }
        context.input.releaseKey { it.keyAttack }
        context.input.holdKey { it.keyAttack }
        context.waitTicks(CLICK_TICKS)
        return CombatEvents.punches > 0
    }

}

private const val CLICK_TICKS = 2
