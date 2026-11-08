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

import net.ccbluex.liquidbounce.features.module.modules.combat.autoclicker.ModuleTriggerBot
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.utils.client.logger
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * With its reaction, TriggerBot (and AutoClicker, which shares its attacker) holds its clicks on a new husk under
 * the crosshair back for the reaction time. The husk it clicked before costs no reaction again.
 */
class CrosshairReactionGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.combatTest(ModuleTriggerBot) { server, y ->
        context.onClient {
            ModuleTriggerBot.set("Criticals", "Ignore")
            ModuleTriggerBot.set("Clicker/Technique", "Constant")
            ModuleTriggerBot.set("Clicker/CPS", "20..20")
        }

        val first = summonHusk(context, server, y, 0.5, 3.0)
        val immediate = ticksToFirstClick(context)
        server.discard(first)

        context.onClient {
            ModuleTriggerBot.set("Reaction/Delay", "$REACTION_TICKS..$REACTION_TICKS")
            ModuleTriggerBot.set("Reaction/Enabled", "true")
        }
        summonHusk(context, server, y, 0.5, 3.0)
        val delayed = ticksToFirstClick(context)
        val again = ticksToFirstClick(context)
        logger.info("TriggerBot clicked after $immediate ticks, with the reaction after $delayed ticks, and the " +
            "same husk again after $again ticks")

        check(immediate < REACTION_TICKS && delayed >= REACTION_TICKS) {
            "TriggerBot clicked after $immediate ticks without a reaction and after $delayed ticks with a " +
                "reaction of $REACTION_TICKS ticks"
        }
        check(again < REACTION_TICKS) { "TriggerBot took $again ticks to click the husk it clicked before" }
    }

    /**
     * Enables TriggerBot with a husk under the crosshair.
     *
     * @return the ticks until the first attack
     */
    private fun ticksToFirstClick(context: ClientGameTestContext): Int {
        context.onClient {
            CombatEvents.reset()
            ModuleTriggerBot.enabled = true
        }

        var ticks = 0
        while (ticks < WAIT_TICKS && context.fromClient { CombatEvents.attacks.isEmpty() }) {
            context.waitTick()
            ticks++
        }

        context.onClient { ModuleTriggerBot.enabled = false }
        check(ticks < WAIT_TICKS) { "TriggerBot did not click within $WAIT_TICKS ticks" }
        return ticks
    }

}
