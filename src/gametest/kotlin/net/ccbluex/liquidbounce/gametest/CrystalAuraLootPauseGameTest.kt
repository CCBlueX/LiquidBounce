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
import net.minecraft.world.entity.item.ItemEntity

/**
 * The CrystalAura LootPause option.
 */
class CrystalAuraLootPauseGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.inCrystalAuraArena {
        lootPause()
    }

    private fun CrystalAuraArena.lootPause() {
        configure("Place/Enabled" to "false", "Destroy/Enabled" to "false", "LootPause/Enabled" to "true")
        val loot = huskPos.offset(-3, 0, 0)
        cmd("summon husk ${loot.x + 0.5} ${loot.y} ${loot.z + 0.5} {NoAI:1b,Tags:[\"loot\"]}")
        context.onClient { ModuleCrystalAura.enabled = true }
        context.waitFor({ ModuleCrystalAura.targetTracker.target?.maxHealth == 20f }, TIMEOUT)

        cmd("kill @e[type=husk,tag=loot]")
        cmd("summon item ${loot.x + 0.5} ${loot.y} ${loot.z + 0.5} " +
            "{Item:{id:\"minecraft:diamond\",count:1},PickupDelay:32767s}")
        context.waitFor({ client -> client.level!!.entitiesForRendering().any { it is ItemEntity } }, TIMEOUT)

        configure("Place/Enabled" to "true", "Destroy/Enabled" to "true")
        context.waitTicks(PAUSE_TICKS)
        check(crystalsOn(a) == 0) { "Placed while the loot of the killed target lay around" }

        cmd("kill @e[type=item]")
        context.awaitTicks(TIMEOUT) { crystalsOn(a) > 0 }
        context.onClient { ModuleCrystalAura.enabled = false }
        configure("LootPause/Enabled" to "false")
        clearCrystals()
    }

}
