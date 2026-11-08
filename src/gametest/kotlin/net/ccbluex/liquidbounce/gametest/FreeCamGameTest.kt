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
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.KeyMapping
import net.minecraft.client.Options
import kotlin.math.abs

/**
 * [ModuleFreeCam] flies SprintMultiplier times as fast while sprint is held.
 */
class FreeCamGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleFreeCam) }

        try {
            context.worldBuilder().create().use { world ->
                world.server.runCommand("gamemode survival @a")
                context.waitFor({ client -> client.player?.onGround() == true }, SETTLE_TICKS)
                context.onClient {
                    ModuleFreeCam.settings.getValue("SprintMultiplier").setByString("$SPRINT_MULTIPLIER")
                    ModuleFreeCam.enabled = true
                }

                val walking = fastestStep(context, { it.keyUp })
                val sprinting = fastestStep(context, { it.keyUp }, { it.keySprint })
                check(abs(walking - SPEED) < TOLERANCE) { "The camera flew $walking blocks a tick" }
                check(abs(sprinting - SPEED * SPRINT_MULTIPLIER) < TOLERANCE) {
                    "The camera flew $sprinting blocks a tick with sprint held"
                }
            }
        } finally {
            context.onClient {
                ModuleFreeCam.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleFreeCam, saved)
            }
        }
    }

    /**
     * Holds [keys] for a few ticks and returns the largest distance the camera covered in one tick.
     */
    private fun fastestStep(context: ClientGameTestContext, vararg keys: (Options) -> KeyMapping): Double {
        keys.forEach { key -> context.input.holdKey { options -> key(options) } }
        var fastest = 0.0

        try {
            repeat(FLY_TICKS) {
                val before = context.fromClient { ModuleFreeCam.PositionState.pos }
                context.waitTick()
                val after = context.fromClient { ModuleFreeCam.PositionState.pos }
                fastest = maxOf(fastest, after.distanceTo(before))
            }
        } finally {
            keys.forEach { key -> context.input.releaseKey { options -> key(options) } }
        }

        return fastest
    }

    private companion object {
        const val SPEED = 1.0
        const val SPRINT_MULTIPLIER = 3.0
        const val TOLERANCE = 0.01

        const val SETTLE_TICKS = 200
        const val FLY_TICKS = 4
    }

}
