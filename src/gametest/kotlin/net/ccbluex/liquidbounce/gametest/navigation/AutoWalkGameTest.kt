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
import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.fill
import net.ccbluex.gametest.harness.action.give
import net.ccbluex.gametest.harness.action.setBlock
import net.ccbluex.gametest.harness.action.summon
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFightBot
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.features.module.modules.world.autofarm.AutoFarmAutoWalk
import net.ccbluex.liquidbounce.features.module.modules.world.autofarm.ModuleAutoFarm
import net.ccbluex.liquidbounce.features.module.modules.world.fucker.FuckerAutoWalk
import net.ccbluex.liquidbounce.features.module.modules.world.fucker.ModuleFucker
import net.ccbluex.liquidbounce.gametest.command
import net.ccbluex.liquidbounce.gametest.fillArea
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import net.ccbluex.liquidbounce.gametest.matchingBlocks
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.playerPosition
import net.ccbluex.liquidbounce.gametest.restoring
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3

private const val DIRECTION_TICKS = 600
private const val DIRECTION_END = 38.0
private const val STILL = 0.02
private const val MAX_STOPS = 3
private const val SPRINT_TICKS = 200
private const val FOLLOW_TICKS = 300
private const val FOLLOW_DISTANCE = 3.5
private const val FIGHT_TICKS = 400
private const val FUCKER_TICKS = 800

/**
 * AutoWalk walks the way the player faces around a wall, to a `.goto` destination around a closed room and up to a
 * `.follow` player, AutoFarm walks onto an item to pick it up, Fucker's AutoWalk bridges over to a bed and then a
 * dragon egg and breaks both, and KillAura's FightBot walks around a fence to hit a husk.
 */
class AutoWalkGameTest : LiquidBounceGameTest({
    context.restoring(ModuleAutoWalk, ModuleAutoFarm, ModuleFucker, ModuleKillAura) {
        val y = origin.y
        buildCourse(y)
        give("minecraft:diamond_pickaxe")
        give("minecraft:cobblestone", 64)

        checkDirection(y)
        checkGoto(y)
        checkSprint(y)
        checkFollow(y)
        checkFarmItem(y)
        checkFuckerTargets(y)
        checkFightBot(y)
    }
})

/**
 * A wall at x 10, a closed stone room at x 22..28 and three floating islands at x 32..34, 40..42 and 48..50 with
 * a bed on the second and a dragon egg on the third. Along z -18..-12 runs an uneven lane: a hill four blocks high
 * with a pillar on top, a ditch, a wall with a copy far above to compare it with, and slabs. The ground floor is
 * stone at y - 1.
 */
private fun GameTestScope.buildCourse(y: Int) {
    fillArea(BlockPos(0, y, -20), BlockPos(60, y + 14, 20), "minecraft:air")
    fill(BlockPos(0, y - 1, -20), BlockPos(60, y - 1, 20), "minecraft:stone")
    fill(BlockPos(10, y, -6), BlockPos(10, y + 3, 6), "minecraft:stone")
    fill(BlockPos(22, y, -3), BlockPos(28, y + 3, 3), "minecraft:stone hollow")
    fill(BlockPos(32, y + 8, -1), BlockPos(34, y + 8, 1), "minecraft:stone")
    fill(BlockPos(40, y + 8, -1), BlockPos(42, y + 8, 1), "minecraft:stone")
    setBlock(BlockPos(41, y + 9, 0), "minecraft:red_bed[part=foot,facing=east]")
    setBlock(BlockPos(42, y + 9, 0), "minecraft:red_bed[part=head,facing=east]")
    fill(BlockPos(48, y + 8, -1), BlockPos(50, y + 8, 1), "minecraft:stone")
    setBlock(BlockPos(49, y + 9, 0), "minecraft:dragon_egg")

    for (step in 0..3) {
        fill(BlockPos(6 + step, y + step, -18), BlockPos(20 - step, y + step, -12), "minecraft:stone")
    }
    fill(BlockPos(13, y + 4, -16), BlockPos(13, y + 6, -14), "minecraft:stone")
    fill(BlockPos(23, y - 1, -18), BlockPos(24, y - 1, -12), "minecraft:air")
    fill(BlockPos(27, y, -20), BlockPos(27, y + 2, -11), "minecraft:stone")
    fill(BlockPos(27, y + 40, -20), BlockPos(27, y + 42, -11), "minecraft:stone")
    fill(BlockPos(31, y, -18), BlockPos(33, y, -12), "minecraft:stone_slab")
    awaitServer("block 33 $y -12 minecraft:stone_slab")
}

/** Bumping into a stair while jumping up the next is how the game moves; only standing on ground is a stop. */
private fun GameTestScope.checkDirection(y: Int) {
    moveTo(Vec3(2.5, y.toDouble(), -15.5), yaw = -90f)
    context.onClient {
        ModuleAutoWalk.containedValues.first { it.name == "Mode" }.setByString("Direction")
        ModuleAutoWalk.enabled = true
    }

    var stops = 0
    watched("walking the way it faces") {
        var last = context.playerPosition()
        repeat(DIRECTION_TICKS) {
            ticks(1)
            val (position, onGround) = context.fromClient { mc ->
                val player = checkNotNull(mc.player)
                player.position() to player.onGround()
            }
            if (onGround && position.subtract(last).horizontalDistance() < STILL) {
                stops++
            }
            last = position
            if (position.x > DIRECTION_END) {
                return@watched
            }
        }
        error("Did not walk the lane within $DIRECTION_TICKS ticks, stopped at $last")
    }
    context.onClient { ModuleAutoWalk.enabled = false }
    check(stops <= MAX_STOPS) { "Stood still on the ground for $stops ticks" }
    check(matchingBlocks(BlockPos(27, y, -20), BlockPos(27, y + 2, -11), BlockPos(27, y + 40, -20)) == 10 * 3) {
        "The wall was changed"
    }
}

