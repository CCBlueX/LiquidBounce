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
import net.ccbluex.liquidbounce.utils.render.readNativeImage
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs

/**
 * The glow around the ring [ModuleJumpEffect] leaves when the player jumps.
 */
class JumpEffectGlowGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.inVisualTestWorld {
            context.setValue("jumpEffect.mode.simple.lifetime", "100..100")
            context.setValue("jumpEffect.mode.simple.animCurve", "None")
            context.setValue("jumpEffect.mode.simple.endRadius", "0.4..1.2")
            context.setValue("jumpEffect.mode.simple.hueOffsetAnim", "0")
            context.setValue("jumpEffect.mode.simple.glow.size", "0.5")
            context.setValue("jumpEffect.mode.simple.glow.opacity", "1")
            context.onClient { ModuleJumpEffect.enabled = true }

            context.input.lookAt(0f, LOOK_AT_FEET_PITCH)
            context.input.holdKey { options -> options.keyJump }
            context.waitFor({ client -> !client.player().onGround() }, SETTLE_TIMEOUT_TICKS)
            context.input.releaseKey { options -> options.keyJump }
            context.waitFor({ client -> client.player().onGround() }, SETTLE_TIMEOUT_TICKS)
            check(context.fromClient { ModuleJumpEffect.circles.size } == 1) { "the jump left no circle" }
            context.waitTicks(SETTLE_TICKS)

            val plain = context.takeScreenshot("visual_jumpeffect_glow_plain")
            context.waitTicks(1)
            val plainAgain = context.takeScreenshot("visual_jumpeffect_glow_plain_again")

            context.setValue("jumpEffect.mode.simple.glow.enabled", "true")
            context.waitTicks(1)
            val glowing = context.takeScreenshot("visual_jumpeffect_glow_glow")

            context.onClient { ModuleJumpEffect.enabled = false }

            val unchanged = changedPixels(plain, plainAgain)
            val glow = changedPixels(plainAgain, glowing)
            check(glow > MIN_GLOW_PIXELS && glow > unchanged * 10) {
                "the glow changed $glow pixels, a tick without it $unchanged"
            }
        }
    }

    private fun changedPixels(first: Path, second: Path): Int {
        val a = centerPixels(first)
        val b = centerPixels(second)
        return a.indices.count { differs(a[it], b[it]) }
    }

    /**
     * The middle of the screen around the ring, clear of the HUD, chat and notifications.
     */
    private fun centerPixels(screenshot: Path) = Files.newInputStream(screenshot).use { it.readNativeImage() }.use {
        val width = it.width / 3
        val height = it.height / 2
        IntArray(width * height) { i -> it.getPixel(width + i % width, height / 2 + i / width) }
    }

    private fun differs(first: Int, second: Int) = (0 until 24 step 8).any { shift ->
        abs((first shr shift and 0xFF) - (second shr shift and 0xFF)) > CHANNEL_THRESHOLD
    }

    private companion object {
        const val LOOK_AT_FEET_PITCH = 80f
        const val SETTLE_TICKS = 10
        const val CHANNEL_THRESHOLD = 24
        const val MIN_GLOW_PIXELS = 2000
    }

}
