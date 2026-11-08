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
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBreadcrumbs
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * The footstep, ring and particle marks [ModuleBreadcrumbs] leaves while walking.
 */
class BreadcrumbsModesGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleBreadcrumbs) }

        try {
            context.inVisualTestWorld {
                context.setValue("breadcrumbs.temporary.alive", "10000")
                context.setValue("breadcrumbs.size", "0.5")
                context.onClient { ModuleBreadcrumbs.enabled = true }
                // Walking backwards to the east keeps the marks in view
                context.input.lookAt(90f, LOOK_DOWN_PITCH)

                for (mode in arrayOf("Footsteps", "Rings", "Particles")) {
                    context.setValue("breadcrumbs.mode", mode)
                    context.input.holdKeyFor({ it.keyDown }, WALK_TICKS)

                    val marks = context.fromClient { ModuleBreadcrumbs.markCount }
                    check(marks >= MIN_MARKS) { "only $marks ${mode.lowercase()} after walking" }
                    context.takeScreenshot("visual_breadcrumbs_${mode.lowercase()}")
                }

                // The marks lie on the ground, so none are laid while falling
                context.onClient { client ->
                    val player = client.player()
                    player.setPos(player.x, player.y + AIR_HEIGHT, player.z)
                    player.setOnGround(false)
                }
                context.waitTick()
                val grounded = context.fromClient { ModuleBreadcrumbs.markCount }
                context.input.holdKey { it.keyDown }
                repeat(AIR_TICKS) {
                    context.waitTick()
                    check(context.fromClient { !it.player().onGround() }) { "the player landed too early" }
                }
                context.input.releaseKey { it.keyDown }
                val airborne = context.fromClient { ModuleBreadcrumbs.markCount }
                check(airborne == grounded) { "${airborne - grounded} marks laid in the air" }
                context.waitFor({ client -> client.player?.onGround() == true }, SETTLE_TIMEOUT_TICKS)
            }
        } finally {
            context.onClient {
                ModuleBreadcrumbs.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleBreadcrumbs, saved)
            }
        }
    }

    private companion object {
        const val WALK_TICKS = 20
        const val MIN_MARKS = 3
        const val AIR_HEIGHT = 5.0
        const val AIR_TICKS = 8
    }

}
