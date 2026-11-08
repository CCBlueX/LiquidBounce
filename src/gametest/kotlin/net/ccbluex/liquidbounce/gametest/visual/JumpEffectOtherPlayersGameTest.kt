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

import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.ModuleJumpEffect
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * [ModuleJumpEffect] below another player that jumps.
 */
class JumpEffectOtherPlayersGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.inVisualTestWorld {
            context.setValue("jumpEffect.otherPlayers", "true")
            context.setValue("jumpEffect.mode.simple.lifetime", "100..100")
            context.onClient { ModuleJumpEffect.enabled = true }

            val other = context.spawnFakePlayer("Jumper", 0.0, 3.0)
            context.onClient { other.setOnGround(true) }
            context.waitTicks(2)

            context.onClient {
                other.setPos(other.x, other.y + JUMP_HEIGHT, other.z)
                other.setOnGround(false)
            }
            context.waitTicks(2)

            val circles = context.fromClient { ModuleJumpEffect.circles.map { it.value } }
            check(circles.size == 1 && circles.single().distanceTo(other.position()) < 1.0) {
                "no jump circle below the other player: $circles"
            }

            context.input.lookAt(0f, LOOK_DOWN_PITCH)
            context.waitTicks(ANIMATION_TICKS)
            context.takeScreenshot("visual_jumpeffect_other_player")

            context.onClient {
                ModuleJumpEffect.enabled = false
                other.discard()
            }
        }
    }

    private companion object {
        const val JUMP_HEIGHT = 0.42
        const val ANIMATION_TICKS = 20
    }

}
