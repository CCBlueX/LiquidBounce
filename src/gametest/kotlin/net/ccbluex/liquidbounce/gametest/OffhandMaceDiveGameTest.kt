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
 * The Offhand Totem/MaceDive option.
 */
class OffhandMaceDiveGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.inOffhandWorld {
        // only the health check could ask for a totem, and it does not at full health with no armor rule
        configure("Totem/Health/Enabled" to "true", "Totem/Health/MissingArmor" to "false")
        context.onClient { ModuleOffhand.enabled = true }
        giveTotems(1)
        removeOffhand()

        check(!diveEquips()) { "A totem was swapped in for a mace dive without MaceDive" }

        configure("Totem/MaceDive/Enabled" to "true")
        check(diveEquips()) { "MaceDive did not swap a totem in before the diver landed" }
    }

    /**
     * Drops a husk with a mace onto the player and returns whether the offhand got a totem while it was falling.
     */
    private fun OffhandFixture.diveEquips(): Boolean {
        cmd("execute at @p run summon husk ~1 ~12 ~ " +
            "{Tags:[\"diver\"],equipment:{mainhand:{id:\"minecraft:mace\",count:1}}}")
        context.waitFor({ client -> client.level!!.entitiesForRendering().any { it is Husk } }, TIMEOUT)

        var equipped = false
        for (tick in 0 until TIMEOUT) {
            context.waitTick()
            val (offhandTotem, diverAbove) = server.query { server ->
                val player = server.playerList.players[0]
                val diver = server.overworld().getEntitiesOfClass(Husk::class.java, player.boundingBox.inflate(16.0))
                    .firstOrNull()
                player.offhandItem.isTotem() to (diver != null && diver.y > player.y + 1.0)
            }

            if (offhandTotem) {
                equipped = diverAbove
                break
            }
            if (!diverAbove) {
                break
            }
        }

        cmd("kill @e[tag=diver]")
        context.waitFor({ client -> client.level!!.entitiesForRendering().none { it is Husk } }, TIMEOUT)
        if (equipped) {
            removeOffhand()
        }
        return equipped
    }

}
