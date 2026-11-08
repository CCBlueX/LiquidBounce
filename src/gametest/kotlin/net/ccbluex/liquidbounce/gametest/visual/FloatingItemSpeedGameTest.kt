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
package net.ccbluex.liquidbounce.gametest.visual

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue
import net.ccbluex.liquidbounce.features.module.modules.render.DoRender
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * A totem popped by lethal damage plays its animation faster with [ModuleAntiBlind].
 */
class FloatingItemSpeedGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.inVisualTestWorld { world ->
            @Suppress("UNCHECKED_CAST")
            val render = context.fromClient {
                checkNotNull(ConfigSystem.findValueByKey("liquidbounce.modules.antiBlind.doRender"))
            } as MultiChoiceListValue<DoRender>
            val hidesFloatingItems = DoRender.FLOATING_ITEMS !in render

            context.onClient {
                ModuleAntiBlind.enabled = true
                if (hidesFloatingItems) {
                    render.toggle(DoRender.FLOATING_ITEMS)
                }
            }
            context.setValue("antiBlind.floatingItemSpeed", SPEED.toString())

            world.server.runCommand("gamemode survival @a")
            world.server.runCommand("item replace entity @a weapon.offhand with minecraft:totem_of_undying")
            world.server.runCommand("damage @p 1000 minecraft:generic")

            context.waitFor({ client -> client.player().itemActivation().isActive }, SETTLE_TIMEOUT_TICKS)
            context.waitTicks(2)
            context.takeScreenshot("visual_antiblind_totem")
            context.waitTicks(3)

            val left = context.fromClient { client -> client.player().itemActivation().ticks() }
            check(left <= FAST_TOTEM_TICKS_LEFT) { "the totem animation has $left ticks left" }

            context.setValue("antiBlind.floatingItemSpeed", "1")
            context.onClient {
                if (hidesFloatingItems) {
                    render.toggle(DoRender.FLOATING_ITEMS)
                }
                ModuleAntiBlind.enabled = false
            }
        }
    }

    private companion object {
        const val SPEED = 4

        // 40 ticks at 4 per tick: at most 20 left 5 ticks after the pop, where the vanilla animation has 35
        const val FAST_TOTEM_TICKS_LEFT = 24
    }

}
