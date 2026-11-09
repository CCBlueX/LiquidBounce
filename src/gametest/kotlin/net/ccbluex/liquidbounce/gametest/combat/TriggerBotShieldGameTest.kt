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
import net.ccbluex.liquidbounce.utils.entity.wouldBlockHit
import net.ccbluex.liquidbounce.utils.item.isAxe
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity

private const val AXE_SLOT = 1

/**
 * TriggerBot (and AutoClicker, which shares its attacker) hits a raised shield by default. Without IgnoreShield it
 * holds its clicks back, also with an axe in another hotbar slot, until the axe is in the main hand.
 */
class TriggerBotShieldGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.combatTest(ModuleTriggerBot) { server, y ->
        val blocker = summonBlocker(context, server, y)
        server.runCommand("item replace entity @a hotbar.$AXE_SLOT with iron_axe")

        context.onClient {
            ModuleTriggerBot.set("Criticals", "Ignore")
            ModuleTriggerBot.set("Clicker/Technique", "Constant")
            ModuleTriggerBot.set("Clicker/CPS", "20..20")
            ModuleTriggerBot.set("IgnoreShield", "false")
            CombatEvents.reset()
            ModuleTriggerBot.enabled = true
        }
        context.waitTicks(MEASURE_TICKS)
        context.onClient {
            check(CombatEvents.attacks.isEmpty() && CombatEvents.punches == 0) {
                "IgnoreShield off: ${CombatEvents.attacks.size} attacks, ${CombatEvents.punches} swings into the shield"
            }

            ModuleTriggerBot.set("IgnoreShield", "true")
        }
        context.waitTicks(MEASURE_TICKS)
        context.onClient {
            check(blocker in CombatEvents.attacks) { "IgnoreShield: no attack on the shield" }
            check(blocker.wouldBlockHit) { "The fist lowered the shield" }

            ModuleTriggerBot.set("IgnoreShield", "false")
        }

        context.input.holdKeyFor({ it.keyHotbarSlots[AXE_SLOT] }, 1)
        context.waitFor({ client -> checkNotNull(client.player).mainHandItem.isAxe }, WAIT_TICKS)
        context.onClient { CombatEvents.reset() }
        context.waitTicks(MEASURE_TICKS)
        check(context.fromClient { blocker in CombatEvents.attacks }) { "IgnoreShield off: no attack with an axe" }
    }

    /**
     * A husk in front of the player that faces them with a raised shield.
     */
    private fun summonBlocker(context: ClientGameTestContext, server: TestServerContext, y: Int): LivingEntity {
        val id = summonHusk(context, server, y, 0.5, 2.5, equipment = "{offhand:{id:\"minecraft:shield\"}}")
        server.runOnServer<RuntimeException> { s ->
            val husk = s.overworld().getEntity(id) as LivingEntity
            husk.forceSetRotation(180f, false, 0f, false)
            husk.startUsingItem(InteractionHand.OFF_HAND)
        }

        val blocker = context.fromClient { client -> checkNotNull(client.level).getEntity(id) as LivingEntity }
        context.waitFor({ blocker.wouldBlockHit }, WAIT_TICKS)
        return blocker
    }

}
