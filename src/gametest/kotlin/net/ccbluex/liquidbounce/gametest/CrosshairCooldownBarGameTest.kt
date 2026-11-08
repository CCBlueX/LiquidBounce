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
import net.ccbluex.liquidbounce.features.module.modules.render.crosshair.ModuleCrosshair
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.world.entity.monster.zombie.Husk
import net.minecraft.world.level.levelgen.Heightmap

/**
 * The cooldown bar of [ModuleCrosshair] shows the attack cooldown and its ready flash, the item cooldown in either
 * hand and the bow charge; a bow let go before full charge must not flash ready.
 */
class CrosshairCooldownBarGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleCrosshair) }

        try {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val y = server.computeOnServer<Int, RuntimeException> {
                    it.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0)
                }
                server.runCommand("gamerule spawn_mobs false")
                server.runCommand("kill @e[type=!player]")
                server.runCommand("fill -12 $y -12 24 ${y + 10} 12 air")
                server.runCommand("fill -12 ${y - 1} -12 24 ${y - 1} 12 smooth_stone")
                server.runCommand("gamemode creative @a")
                server.runCommand("tp @a 0.5 $y 0.5 -90 0")
                world.connection.waitForChunksRender()

                context.onClient {
                    ModuleCrosshair.CooldownBar.enabled = true
                    ModuleCrosshair.enabled = true
                }

                checkAttack(context, server, y)
                checkItemCooldowns(context, server)
                checkBow(context, server)
            }
        } finally {
            context.input.releaseKey { it.keyUse }
            context.onClient {
                ModuleCrosshair.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleCrosshair, saved)
            }
        }
    }

    private fun checkAttack(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        server.runCommand("summon husk 2.5 $y 0.5 {NoAI:1b,PersistenceRequired:1b,Silent:1b}")
        server.runCommand("item replace entity @a weapon.mainhand with diamond_sword")
        context.waitFor({ it.crosshairPickEntity is Husk }, TIMEOUT_TICKS)
        context.waitTicks(SETTLE_TICKS)

        context.onClient { mc ->
            val player = checkNotNull(mc.player)
            checkNotNull(mc.gameMode).attack(player, checkNotNull(mc.crosshairPickEntity))
        }
        context.waitTicks(1)
        val attack = context.fromClient { ModuleCrosshair.CooldownBar.progress(0f) }
        check(attack != null && attack < 1f) { "The cooldown bar does not show the attack cooldown: $attack" }
        context.takeScreenshot("crosshair-attack-cooldown")

        context.waitFor({ ModuleCrosshair.CooldownBar.flash > 0f }, TIMEOUT_TICKS)
        context.takeScreenshot("crosshair-ready-flash")
        server.runCommand("kill @e[type=husk]")
    }

    private fun checkItemCooldowns(context: ClientGameTestContext, server: TestServerContext) {
        for (slot in listOf("weapon.mainhand", "weapon.offhand")) {
            server.runCommand("item replace entity @a weapon.mainhand with air")
            server.runCommand("item replace entity @a weapon.offhand with air")
            server.runCommand("item replace entity @a $slot with ender_pearl")
            context.waitTicks(SETTLE_TICKS)
            server.computeOnServer<Unit, RuntimeException> {
                val player = it.playerList.players.first()
                val stack = if (slot == "weapon.mainhand") player.mainHandItem else player.offhandItem
                player.cooldowns.addCooldown(stack, COOLDOWN_TICKS)
            }
            context.waitTicks(SETTLE_TICKS)
            val pearl = context.fromClient { ModuleCrosshair.CooldownBar.progress(0f) }
            check(pearl != null && pearl < 1f) { "The cooldown bar does not show the pearl cooldown in $slot: $pearl" }
        }
        server.runCommand("item replace entity @a weapon.offhand with air")
    }

    private fun checkBow(context: ClientGameTestContext, server: TestServerContext) {
        server.runCommand("item replace entity @a weapon.mainhand with bow")
        server.runCommand("item replace entity @a weapon.offhand with arrow 64")
        context.waitTicks(COOLDOWN_TICKS)

        context.input.holdKey { it.keyUse }
        context.waitTicks(PARTIAL_CHARGE_TICKS)
        val partial = context.fromClient { ModuleCrosshair.CooldownBar.progress(0f) }
        check(partial != null && partial > 0f && partial < 1f) { "The bow is not partly drawn: $partial" }
        context.takeScreenshot("crosshair-bow-charge")
        context.input.releaseKey { it.keyUse }

        var partialFlash = 0f
        repeat(FLASH_WATCH_TICKS) {
            context.waitTicks(1)
            partialFlash = maxOf(partialFlash, context.fromClient { ModuleCrosshair.CooldownBar.flash })
        }
        check(partialFlash == 0f) { "A bow let go before full charge flashed ready: $partialFlash" }

        context.input.holdKey { it.keyUse }
        context.waitFor({ ModuleCrosshair.CooldownBar.flash > 0f }, TIMEOUT_TICKS)
        context.input.releaseKey { it.keyUse }
    }

    private companion object {
        const val SETTLE_TICKS = 5
        const val TIMEOUT_TICKS = 60
        const val COOLDOWN_TICKS = 100
        const val PARTIAL_CHARGE_TICKS = 15
        const val FLASH_WATCH_TICKS = 10
    }

}
