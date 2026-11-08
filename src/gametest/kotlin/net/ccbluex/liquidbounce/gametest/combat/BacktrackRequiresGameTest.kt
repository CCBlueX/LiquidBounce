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

import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.config.gson.util.GSON_DEFAULT
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.world.item.Items

/**
 * Backtrack with the Weapon requirement lags the target only while a weapon is held.
 */
class BacktrackRequiresGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) =
        context.runCombatChecks(listOf(ModuleBacktrack), { checkWeaponRequirement() })

    private fun CombatScene.checkWeaponRequirement() {
        reset()
        val husk = spawnHusk(at(0.0, 0.0, 2.0))
        context.onClient {
            ModuleBacktrack.set("Chance", "100")
            ModuleBacktrack.value("Requires").deserializeFrom(GSON_DEFAULT, JsonPrimitive("Weapon"))
            ModuleBacktrack.enabled = true
        }

        attack(husk)
        check(!context.fromClient { ModuleBacktrack.shouldCancelPackets() }) { "lagging without a weapon" }

        world.server.runCommand("item replace entity @a weapon.mainhand with minecraft:diamond_sword")
        context.waitFor(
            { client -> checkNotNull(client.player).mainHandItem.`is`(Items.DIAMOND_SWORD) },
            ATTACK_TICKS * 5,
        )
        attack(husk)
        check(context.fromClient { ModuleBacktrack.shouldCancelPackets() }) { "not lagging with a sword" }
    }

}
