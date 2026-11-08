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

import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.gametest.CrystalAuraArena.Companion.PAUSE_TICKS
import net.ccbluex.liquidbounce.gametest.CrystalAuraArena.Companion.TIMEOUT
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * The CrystalAura Triggers/OnlyOnGround option.
 */
class CrystalAuraOnlyOnGroundGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.inCrystalAuraArena {
        configure("Triggers/OnlyOnGround" to "true")
        hover(true)
        context.onClient { ModuleCrystalAura.enabled = true }
        context.waitTicks(PAUSE_TICKS)
        check(crystalsOn(a) == 0) { "Placed while airborne with OnlyOnGround on" }

        // the crystal is in reach from up there
        configure("Triggers/OnlyOnGround" to "false")
        context.awaitTicks(TIMEOUT) { crystalsOn(a) > 0 }
        context.onClient { ModuleCrystalAura.enabled = false }
        clearCrystals()

        configure("Triggers/OnlyOnGround" to "true")
        hover(false)
        context.onClient { ModuleCrystalAura.enabled = true }
        context.awaitTicks(TIMEOUT) { crystalsOn(a) > 0 }
    }

    /**
     * Lets the player fly [HOVER_HEIGHT] blocks above the floor, or land again.
     */
    private fun CrystalAuraArena.hover(airborne: Boolean) {
        context.onClient { client ->
            val player = client.player!!
            player.abilities.mayfly = airborne
            player.abilities.flying = airborne
            player.onUpdateAbilities()
        }

        if (airborne) {
            cmd("execute as @a at @s run tp @s ~ ~$HOVER_HEIGHT ~")
            context.waitFor({ client ->
                val player = client.player!!
                !player.onGround() && player.y >= origin.y + HOVER_HEIGHT - 0.1
            }, TIMEOUT)
        } else {
            context.waitFor({ client -> client.player!!.onGround() }, TIMEOUT)
        }
    }

    private companion object {
        const val HOVER_HEIGHT = 1.5
    }

}
