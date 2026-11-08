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

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoEntityInteract
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.zombie.Husk
import net.minecraft.world.item.Items
import net.minecraft.world.phys.EntityHitResult

/**
 * [ModuleNoEntityInteract] with OnlyUse has to keep the crosshair and the attack on an armor stand (a skipped type)
 * while a right click goes past it, with Offhand also for the offhand item, and OnlyEnemies has to leave everything
 * but enemies to the crosshair.
 *
 * A right click that reaches the armor stand hangs the held helmet on it, one that goes past it puts the helmet on.
 */
class NoEntityInteractGameTest : FabricClientGameTest {

    @Suppress("LongMethod")
    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleNoEntityInteract) }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                server.runCommand("gamemode survival @a")
                context.waitFor({ client -> client.player?.onGround() == true }, SETTLE_TICKS)
                val groundY = context.fromClient { client -> checkNotNull(client.player).y }
                server.runCommand("execute as @a run tp @s 0.5 $groundY 0.5 0 0")
                context.onClient {
                    ModuleNoEntityInteract.enabled = true
                    ModuleNoEntityInteract.settings.getValue("OnlyUse").setByString("true")
                }

                // OnlyUse: the main hand's right click goes past the armor stand, the attack still hits it
                reset(context, server, groundY, "armor_stand", mainHand = true)
                check(crosshairTargetsEntity(context)) { "OnlyUse moved the crosshair off the armor stand" }
                context.input.pressKey { options -> options.keyUse }
                context.waitTicks(SETTLE_USE_TICKS)
                check(helmetOn(server) == HelmetOn.PLAYER) { "OnlyUse let the right click reach the armor stand" }
                repeat(2) {
                    context.input.pressKey { options -> options.keyAttack }
                    context.waitTick()
                }
                context.waitTicks(SETTLE_USE_TICKS)
                check(server.computeOnServer<Boolean, RuntimeException> { minecraft ->
                    minecraft.overworld().allEntities.none { it is ArmorStand }
                }) { "OnlyUse kept the attack from breaking the armor stand" }

                // The offhand is only held back with Offhand
                reset(context, server, groundY, "armor_stand", mainHand = false)
                context.input.pressKey { options -> options.keyUse }
                context.waitTicks(SETTLE_USE_TICKS)
                check(helmetOn(server) == HelmetOn.ARMOR_STAND) { "The offhand was held back without Offhand" }

                context.onClient { ModuleNoEntityInteract.settings.getValue("Offhand").setByString("true") }
                reset(context, server, groundY, "armor_stand", mainHand = false)
                context.input.pressKey { options -> options.keyUse }
                context.waitTicks(SETTLE_USE_TICKS)
                check(helmetOn(server) == HelmetOn.PLAYER) { "Offhand let the offhand reach the armor stand" }

                // OnlyEnemies: an armor stand is no enemy, a husk is and stays skipped
                context.onClient { ModuleNoEntityInteract.settings.getValue("OnlyUse").setByString("false") }
                reset(context, server, groundY, "armor_stand", mainHand = true)
                check(!crosshairTargetsEntity(context)) { "The crosshair did not skip the armor stand" }
                context.onClient { ModuleNoEntityInteract.settings.getValue("OnlyEnemies").setByString("true") }
                context.waitTicks(SETTLE_USE_TICKS)
                check(crosshairTargetsEntity(context)) { "OnlyEnemies skipped an armor stand" }

                reset(context, server, groundY, "husk", mainHand = true)
                check(context.fromClient { client ->
                    checkNotNull(client.level).entitiesForRendering().any { it is Husk && it.shouldBeAttacked() }
                }) { "The husk is not an enemy" }
                check(!crosshairTargetsEntity(context)) { "OnlyEnemies did not skip an enemy" }
            }
        } finally {
            context.onClient {
                ModuleNoEntityInteract.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleNoEntityInteract, saved)
            }
        }
    }

    /**
     * Puts a fresh [entity] in front of the player and a helmet in the main or the off hand.
     */
    private fun reset(
        context: ClientGameTestContext,
        server: TestServerContext,
        groundY: Double,
        entity: String,
        mainHand: Boolean,
    ) {
        server.runCommand("kill @e[type=!player]")
        server.runCommand("clear @a")
        server.runCommand("summon $entity 0.5 $groundY 2.5 {NoAI:1b,NoGravity:1b,Silent:1b}")
        val hand = if (mainHand) "weapon.mainhand" else "weapon.offhand"
        server.runCommand("item replace entity @a $hand with diamond_helmet")
        if (!mainHand) {
            // A held item outside HoldingItems keeps the main hand's click off the armor stand
            server.runCommand("item replace entity @a weapon.mainhand with stick")
        }
        // Wait until only the new entity is left, the broken armor stand and its drop are gone
        context.waitFor({ client ->
            val player = checkNotNull(client.player)
            val others = checkNotNull(client.level).entitiesForRendering().filter { it !== player }
            (if (mainHand) player.mainHandItem else player.offhandItem).`is`(Items.DIAMOND_HELMET) &&
                others.size == 1 && BuiltInRegistries.ENTITY_TYPE.getKey(others.single().type).path == entity
        }, SETTLE_TICKS)
        context.waitTicks(SETTLE_USE_TICKS)
    }

    private fun crosshairTargetsEntity(context: ClientGameTestContext) =
        context.fromClient { client -> client.hitResult is EntityHitResult }

    private fun helmetOn(server: TestServerContext) = server.computeOnServer<HelmetOn, RuntimeException> { minecraft ->
        val player = minecraft.playerList.players.single()
        val stand = minecraft.overworld().allEntities.filterIsInstance<ArmorStand>().single()

        when {
            player.getItemBySlot(EquipmentSlot.HEAD).`is`(Items.DIAMOND_HELMET) -> HelmetOn.PLAYER
            stand.getItemBySlot(EquipmentSlot.HEAD).`is`(Items.DIAMOND_HELMET) -> HelmetOn.ARMOR_STAND
            else -> HelmetOn.NOWHERE
        }
    }

    private enum class HelmetOn { PLAYER, ARMOR_STAND, NOWHERE }

    private companion object {
        const val SETTLE_TICKS = 200
        const val SETTLE_USE_TICKS = 5
    }

}
