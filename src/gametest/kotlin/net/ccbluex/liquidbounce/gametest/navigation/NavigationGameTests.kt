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

import net.ccbluex.gametest.harness.Anticheat
import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.connection.GameTestEnvironment
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.gametest.command
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.health
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.playerPosition
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.navigation.NavigationBaseValueGroup
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/** `-PgameTestNavigation=false` skips the navigation game tests, which take minutes. */
private val navigationGameTests = System.getProperty("net.ccbluex.liquidbounce.gametest.navigation") != "false"

/** The navigation game tests, one entrypoint so that they can be skipped together. */
class NavigationGameTests : FabricClientGameTest {

    /**
     * The teleport engine moves the player further per packet than walking could. Every anticheat catches that,
     * Paper's own checks are what it is made for.
     */
    private val paperOnly = listOf(
        TeleportCommandGameTest(),
        TeleportRouteGameTest(),
        TeleportReachGameTest(),
    )

    private val tests = listOf(
        WalkNavigationGameTest(),
        AutoWalkGameTest(),
        AutoWalkStagesGameTest(),
        WalkParkourGameTest(),
        ParkourAssistGameTest(),
        ParkourEdgeGameTest(),
        WalkSneakGameTest(),
        WalkSwimGameTest(),
        WalkBlockedGameTest(),
        PlayerMotionGameTest(),
        FlightNavigationGameTest(),
        RideNavigationGameTest(),
        ElytraTravelGameTest(),
    )

    override fun runTest(context: ClientGameTestContext) {
        if (!navigationGameTests) {
            return
        }
        // Server game tests report at the end of the run; a local one that fails must not keep the others from it
        val run = if (Anticheat.VANILLA in GameTestEnvironment.anticheats) paperOnly + tests else tests
        val failures = run.mapNotNull { test -> runCatching { test.runTest(context) }.exceptionOrNull() }
        failures.firstOrNull()?.let { first ->
            failures.drop(1).forEach(first::addSuppressed)
            throw first
        }
    }

}

private const val NEAR_SQR = 1.0E-4

/** Walks to [goal] while [active]; make it on the client thread. */
internal class TestWalker(name: String, private val radius: Double, build: Boolean = false) :
    NavigationBaseValueGroup<Unit>(
        name = name, enabled = true, allowBreak = build, allowPlace = build, showRoute = false,
    ) {

    @Volatile
    var active = false

    @Volatile
    var goal: Vec3? = null

    override val running: Boolean
        get() = active && super.running

    override fun createNavigationContext() = Unit

    override fun calculateGoalPosition(context: Unit) = goal

    override fun goalRadius(context: Unit) = radius

    fun start(context: ClientGameTestContext) = context.onClient { active = true }

    fun stop(context: ClientGameTestContext) = context.onClient {
        active = false
        goal = null
    }

    fun <T> walking(context: ClientGameTestContext, goal: Vec3, block: () -> T): T {
        context.onClient { this.goal = goal }
        try {
            return block()
        } finally {
            context.onClient { this.goal = null }
        }
    }

    /** Walks to [goal] under [TurnWatch] and fails with [failure] after [ticks]; [observe] sees every tick. */
    fun walk(
        context: ClientGameTestContext,
        goal: Vec3,
        ticks: Int,
        title: String,
        failure: String,
        acting: () -> Boolean = { false },
        observe: (LocalPlayer) -> Unit = {},
    ) = walking(context, goal) {
        TurnWatch.watch(context, title, acting) {
            await(context, ticks, "$failure to $goal") {
                context.fromClient { mc ->
                    val player = checkNotNull(mc.player)
                    observe(player)
                    player.position().arrivedAt(goal, radius)
                }
            }
        }
    }
}

internal fun Vec3.arrivedAt(goal: Vec3, radius: Double) =
    horizontalDistanceTo(goal) <= radius + 0.1 && abs(y - goal.y) < 1.0

internal fun Vec3.near(other: Vec3, toleranceSqr: Double = NEAR_SQR) = distanceToSqr(other) < toleranceSqr

/** Teleports the player to [position], facing [yaw], and waits until it stands there. */
internal fun GameTestScope.moveTo(position: Vec3, yaw: Float = 0f) = travel(BlockPos.containing(position), yaw)

/** Runs [block] and fails if it cost the player health on the server. */
internal inline fun GameTestScope.unhurt(what: String, block: () -> Unit) {
    val before = health()
    block()
    val after = health()
    check(after >= before) { "$what: hurt the player from $before to $after" }
}

internal fun GameTestScope.awaitHuskHurt(timeout: Int) =
    awaitServer("entity @e[type=minecraft:husk,nbt=!{Health:20.0f}]", timeout)

/** Waits up to [ticks] for [condition] and fails with [failure] and where the player stopped. */
internal inline fun await(context: ClientGameTestContext, ticks: Int, failure: String, condition: () -> Boolean) {
    repeat(ticks) {
        if (condition()) {
            return
        }
        context.waitTick()
    }
    error("$failure within $ticks ticks, stopped at ${context.playerPosition()}")
}

internal fun awaitPlayer(
    context: ClientGameTestContext,
    ticks: Int,
    failure: String,
    condition: (LocalPlayer) -> Boolean,
) = await(context, ticks, failure) { context.fromClient { condition(checkNotNull(it.player)) } }

/** Watches a run for [timeout] ticks until [arrived] or a fall below [lowest]; why it failed, or null. */
internal fun runCourse(
    context: ClientGameTestContext,
    title: String,
    tag: String,
    lowest: Int,
    timeout: Int,
    arrived: (Vec3) -> Boolean,
): String? = TurnWatch.watch(context, title) {
    for (tick in 0 until timeout) {
        val position = context.playerPosition()
        if (position.y < lowest) {
            return@watch "fell at $position after $tick ticks"
        }
        if (arrived(position)) {
            logger.info("PARKOUR $tag: done in $tick ticks")
            return@watch null
        }
        context.waitTick()
    }

    "not done within $timeout ticks, stopped at ${context.playerPosition()}"
}

/** Goes to [goal] with `.goto` until AutoWalk turns off within [arrival] of it; true if [flying] held on the way. */
internal fun goto(
    context: ClientGameTestContext,
    goal: Vec3,
    what: String,
    ticks: Int,
    arrival: Double,
    flying: (LocalPlayer) -> Boolean,
): Boolean {
    context.command("goto ${goal.x} ${goal.y} ${goal.z}")
    check(context.fromClient { ModuleAutoWalk.enabled }) { ".goto did not turn AutoWalk on" }

    var flew = false
    repeat(ticks) {
        val (arrived, now) = context.fromClient { mc -> !ModuleAutoWalk.enabled to flying(checkNotNull(mc.player)) }
        flew = flew || now
        if (arrived) {
            val position = context.playerPosition()
            check(position.distanceTo(goal) <= arrival) { "AutoWalk stopped at $position going $what" }
            return flew
        }
        context.waitTick()
    }

    val position = context.playerPosition()
    context.onClient { ModuleAutoWalk.enabled = false }
    error("Did not get $what to $goal within $ticks ticks, stopped at $position")
}

