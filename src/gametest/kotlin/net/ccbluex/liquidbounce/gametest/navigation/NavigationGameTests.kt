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

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.gametest.command
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.player
import net.ccbluex.liquidbounce.gametest.playerPosition
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.navigation.NavigationBaseValueGroup
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.monster.zombie.Husk
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/** `-PgameTestNavigation=false` skips the navigation game tests, which take minutes. */
val navigationGameTests = System.getProperty("net.ccbluex.liquidbounce.gametest.navigation") != "false"

private const val WAIT_TICKS = 200
private const val SETTLE_TICKS = 5
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

/** Teleports the player to [position], facing [yaw] if given, and waits until the client is there. */
internal fun moveTo(
    context: ClientGameTestContext,
    server: TestServerContext,
    position: Vec3,
    yaw: Float? = null,
    settleTicks: Int = SETTLE_TICKS,
    toleranceSqr: Double = NEAR_SQR,
) {
    val rotation = yaw?.let { " $it 0" }.orEmpty()
    server.runCommand("tp @a ${position.x} ${position.y} ${position.z}$rotation")
    awaitPosition(context, position, settleTicks, toleranceSqr)
}

internal fun awaitPosition(
    context: ClientGameTestContext,
    position: Vec3,
    settleTicks: Int = SETTLE_TICKS,
    toleranceSqr: Double = NEAR_SQR,
) {
    context.waitFor({ it.player?.position()?.near(position, toleranceSqr) == true }, WAIT_TICKS)
    context.waitTicks(settleTicks)
}

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

internal inline fun unhurt(server: TestServerContext, what: String, block: () -> Unit) {
    val health = server.player { it.health }
    block()
    val after = server.player { it.health }
    check(after >= health) { "AutoWalk hurt the player $what, from $health to $after" }
}

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

/** Runs [block], then turns [modules] off and puts their settings back. */
internal inline fun ClientGameTestContext.restoring(vararg modules: ClientModule, block: () -> Unit) {
    val saved = fromClient { modules.map { ConfigSystem.serializeValueGroup(it) } }
    try {
        block()
    } finally {
        onClient {
            for ((module, config) in modules.zip(saved)) {
                module.enabled = false
                ConfigSystem.deserializeValueGroup(module, config)
            }
        }
    }
}

internal fun TestServerContext.huskHurt(): Boolean = computeOnServer<Boolean, RuntimeException> { server ->
    server.overworld().allEntities.any { it is Husk && it.health < it.maxHealth }
}
