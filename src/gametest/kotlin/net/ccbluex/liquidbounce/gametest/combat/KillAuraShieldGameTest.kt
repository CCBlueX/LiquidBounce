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
import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.utils.entity.wouldBlockHit
import net.ccbluex.liquidbounce.utils.world.nextLocalEntityId
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.UUID

/**
 * KillAura's shield handling: the boolean `IgnoreShield` of older configs still loads, Skip keeps a player with a
 * raised shield as the target and only hits it once the shield is down, Ignore hits the shield and Avoid drops
 * the player.
 */
class KillAuraShieldGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.combatTest(ModuleKillAura) { _, y ->
        checkMigration(context)
        checkHandling(context, y)
    }

    private fun checkMigration(context: ClientGameTestContext) = context.onClient {
        val json = ConfigSystem.serializeValueGroup(ModuleKillAura)
        val target = json.getAsJsonArray("value").map { it.asJsonObject }.first { it["name"].asString == "Target" }
        val shield = target.getAsJsonArray("value").map { it.asJsonObject }.first { it["name"].asString == "Shield" }
        shield.addProperty("name", "IgnoreShield")
        shield.add("value", JsonPrimitive(false))

        ConfigSystem.deserializeValueGroup(ModuleKillAura, json)

        val handling = ModuleKillAura.value("Target/Shield").get() as Tagged
        check(handling.tag == "Avoid") { "IgnoreShield false of an old config loaded as ${handling.tag}" }
        ModuleKillAura.set("Target/Shield", "Ignore")
    }

    private fun checkHandling(context: ClientGameTestContext, y: Int) {
        val blocker = context.fromClient { client ->
            val level = checkNotNull(client.level)
            Blocker(level).apply {
                id = level.nextLocalEntityId()
                setPos(0.5, y.toDouble(), 2.5)
                yRot = 180f
                yHeadRot = 180f
                yBodyRot = 180f
                setItemSlot(EquipmentSlot.OFFHAND, ItemStack(Items.SHIELD))
                level.addEntity(this)
                useShield(true)
            }
        }
        context.waitFor({ blocker.wouldBlockHit }, WAIT_TICKS)

        context.onClient {
            ModuleKillAura.set("Target/Shield", "Skip")
            CombatEvents.reset()
            ModuleKillAura.enabled = true
        }
        context.waitTicks(MEASURE_TICKS)
        context.onClient {
            check(ModuleKillAura.targetTracker.target === blocker) { "Skip dropped the blocking player" }
            check(blocker !in CombatEvents.attacks) { "Skip hit the raised shield" }

            blocker.useShield(false)
        }
        context.waitTicks(MEASURE_TICKS)
        context.onClient {
            check(blocker in CombatEvents.attacks) { "Skip did not hit once the shield was down" }

            blocker.useShield(true)
        }
        context.waitFor({ blocker.wouldBlockHit }, WAIT_TICKS)

        context.onClient {
            ModuleKillAura.set("Target/Shield", "Ignore")
            CombatEvents.reset()
        }
        context.waitTicks(MEASURE_TICKS)
        check(context.fromClient { blocker in CombatEvents.attacks }) { "Ignore did not hit the raised shield" }

        context.onClient { ModuleKillAura.set("Target/Shield", "Avoid") }
        context.waitTicks(SETTLE_TICKS)
        context.onClient {
            check(ModuleKillAura.targetTracker.target == null) { "Avoid kept the blocking player" }

            ModuleKillAura.enabled = false
            blocker.discard()
        }
    }

}

/**
 * A client-only player that raises and lowers its shield through the flags the server syncs for a real one.
 */
private class Blocker(level: ClientLevel) : RemotePlayer(level, GameProfile(UUID.randomUUID(), "Blocker")) {

    fun useShield(using: Boolean) {
        setLivingEntityFlag(LIVING_ENTITY_FLAG_OFF_HAND, true)
        setLivingEntityFlag(LIVING_ENTITY_FLAG_IS_USING, using)
    }

}
