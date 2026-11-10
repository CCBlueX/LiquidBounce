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

package net.ccbluex.liquidbounce.gametest.navigation

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFightBot
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.features.module.modules.world.autofarm.AutoFarmAutoWalk
import net.ccbluex.liquidbounce.features.module.modules.world.autofarm.ModuleAutoFarm
import net.ccbluex.liquidbounce.features.module.modules.world.fucker.FuckerAutoWalk
import net.ccbluex.liquidbounce.features.module.modules.world.fucker.ModuleFucker
import net.ccbluex.liquidbounce.gametest.command
import net.ccbluex.liquidbounce.gametest.countBlocks
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.groundY
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.playerPosition
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3

/**
 * AutoWalk walks the way the player faces around a wall, to a `.goto` destination around a closed room and up to a
 * `.follow` player, AutoFarm walks onto an item to pick it up, Fucker's AutoWalk bridges over to a bed and then a
 * dragon egg and breaks both, and KillAura's FightBot walks around a fence to hit a husk.
 */
@Suppress("TooManyFunctions")
class AutoWalkGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        if (!navigationGameTests) {
            return
        }
        context.waitForClient()
        context.restoring(ModuleAutoWalk, ModuleAutoFarm, ModuleFucker, ModuleKillAura) {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val y = server.groundY()
                buildCourse(server, y)
                server.runCommand("gamemode survival @a")
                server.runCommand("clear @a")
                server.runCommand("give @a diamond_pickaxe")
                server.runCommand("give @a cobblestone 64")

                checkDirection(context, server, y)
                checkGoto(context, server, y)
                checkSprint(context, server, y)
                checkFollow(context, server, y)
                checkFarmItem(context, server, y)
                checkFuckerTargets(context, server, y)
                checkFightBot(context, server, y)
            }
        }
    }

    /**
     * A wall at x 10, a closed stone room at x 22..28 and three floating islands at x 32..34, 40..42 and 48..50 with
     * a bed on the second and a dragon egg on the third. Along z -18..-12 runs an uneven lane: a hill four blocks high
     * with a pillar on top, a ditch, a wall and slabs. The ground floor is stone at y - 1.
     */
    private fun buildCourse(server: TestServerContext, y: Int) {
        server.runCommand("fill 0 $y -20 60 ${y + 14} 20 air")
        server.runCommand("fill 0 ${y - 1} -20 60 ${y - 1} 20 stone")
        server.runCommand("fill 10 $y -6 10 ${y + 3} 6 stone")
        server.runCommand("fill 22 $y -3 28 ${y + 3} 3 stone hollow")
        server.runCommand("fill 32 ${y + 8} -1 34 ${y + 8} 1 stone")
        server.runCommand("fill 40 ${y + 8} -1 42 ${y + 8} 1 stone")
        server.runCommand("setblock 41 ${y + 9} 0 red_bed[part=foot,facing=east]")
        server.runCommand("setblock 42 ${y + 9} 0 red_bed[part=head,facing=east]")
        server.runCommand("fill 48 ${y + 8} -1 50 ${y + 8} 1 stone")
        server.runCommand("setblock 49 ${y + 9} 0 dragon_egg")

        for (step in 0..3) {
            server.runCommand("fill ${6 + step} ${y + step} -18 ${20 - step} ${y + step} -12 stone")
        }
        server.runCommand("fill 13 ${y + 4} -16 13 ${y + 6} -14 stone")
        server.runCommand("fill 23 ${y - 1} -18 24 ${y - 1} -12 air")
        server.runCommand("fill 27 $y -20 27 ${y + 2} -11 stone")
        server.runCommand("fill 31 $y -18 33 $y -12 stone_slab")
    }

    /** Bumping into a stair while jumping up the next is how the game moves; only standing on ground is a stop. */
    private fun checkDirection(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(2.5, y.toDouble(), -15.5), yaw = -90f)
        context.onClient {
            ModuleAutoWalk.containedValues.first { it.name == "Mode" }.setByString("Direction")
            ModuleAutoWalk.enabled = true
        }

        var stops = 0
        unhurt(context, server, "walking the way it faces") {
            var last = context.playerPosition()
            repeat(DIRECTION_TICKS) {
                context.waitTick()
                val (position, onGround) = context.fromClient { mc ->
                    val player = checkNotNull(mc.player)
                    player.position() to player.onGround()
                }
                if (onGround && position.subtract(last).horizontalDistance() < STILL) {
                    stops++
                }
                last = position
                if (position.x > DIRECTION_END) {
                    return@unhurt
                }
            }
            error("Did not walk the lane within $DIRECTION_TICKS ticks, stopped at $last")
        }
        context.onClient { ModuleAutoWalk.enabled = false }
        check(stops <= MAX_STOPS) { "Stood still on the ground for $stops ticks" }
        check(server.countBlocks(BlockPos(27, y, -20), BlockPos(27, y + 2, -11)) == 10 * 3) { "The wall was changed" }
    }

    private fun checkGoto(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(18.5, y.toDouble(), 0.5), yaw = 0f)
        val destination = Vec3(31.5, y.toDouble(), 0.5)
        context.command("goto ${destination.x} ${destination.y} ${destination.z}")
        check(context.fromClient { ModuleAutoWalk.enabled }) { ".goto did not turn AutoWalk on" }

        unhurt(context, server, "going around the room") {
            awaitPlayer(context, 600, "Did not arrive behind the closed room") { !ModuleAutoWalk.enabled }
        }
        val position = context.playerPosition()
        check(position.distanceTo(destination) <= 1.1) { "AutoWalk turned off at $position, not at $destination" }

        val loaded = context.fromClient {
            val json = ConfigSystem.serializeValueGroup(ModuleAutoWalk)
            val saved = checkNotNull(json.destination()) { "No Destination in the AutoWalk config" }
            saved.addProperty("x", saved["x"].asInt + 1)
            ConfigSystem.deserializeValueGroup(ModuleAutoWalk, json)
            ConfigSystem.serializeValueGroup(ModuleAutoWalk).destination()?.get("x")?.asInt
        }
        check(loaded == BlockPos.containing(destination).x + 1) { "A config did not load the Goto destination" }
    }

    /** The value object of the `Destination` setting somewhere in [this] config. */
    private fun JsonElement.destination(): JsonObject? = when {
        isJsonArray -> asJsonArray.firstNotNullOfOrNull { it.destination() }
        !isJsonObject -> null
        asJsonObject["name"]?.takeIf { it.isJsonPrimitive }?.asString == "Destination" ->
            asJsonObject["value"]?.takeIf { it.isJsonObject }?.asJsonObject
        else -> asJsonObject.entrySet().firstNotNullOfOrNull { it.value.destination() }
    }

    /** Walking away from where the player looks still sprints, as the walk turns the head. */
    private fun checkSprint(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(2.5, y.toDouble(), 16.5), yaw = 90f)
        context.command("goto 20 $y 16")

        var ticks = 0
        var sprinting = 0
        unhurt(context, server, "sprinting along") {
            repeat(SPRINT_TICKS) {
                context.waitTick()
                ticks++
                if (context.fromClient { checkNotNull(it.player).isSprinting }) {
                    sprinting++
                }
                if (context.fromClient { !ModuleAutoWalk.enabled }) {
                    return@unhurt
                }
            }
            error("Did not arrive within $SPRINT_TICKS ticks, stopped at ${context.playerPosition()}")
        }
        check(sprinting * 2 >= ticks) { "Sprinted for only $sprinting of $ticks ticks" }
    }

    /** `.follow` walks up to a player the tab list does not show. */
    private fun checkFollow(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        val twin = Vec3(16.5, y.toDouble(), 8.5)
        moveTo(context, server, twin, yaw = 0f)
        context.command("fakeplayer spawn Twin")
        moveTo(context, server, Vec3(2.5, y.toDouble(), 8.5), yaw = 0f)

        try {
            context.command("follow Twin")
            check(context.fromClient { ModuleAutoWalk.enabled }) { ".follow did not turn AutoWalk on" }
            unhurt(context, server, "following") {
                awaitPlayer(context, FOLLOW_TICKS, "Did not walk up to the followed player") {
                    it.position().distanceTo(twin) <= FOLLOW_DISTANCE
                }
            }
        } finally {
            context.onClient { ModuleAutoWalk.enabled = false }
            context.command("fakeplayer clear")
        }
    }

    private fun checkFarmItem(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(4.5, y.toDouble(), 12.5), yaw = 0f)
        server.runCommand(
            "summon item 7.5 $y 12.5 {Item:{id:\"minecraft:wheat_seeds\",count:1},PickupDelay:0s,Motion:[0d,0d,0d]}"
        )
        context.onClient {
            AutoFarmAutoWalk.enabled = true
            ModuleAutoFarm.enabled = true
        }

        try {
            unhurt(context, server, "walking to an item") {
                awaitPlayer(context, 200, "AutoFarm did not pick up the item") {
                    it.inventory.hasAnyOf(setOf(Items.WHEAT_SEEDS))
                }
            }
        } finally {
            context.onClient { ModuleAutoFarm.enabled = false }
        }
    }

    private fun checkFuckerTargets(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(33.5, y + 9.0, 0.5), yaw = 0f)
        context.onClient {
            FuckerAutoWalk.enabled = true
            ModuleFucker.enabled = true
        }

        try {
            // The bed first, being closer; hitting the egg teleports it away.
            for ((target, from, to) in listOf(
                Triple("bed", BlockPos(41, y + 9, 0), BlockPos(42, y + 9, 0)),
                Triple("dragon egg", BlockPos(49, y + 9, 0), BlockPos(49, y + 9, 0)),
            )) {
                unhurt(context, server, "walking to the $target", acting = { FuckerAutoWalk.isActing }) {
                    await(context, 800, "The $target was not hit") { server.countBlocks(from, to) == 0 }
                }
            }
        } finally {
            context.onClient { ModuleFucker.enabled = false }
            server.runCommand("fill 30 ${y - 1} -20 60 ${y + 14} 20 air replace dragon_egg")
        }
    }

    /** Far from the husk, FightBot walks up to it instead of keeping away while the weapon recharges. */
    private fun checkFightBot(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        server.runCommand("fill 50 $y 4 50 $y 12 oak_fence")
        server.runCommand("summon husk 56.5 $y 8.5 {NoAI:1b,PersistenceRequired:1b}")
        moveTo(context, server, Vec3(44.5, y.toDouble(), 8.5), yaw = 0f)
        context.onClient {
            KillAuraFightBot.enabled = true
            ModuleKillAura.enabled = true
        }

        try {
            await(context, FIGHT_TICKS, "FightBot did not hit the husk") { server.huskHurt() }
        } finally {
            context.onClient { ModuleKillAura.enabled = false }
            server.runCommand("kill @e[type=husk]")
        }
    }

    private fun unhurt(
        context: ClientGameTestContext,
        server: TestServerContext,
        what: String,
        acting: () -> Boolean = { false },
        block: () -> Unit,
    ) {
        unhurt(server, what) { TurnWatch.watch(context, "AutoWalk $what", acting, block) }
    }

    private companion object {
        const val DIRECTION_TICKS = 600
        const val DIRECTION_END = 38.0
        const val STILL = 0.02
        const val MAX_STOPS = 3
        const val SPRINT_TICKS = 200
        const val FOLLOW_TICKS = 300
        const val FOLLOW_DISTANCE = 3.5
        const val FIGHT_TICKS = 400
    }

}
