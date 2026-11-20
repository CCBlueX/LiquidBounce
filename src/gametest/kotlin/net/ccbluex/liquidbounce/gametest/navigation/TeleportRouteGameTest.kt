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

import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.ServerGameTest
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.fill
import net.ccbluex.gametest.harness.action.summon
import net.ccbluex.gametest.harness.action.teleport
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura
import net.ccbluex.liquidbounce.gametest.awaitServerAt
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.playerPosition
import net.ccbluex.liquidbounce.gametest.restoring
import net.ccbluex.liquidbounce.gametest.serverPosition
import net.ccbluex.liquidbounce.utils.navigation.route.BlockGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteFailure
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.teleport.Route
import net.ccbluex.liquidbounce.utils.navigation.teleport.StepsValueGroup
import net.ccbluex.liquidbounce.utils.navigation.teleport.Teleport
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportManager
import net.ccbluex.liquidbounce.utils.navigation.teleport.WaypointSearch
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.ccbluex.liquidbounce.utils.world.SectionCache
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.monster.zombie.Husk
import net.minecraft.world.phys.Vec3

private const val WAIT_TICKS = 200
private const val TPAURA_TICKS = 400
private const val SETTLE_TICKS = 5
private const val SLOW_STEP = 2.0

private object Owner : EventListener

/**
 * Routes from the teleport engine reach a survival player on the server without setbacks or fall damage, a setback
 * ends a run, and TpAura drives the same engine end to end.
 */
class TeleportRouteGameTest : ServerGameTest({
    context.restoring(ModuleTpAura) {
        val y = origin.y
        buildArena(y)
        run("gamerule minecraft:natural_health_regeneration false")
        // Peaceful has no husks
        run("difficulty easy")
        moveTo(Vec3(2.5, y.toDouble(), 0.5))

        unhurt("A route") {
            checkDirectAndDetour(y)
            checkClimbHoldAndUndo(y)
            checkSetback(y)
            checkOutcomes(y)
            checkTpAura(y)
        }
    }
})

/**
 * Floor at y - 1 for x 0..40 and z -12..12, a 12 high wall at x 20 for z -8..8, a platform at x 32..36
 * with its top at y + 5, and a closed cell at 7, y, 9.
 */
private fun GameTestScope.buildArena(y: Int) {
    fill(BlockPos(0, y - 1, -12), BlockPos(40, y - 1, 12), "minecraft:stone")
    fill(BlockPos(0, y, -12), BlockPos(40, y + 14, 12), "minecraft:air")
    fill(BlockPos(20, y, -8), BlockPos(20, y + 11, 8), "minecraft:stone")
    fill(BlockPos(32, y, -3), BlockPos(36, y + 4, 3), "minecraft:stone")
    fill(BlockPos(6, y, 8), BlockPos(8, y + 2, 10), "minecraft:stone")
    fill(BlockPos(7, y, 9), BlockPos(7, y + 1, 9), "minecraft:air")
    awaitServer("block 7 ${y + 1} 9 minecraft:air")
}

private fun GameTestScope.checkDirectAndDetour(y: Int) {
    val direct = found(context.route(BlockPos(12, y, 0)))
    check(direct.points.size == 2) { "Open ground took a detour: ${direct.points}" }
    arrive(dispatch(direct, Teleport.Finish.LAND), Vec3(12.5, y.toDouble(), 0.5))

    val detour = found(context.route(BlockPos(28, y, 0)))
    val points = detour.points
    check(points.size > 2) { "The route went through the wall: $points" }
    val inWall = points.map(BlockPos::containing).filter { it.x == 20 && it.z in -8..8 }
    check(inWall.isEmpty()) { "A waypoint is in the wall: $points" }
    arrive(dispatch(detour, Teleport.Finish.LAND), Vec3(28.5, y.toDouble(), 0.5))
}

private fun GameTestScope.checkClimbHoldAndUndo(y: Int) {
    val top = Vec3(34.5, y + 5.0, 0.5)
    arrive(dispatch(found(context.route(BlockPos(34, y + 5, 0))), Teleport.Finish.LAND), top)

    val below = Vec3(26.5, y.toDouble(), 0.5)
    val hold = dispatch(found(context.route(BlockPos(26, y, 0))), Teleport.Finish.HOLD)
    check(hold.state == Teleport.State.DONE) { "The held run ended as ${hold.state}" }
    awaitServerAt(below, timeout = WAIT_TICKS)
    check(context.playerPosition().near(top)) { "The client moved with a held run" }
    check(context.fromClient { TeleportManager.serverPosition?.near(below) == true })

    context.onClient { TeleportManager.undo(Owner) }
    awaitClient("the undo", WAIT_TICKS) { !TeleportManager.holds(Owner) }
    awaitServerAt(top, timeout = WAIT_TICKS)
    ticks(SETTLE_TICKS)
    check(serverPosition().near(top)) { "The server did not stay where the undo ended" }
}

