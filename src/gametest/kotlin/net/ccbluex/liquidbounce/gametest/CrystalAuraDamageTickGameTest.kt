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
import net.ccbluex.liquidbounce.gametest.CrystalAuraArena.Companion.TIMEOUT
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.world.item.Items

/**
 * The CrystalAura Destroy/DamageTick option.
 */
class CrystalAuraDamageTickGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.inCrystalAuraArena {
        val free = explosions()
        check(free.crystals >= free.hits + MIN_WASTED) {
            "Without DamageTick only ${free.crystals} crystals for ${free.hits} hits, the hurt time wasted none"
        }

        configure("Destroy/DamageTick" to "true")
        val gated = explosions()
        check(gated.hits >= MIN_HITS) { "Only ${gated.hits} hits with DamageTick" }
        // the last crystal may still stand when the sample ends
        check(gated.crystals <= gated.hits + 1) {
            "DamageTick used ${gated.crystals} crystals for ${gated.hits} hits, so it attacked in the hurt time"
        }
        configure("Destroy/DamageTick" to "false")
    }

    private class Sample(val crystals: Int, val hits: Int)

    /**
     * Runs the aura for [SAMPLE_TICKS] and counts the crystals it used and the explosions that hurt the husk. The
     * husk takes the same damage from every crystal on [CrystalAuraArena.a], so a crystal broken in its hurt time
     * deals nothing.
     */
    private fun CrystalAuraArena.explosions(): Sample {
        fun crystals() = context.fromClient { client -> client.player!!.inventory.countItem(Items.END_CRYSTAL) }
        fun health() = context.fromClient { client -> husk(client).health }

        val stock = crystals()
        cmd("give @a end_crystal 64")
        context.waitFor({ client -> client.player!!.inventory.countItem(Items.END_CRYSTAL) > stock }, TIMEOUT)

        val before = crystals()
        var health = health()
        var hits = 0
        context.onClient { ModuleCrystalAura.enabled = true }
        // keeps counting a little after disabling, an attack sent just before can still land
        repeat(SAMPLE_TICKS + SETTLE_TICKS) { tick ->
            if (tick == SAMPLE_TICKS) {
                context.onClient { ModuleCrystalAura.enabled = false }
            }
            context.waitTick()
            val now = health()
            if (now < health) {
                hits++
            }
            health = now
        }

        val used = before - crystals()
        clearCrystals()
        return Sample(used, hits)
    }

    private companion object {
        const val SAMPLE_TICKS = 60
        const val SETTLE_TICKS = 10
        const val MIN_HITS = 3
        const val MIN_WASTED = 4
    }

}
