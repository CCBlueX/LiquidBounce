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

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAimbot
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.world.entity.LivingEntity

/**
 * The armor target priority prefers a bare husk over a closer one in a chestplate.
 */
class TargetArmorPriorityGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.combatTest(ModuleAimbot) { server, y ->
        val target = { context.fromClient { ModuleAimbot.targetTracker.target?.id } }
        val chestplate = "{chest:{id:\"minecraft:diamond_chestplate\"}}"
        val armored = summonHusk(context, server, y, 0.5, 3.0, equipment = chestplate)
        val bare = summonHusk(context, server, y, -2.5, 2.0)
        context.waitFor({ client ->
            val armor = { id: Int -> (client.level?.getEntity(id) as? LivingEntity)?.armorValue ?: 0 }
            armor(armored) > armor(bare)
        }, WAIT_TICKS)
        context.onClient {
            ModuleAimbot.set("Target/Priority", "Armor")
            ModuleAimbot.enabled = true
        }
        context.waitTicks(SETTLE_TICKS)
        check(target() == bare) { "The armor priority picked ${target()} over the bare husk $bare" }

        context.onClient { ModuleAimbot.enabled = false }
        server.runCommand("kill @e[type=husk]")
    }

}