private fun GameTestScope.checkSetback(y: Int) {
    val slow = StepsValueGroup().apply {
        containedValues.first { it.name == "Length" }.setByString(SLOW_STEP.toString())
        containedValues.first { it.name == "PerTick" }.setByString("1")
    }
    val far = found(context.route(BlockPos(2, y, 0), stepLength = SLOW_STEP))
    val run = context.fromClient {
        checkNotNull(TeleportManager.dispatch(Owner, far, Teleport.Finish.LAND, slow))
    }
    check(run.ticks > SETTLE_TICKS * 2) { "The slow run is too short to interrupt" }

    ticks(SETTLE_TICKS)
    val setback = Vec3(30.5, y.toDouble(), -10.5)
    teleport(BlockPos.containing(setback))
    awaitClient("the run to end", WAIT_TICKS) { run.state != Teleport.State.ACTIVE }

    check(run.state == Teleport.State.REJECTED) { "A setback ended the run as ${run.state}" }
    check(context.fromClient { !TeleportManager.holds(Owner) && TeleportManager.serverPosition == null })
    awaitClient("the client at the setback") { it.player?.position()?.near(setback) == true }
    ticks(SETTLE_TICKS)
    check(serverPosition().near(setback)) { "The rejected run kept moving the player" }
}

private fun GameTestScope.checkOutcomes(y: Int) {
    moveTo(Vec3(3.5, y.toDouble(), 9.5))
    check(context.route(BlockPos(3, y, 9)) == RouteResult.AlreadyThere) { "Standing on the goal is no arrival" }

    val enclosed = context.route(BlockPos(7, y, 9), maxLength = 12.0)
    check((enclosed as? RouteResult.Failed)?.failure == RouteFailure.NO_PATH) { "A closed cell gave $enclosed" }

    val midAir = context.route(BlockPos(5, y + 3, 9))
    check((midAir as? RouteResult.Failed)?.failure == RouteFailure.INVALID_DESTINATION) { "Mid-air gave $midAir" }
}

/** TpAura reaches a husk on open ground, one 14 blocks below a tower and one on a platform 13 blocks up. */
private fun GameTestScope.checkTpAura(y: Int) {
    checkTpAuraAttack(Vec3(2.5, y.toDouble(), -10.5), BlockPos(16, y, -11))

    fill(BlockPos(26, y, -12), BlockPos(28, y + 13, -10), "minecraft:stone")
    checkTpAuraAttack(Vec3(27.5, y + 14.0, -10.5), BlockPos(34, y, -11))
    fill(BlockPos(26, y, -12), BlockPos(28, y + 13, -10), "minecraft:air")

    fill(BlockPos(32, y + 12, -12), BlockPos(36, y + 12, -8), "minecraft:stone")
    checkTpAuraAttack(Vec3(27.5, y.toDouble(), -10.5), BlockPos(34, y + 13, -11))
    fill(BlockPos(32, y + 12, -12), BlockPos(36, y + 12, -8), "minecraft:air")
}

/** Starts TpAura at [origin] until it hits a husk standing at [target], then checks that the player came back. */
private fun GameTestScope.checkTpAuraAttack(origin: Vec3, target: BlockPos) {
    moveTo(origin)
    summon("minecraft:husk", target, "{NoAI:1b,PersistenceRequired:1b}")
    awaitClient("the husk", WAIT_TICKS) { client ->
        client.level?.entitiesForRendering()?.any { it is Husk && it.isAlive } == true
    }

    context.onClient { ModuleTpAura.enabled = true }
    awaitHuskHurt(TPAURA_TICKS)

    context.onClient { ModuleTpAura.enabled = false }
    awaitClient("TpAura to let go", WAIT_TICKS) {
        !TeleportManager.holds(ModuleTpAura) && !TeleportManager.isMoving
    }
    check(context.playerPosition().near(origin)) { "TpAura moved the client from $origin" }
    awaitServerAt(origin, timeout = WAIT_TICKS)

    run("kill @e[type=minecraft:husk]")
    awaitClient("the husk gone", WAIT_TICKS) { client ->
        client.level?.entitiesForRendering()?.none { it is Husk } == true
    }
}

/** Searches like a module does: over a copy of the world, slice by slice. */
private fun ClientGameTestContext.route(
    goal: BlockPos,
    maxLength: Double = 80.0,
    stepLength: Double = StepsValueGroup.DEFAULT_STEP_LENGTH,
): RouteResult<Route> = fromClient { mc ->
    val level = checkNotNull(mc.level)
    val start = checkNotNull(mc.player).position()
    val region = BlockGoal(goal).region(start, maxLength, maxLength.toInt())
    val search = WaypointSearch(
        Terrain(SectionCache.capture(level, region)),
        start,
        [goal],
        RouteLimits(maxLength),
        stepLength,
    )

    var result = search.advance()
    while (result == RouteResult.Searching) {
        result = search.advance()
    }
    result
}

private fun found(result: RouteResult<Route>) =
    (result as? RouteResult.Found<Route>)?.value ?: error("Expected a route, got $result")

private fun GameTestScope.dispatch(route: Route, finish: Teleport.Finish): Teleport {
    val run = context.fromClient {
        checkNotNull(TeleportManager.dispatch(Owner, route, finish, StepsValueGroup()))
    }
    awaitClient("the run to end", WAIT_TICKS) { run.state != Teleport.State.ACTIVE }
    return run
}

private fun GameTestScope.arrive(run: Teleport, destination: Vec3) {
    check(run.state == Teleport.State.DONE) { "The run to $destination ended as ${run.state}" }
    awaitServerAt(destination, timeout = WAIT_TICKS)
    ticks(SETTLE_TICKS)
    check(context.playerPosition().near(destination)) { "Set back from $destination" }
    check(serverPosition().near(destination)) { "The server did not keep $destination" }
}
