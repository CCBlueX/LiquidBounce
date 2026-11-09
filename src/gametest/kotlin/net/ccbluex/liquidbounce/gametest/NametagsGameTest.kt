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
import net.ccbluex.liquidbounce.features.module.modules.render.nametags.ModuleNametags
import net.ccbluex.liquidbounce.features.module.modules.render.nametags.NametagHealthBar
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.levelgen.Heightmap

/**
 * [ModuleNametags] only tags the nearest mobs when its tags are limited, and the health bar under a tag eases towards
 * a changed health over several ticks. Leaves a screenshot of the tags with their health bars.
 */
class NametagsGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleNametags) }

        try {
            context.worldBuilder().create().use { world ->
                val y = world.server.computeOnServer<Int, RuntimeException> {
                    it.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0)
                }
                buildStage(world.server, y)
                context.waitTicks(SETTLE_TICKS)

                context.onClient {
                    ModuleNametags.settings.getValue("MaxTags").setByString(LIMIT.toString())
                    NametagHealthBar.enabled = true
                    ModuleNametags.enabled = true
                }
                context.waitTicks(STEP_TICKS)

                check(context.taggedNames() == NAMES.take(LIMIT)) {
                    "limit $LIMIT tagged ${context.taggedNames()}, expected the nearest ${NAMES.take(LIMIT)}"
                }

                context.onClient { ModuleNametags.settings.getValue("MaxTags").setByString("0") }
                context.waitTicks(STEP_TICKS)
                check(context.taggedNames() == NAMES) { "no limit tagged ${context.taggedNames()}" }

                checkHealthEases(context, world.server)
            }
        } finally {
            context.onClient {
                ModuleNametags.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleNametags, saved)
            }
        }
    }

    private fun checkHealthEases(context: ClientGameTestContext, server: TestServerContext) {
        val nearest = context.fromClient { ModuleNametags.taggedEntities.first() as LivingEntity }
        val maxHealth = context.fromClient { nearest.maxHealth }
        val initial = context.fromClient { NametagHealthBar.displayedHealth(nearest) }
        check(initial == maxHealth) { "bar of a healthy mob shows $initial" }

        server.runCommand("data merge entity @e[type=husk,name=${NAMES.first()},limit=1] {Health:${LOW_HEALTH}f}")
        context.waitFor({ nearest.health == LOW_HEALTH }, STEP_TICKS)

        val shown = ArrayList<Float>()
        repeat(EASE_TICKS) {
            shown += checkNotNull(context.fromClient { NametagHealthBar.displayedHealth(nearest) }) {
                "bar of ${NAMES.first()} is gone"
            }
            context.waitTick()
        }

        check(shown.zipWithNext().all { (a, b) -> b <= a }) { "bar did not only go down: $shown" }
        check(shown.count { it > LOW_HEALTH && it < maxHealth } >= MIN_EASING_STEPS) {
            "bar jumped instead of easing: $shown"
        }
        check(shown.last() == LOW_HEALTH) { "bar did not reach $LOW_HEALTH: $shown" }

        context.takeScreenshot("nametags-health-bar")
    }

    private fun ClientGameTestContext.taggedNames() = fromClient {
        ModuleNametags.taggedEntities.map { it.customName?.string }
    }

    /**
     * Husks in a row in front of the player at 0 [y] 0 looking south, each further away than the one before.
     */
    private fun buildStage(server: TestServerContext, y: Int) {
        server.runCommand("gamerule advance_time false")
        server.runCommand("gamerule spawn_mobs false")
        server.runCommand("time set noon")
        server.runCommand("weather clear")
        server.runCommand("difficulty easy")
        server.runCommand("gamemode creative @a")

        for ((index, name) in NAMES.withIndex()) {
            server.runCommand(
                "summon husk ${if (index % 2 == 0) -0.5 else 1.5} $y ${3 + index * 2} " +
                    "{NoAI:1b,Silent:1b,PersistenceRequired:1b,CustomName:\"$name\",Rotation:[180f,0f]}"
            )
        }

        server.runCommand("tp @a 0.5 $y 0.5 0 12")
    }

    private companion object {
        val NAMES = listOf("Tag1", "Tag2", "Tag3", "Tag4", "Tag5")

        const val LIMIT = 3
        const val LOW_HEALTH = 4f
        const val MIN_EASING_STEPS = 3

        const val SETTLE_TICKS = 40
        const val STEP_TICKS = 10
        const val EASE_TICKS = 60
    }

}