private fun GameTestScope.checkGoto(y: Int) {
    moveTo(Vec3(18.5, y.toDouble(), 0.5))
    val destination = Vec3(31.5, y.toDouble(), 0.5)
    context.command("goto ${destination.x} ${destination.y} ${destination.z}")
    check(context.fromClient { ModuleAutoWalk.enabled }) { ".goto did not turn AutoWalk on" }

    watched("going around the room") {
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
private fun GameTestScope.checkSprint(y: Int) {
    moveTo(Vec3(2.5, y.toDouble(), 16.5), yaw = 90f)
    context.command("goto 20 $y 16")

    var elapsed = 0
    var sprinting = 0
    watched("sprinting along") {
        repeat(SPRINT_TICKS) {
            ticks(1)
            elapsed++
            if (context.fromClient { checkNotNull(it.player).isSprinting }) {
                sprinting++
            }
            if (context.fromClient { !ModuleAutoWalk.enabled }) {
                return@watched
            }
        }
        error("Did not arrive within $SPRINT_TICKS ticks, stopped at ${context.playerPosition()}")
    }
    check(sprinting * 2 >= elapsed) { "Sprinted for only $sprinting of $elapsed ticks" }
}

/** `.follow` walks up to a player the tab list does not show. */
private fun GameTestScope.checkFollow(y: Int) {
    val twin = Vec3(16.5, y.toDouble(), 8.5)
    moveTo(twin)
    context.command("fakeplayer spawn Twin")
    moveTo(Vec3(2.5, y.toDouble(), 8.5))

    try {
        context.command("follow Twin")
        check(context.fromClient { ModuleAutoWalk.enabled }) { ".follow did not turn AutoWalk on" }
        watched("following") {
            awaitPlayer(context, FOLLOW_TICKS, "Did not walk up to the followed player") {
                it.position().distanceTo(twin) <= FOLLOW_DISTANCE
            }
        }
    } finally {
        context.onClient { ModuleAutoWalk.enabled = false }
        context.command("fakeplayer clear")
    }
}

private fun GameTestScope.checkFarmItem(y: Int) {
    moveTo(Vec3(4.5, y.toDouble(), 12.5))
    summon(
        "minecraft:item",
        BlockPos(7, y, 12),
        """{Item:{id:"minecraft:wheat_seeds",count:1},PickupDelay:0s,Motion:[0d,0d,0d]}""",
    )
    context.onClient {
        AutoFarmAutoWalk.enabled = true
        ModuleAutoFarm.enabled = true
    }

    try {
        watched("walking to an item") {
            awaitPlayer(context, 200, "AutoFarm did not pick up the item") {
                it.inventory.hasAnyOf(setOf(Items.WHEAT_SEEDS))
            }
        }
    } finally {
        context.onClient { ModuleAutoFarm.enabled = false }
    }
}

private fun GameTestScope.checkFuckerTargets(y: Int) {
    moveTo(Vec3(33.5, y + 9.0, 0.5))
    context.onClient {
        FuckerAutoWalk.enabled = true
        ModuleFucker.enabled = true
    }

    try {
        // The bed first, being closer; hitting the egg teleports it away. Fucker aims at what it mines.
        val acting = { FuckerAutoWalk.isActing || ModuleFucker.hasTarget }
        watched("walking to the bed", acting) {
            awaitServer("block 41 ${y + 9} 0 minecraft:air", FUCKER_TICKS)
            awaitServer("block 42 ${y + 9} 0 minecraft:air", FUCKER_TICKS)
        }
        watched("walking to the dragon egg", acting) {
            awaitServer("block 49 ${y + 9} 0 minecraft:air", FUCKER_TICKS)
        }
    } finally {
        context.onClient { ModuleFucker.enabled = false }
        fill(BlockPos(30, y - 1, -20), BlockPos(60, y + 14, 20), "minecraft:air replace minecraft:dragon_egg")
    }
}

/** Far from the husk, FightBot walks up to it instead of keeping away while the weapon recharges. */
private fun GameTestScope.checkFightBot(y: Int) {
    // Peaceful has no husks
    run("difficulty easy")
    fill(BlockPos(50, y, 4), BlockPos(50, y, 12), "minecraft:oak_fence")
    summon("minecraft:husk", BlockPos(56, y, 8), "{NoAI:1b,PersistenceRequired:1b}")
    moveTo(Vec3(44.5, y.toDouble(), 8.5))
    context.onClient {
        // Anticheats allow no more than the reach of the game
        ModuleKillAura.range.containedValues.first { it.name == "RangeIncrease" }.setByString("0")
        KillAuraFightBot.enabled = true
        ModuleKillAura.enabled = true
    }

    try {
        awaitHuskHurt(FIGHT_TICKS)
    } finally {
        context.onClient { ModuleKillAura.enabled = false }
        run("kill @e[type=minecraft:husk]")
    }
}

/** Runs [block] unhurt and without glancing back. */
private inline fun GameTestScope.watched(
    what: String,
    noinline acting: () -> Boolean = { false },
    crossinline block: () -> Unit,
) = unhurt("AutoWalk $what") { TurnWatch.watch(context, "AutoWalk $what", acting) { block() } }
