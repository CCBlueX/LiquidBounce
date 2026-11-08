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
 * With OnlyNearEnemy, NoMissCooldown leaves a click into the air alone unless an enemy is within the range: the
 * cooldown of the miss then swallows the next click, as it does in vanilla.
 */
class NoMissCooldownNearEnemyGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.combatTest(ModuleNoMissCooldown) { server, y ->
        face(context, server, y, 0f, -90f)
        context.waitFor({ client -> client.hitResult?.type == HitResult.Type.MISS }, WAIT_TICKS)

        context.onClient {
            ModuleNoMissCooldown.set("OnlyNearEnemy/Enabled", "true")
            ModuleNoMissCooldown.enabled = true
        }
        assertSecondClick(context, "no enemy", range = FAR_RANGE, punches = false)

        summonHusk(context, server, y, 0.5, HUSK_Z)
        assertSecondClick(context, "enemy out of range", range = NEAR_RANGE, punches = false)
        assertSecondClick(context, "enemy in range", range = FAR_RANGE, punches = true)
    }

    /**
     * Clicks into the air twice within the cooldown a vanilla miss has, with [range] as the Range of
     * OnlyNearEnemy, and checks whether the second click [punches].
     */
    private fun assertSecondClick(context: ClientGameTestContext, case: String, range: Int, punches: Boolean) {
        context.onClient { ModuleNoMissCooldown.set("OnlyNearEnemy/Range", range.toString()) }
        val punched = List(2) { clickPunches(context) }
        check(punched == listOf(true, punches)) { "$case with Range $range: punches $punched" }

        context.input.releaseKey { it.keyAttack }
        context.waitTicks(SETTLE_TICKS)
    }

    /**
     * Presses the attack button and keeps it down, after releasing it within the same tick when it was down.
     * Returns whether the player punched.
     */
    private fun clickPunches(context: ClientGameTestContext): Boolean {
        context.onClient { CombatEvents.reset() }
        context.input.releaseKey { it.keyAttack }
        context.input.holdKey { it.keyAttack }
        context.waitTicks(CLICK_TICKS)
        return CombatEvents.punches > 0
    }

}

private const val CLICK_TICKS = 2

/**
 * The husk stands 3.7 blocks from the eyes of the player, between the two ranges.
 */
private const val HUSK_Z = 4.5
private const val NEAR_RANGE = 3
private const val FAR_RANGE = 5
