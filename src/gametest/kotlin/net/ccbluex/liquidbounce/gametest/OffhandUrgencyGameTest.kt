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
package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.ccbluex.liquidbounce.gametest.OffhandFixture.Companion.TIMEOUT
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.world.entity.monster.zombie.Husk

/**
 * The Offhand Totem/Urgency option: the totem delay stays whole while the player is safe and gets shorter once an
 * enemy stands beside them.
 */
class OffhandUrgencyGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.inOffhandWorld {
        configure(
            "Totem/SwitchDelay" to "$DELAY_MS..$DELAY_MS",
            "Totem/Urgency/Enabled" to "true",
            "Totem/Urgency/MinFactor" to "0"
        )
        context.onClient { ModuleOffhand.enabled = true }
        giveTotems(4)

        // the first refill only starts the measurement
        refill()
        val safe = refill()
        check(safe >= DELAY_MS - LATENCY_MS) { "Refilled after ${safe}ms without danger, the delay is ${DELAY_MS}ms" }

        cmd("execute at @p run summon husk ~2 ~ ~ {NoAI:1b,Tags:[\"danger\"]}")
        context.waitFor({ client -> client.level!!.entitiesForRendering().any { it is Husk } }, TIMEOUT)

        refill()
        val urgent = refill()
        check(urgent < DELAY_MS - LATENCY_MS) {
            "Refilled after ${urgent}ms with an enemy beside the player, the delay is ${DELAY_MS}ms"
        }

        cmd("kill @e[tag=danger]")
    }

    private companion object {
        const val DELAY_MS = 1500L
        const val LATENCY_MS = 400L
    }

}
