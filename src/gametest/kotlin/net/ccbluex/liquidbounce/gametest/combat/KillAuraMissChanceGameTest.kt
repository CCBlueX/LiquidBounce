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

import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * Every KillAura hit becomes a fail swing with a FailSwing miss chance of 100 %, none with 0 %.
 */
class KillAuraMissChanceGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.combatTest(ModuleKillAura) { server, y ->
        val husk = summonHusk(context, server, y, 0.5, 3.0)
        context.onClient {
            ModuleKillAura.set("FailSwing/Enabled", "true")
            ModuleKillAura.set("FailSwing/MissChance", "100")
            CombatEvents.reset()
            ModuleKillAura.enabled = true
        }
        context.waitTicks(MEASURE_TICKS)
        val (missedAttacks, punches) = context.fromClient { CombatEvents.attacks.size to CombatEvents.punches }
        check(missedAttacks == 0 && punches > 0) { "Miss chance 100 %: $missedAttacks attacks, $punches swings" }
        check(server.huskHealth(husk) == HUSK_HEALTH) { "Miss chance 100 %: the husk took damage" }

        context.onClient {
            ModuleKillAura.set("FailSwing/MissChance", "0")
            CombatEvents.reset()
        }
        context.waitTicks(MEASURE_TICKS)
        val attacks = context.fromClient { CombatEvents.attacks.size }
        check(attacks > 0) { "Miss chance 0 %: no attack" }
        check(server.huskHealth(husk) < HUSK_HEALTH) { "Miss chance 0 %: the husk took no damage" }

        context.onClient { ModuleKillAura.enabled = false }
        server.runCommand("kill @e[type=husk]")
    }

}
