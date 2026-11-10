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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.groundY
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.player
import net.ccbluex.liquidbounce.gametest.playerPosition
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.ccbluex.liquidbounce.utils.navigation.route.BlockGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteFailure
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.teleport.RunFinish
import net.ccbluex.liquidbounce.utils.navigation.teleport.RunState
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportDispatcher
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportPlan
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportRouteSearch
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportRun
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportStepSettings
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.ccbluex.liquidbounce.utils.navigation.terrain.SectionCache
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.monster.zombie.Husk
import net.minecraft.world.phys.Vec3

/**
 * Routes from the teleport engine reach a survival player on the integrated server without setbacks or fall
 * damage, a setback ends a run, and TpAura drives the same engine end to end.
 */
@Suppress("TooManyFunctions")
class TeleportRouteGameTest : FabricClientGameTest {

    private object Owner : EventListener

    override fun runTest(context: ClientGameTestContext) {
        if (!navigationGameTests) {
            return
        }
        context.waitForClient()
        context.restoring(ModuleTpAura) {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val y = server.groundY()
                buildArena(server, y)
                server.runCommand("gamemode survival @a")
                server.runCommand("gamerule natural_health_regeneration false")
                moveTo(context, server, Vec3(2.5, y.toDouble(), 0.5))

                checkDirectAndDetour(context, server, y)
                checkClimbHoldAndUndo(context, server, y)
                checkSetback(context, server, y)
                checkOutcomes(context, server, y)
                checkTpAura(context, server, y)

                check(server.player { it.health } == server.player { it.maxHealth }) { "A route hurt the player" }
            }
        }
    }

    /**
     * Floor at y - 1 for x 0..40 and z -12..12, a 12 high wall at x 20 for z -8..8, a platform at x 32..36
     * with its top at y + 5, and a closed cell at 7, y, 9.
     */
    private fun buildArena(server: TestServerContext, y: Int) {
        server.runCommand("fill 0 ${y - 1} -12 40 ${y - 1} 12 stone")
        server.runCommand("fill 0 $y -12 40 ${y + 14} 12 air")
        server.runCommand("fill 20 $y -8 20 ${y + 11} 8 stone")
        server.runCommand("fill 32 $y -3 36 ${y + 4} 3 stone")
        server.runCommand("fill 6 $y 8 8 ${y + 2} 10 stone")
        server.runCommand("fill 7 $y 9 7 ${y + 1} 9 air")
    }

    private fun checkDirectAndDetour(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        val direct = found(context.route(BlockPos(12, y, 0)))
        check(direct.route.points.size == 2) { "Open ground took a detour: ${direct.route.points}" }
        arrive(context, server, travel(context, direct, RunFinish.LAND), Vec3(12.5, y.toDouble(), 0.5))

        val detour = found(context.route(BlockPos(28, y, 0)))
        val points = detour.route.points
        check(points.size > 2) { "The route went through the wall: $points" }
        val inWall = points.map(BlockPos::containing).filter { it.x == 20 && it.z in -8..8 }
        check(inWall.isEmpty()) { "A waypoint is in the wall: $points" }
        arrive(context, server, travel(context, detour, RunFinish.LAND), Vec3(28.5, y.toDouble(), 0.5))
    }

    private fun checkClimbHoldAndUndo(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        val top = Vec3(34.5, y + 5.0, 0.5)
        arrive(context, server, travel(context, found(context.route(BlockPos(34, y + 5, 0))), RunFinish.LAND), top)

        val below = Vec3(26.5, y.toDouble(), 0.5)
        val hold = travel(context, found(context.route(BlockPos(26, y, 0))), RunFinish.HOLD)
        check(hold.state == RunState.DONE) { "The held run ended as ${hold.state}" }
        server.waitFor({ it.player().position().near(below) }, WAIT_TICKS)
        check(context.playerPosition().near(top)) { "The client moved with a held run" }
        check(context.fromClient { TeleportDispatcher.serverPosition?.near(below) == true })

        context.onClient { TeleportDispatcher.undo(Owner) }
        context.waitFor({ !TeleportDispatcher.holds(Owner) }, WAIT_TICKS)
        server.waitFor({ it.player().position().near(top) }, WAIT_TICKS)
        context.waitTicks(SETTLE_TICKS)
        check(server.player { it.position() }.near(top)) { "The server did not stay where the undo ended" }
    }

    private fun checkSetback(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        val slow = TeleportStepSettings().apply {
            containedValues.first { it.name == "Length" }.setByString(SLOW_STEP.toString())
            containedValues.first { it.name == "PerTick" }.setByString("1")
        }
        val far = found(context.route(BlockPos(2, y, 0), stepLength = SLOW_STEP))
        val run = context.fromClient {
            checkNotNull(TeleportDispatcher.dispatch(Owner, far, RunFinish.LAND, slow))
        }
        check(run.ticks > SETTLE_TICKS * 2) { "The slow run is too short to interrupt" }

        context.waitTicks(SETTLE_TICKS)
        val setback = Vec3(30.5, y.toDouble(), -10.5)
        server.runCommand("tp @a ${setback.x} ${setback.y} ${setback.z}")
        context.waitFor({ run.state != RunState.ACTIVE }, WAIT_TICKS)

        check(run.state == RunState.REJECTED) { "A setback ended the run as ${run.state}" }
        check(context.fromClient { !TeleportDispatcher.holds(Owner) && TeleportDispatcher.serverPosition == null })
        awaitPosition(context, setback)
        check(server.player { it.position() }.near(setback)) { "The rejected run kept moving the player" }
    }

    private fun checkOutcomes(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        moveTo(context, server, Vec3(3.5, y.toDouble(), 9.5))
        check(context.route(BlockPos(3, y, 9)) == RouteResult.AlreadyThere) { "Standing on the goal is no arrival" }

        val enclosed = context.route(BlockPos(7, y, 9), maxLength = 12.0)
        check((enclosed as? RouteResult.Failed)?.failure == RouteFailure.NO_PATH) { "A closed cell gave $enclosed" }

        val midAir = context.route(BlockPos(5, y + 3, 9))
        check((midAir as? RouteResult.Failed)?.failure == RouteFailure.INVALID_DESTINATION) { "Mid-air gave $midAir" }
    }

    /** TpAura reaches a husk on open ground, one 14 blocks below a tower and one on a platform 13 blocks up. */
    private fun checkTpAura(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        checkTpAuraAttack(context, server, Vec3(2.5, y.toDouble(), -10.5), Vec3(16.5, y.toDouble(), -10.5))

        server.runCommand("fill 26 $y -12 28 ${y + 13} -10 stone")
        checkTpAuraAttack(context, server, Vec3(27.5, y + 14.0, -10.5), Vec3(34.5, y.toDouble(), -10.5))
        server.runCommand("fill 26 $y -12 28 ${y + 13} -10 air")

        server.runCommand("fill 32 ${y + 12} -12 36 ${y + 12} -8 stone")
        checkTpAuraAttack(context, server, Vec3(27.5, y.toDouble(), -10.5), Vec3(34.5, y + 13.0, -10.5))
        server.runCommand("fill 32 ${y + 12} -12 36 ${y + 12} -8 air")
    }

    /**
     * Starts TpAura at [origin] until it hits a husk standing at [target], then checks that the player came back
     * unhurt.
     */
    private fun checkTpAuraAttack(
        context: ClientGameTestContext,
        server: TestServerContext,
        origin: Vec3,
        target: Vec3,
    ) {
        moveTo(context, server, origin)
        server.runCommand("summon husk ${target.x} ${target.y} ${target.z} {NoAI:1b,PersistenceRequired:1b}")
        context.waitFor({ client ->
            client.level?.entitiesForRendering()?.any { it is Husk && it.isAlive } == true
        }, WAIT_TICKS)
        val health = server.player { it.health }

        context.onClient { ModuleTpAura.enabled = true }
        server.waitFor({ s ->
            s.overworld().allEntities.any { it is Husk && it.isAlive && it.health < it.maxHealth }
        }, TPAURA_TICKS)

        context.onClient { ModuleTpAura.enabled = false }
        context.waitFor({ !TeleportDispatcher.holds(ModuleTpAura) && !TeleportDispatcher.isMoving }, WAIT_TICKS)
        check(context.playerPosition().near(origin)) { "TpAura moved the client from $origin" }
        server.waitFor({ it.player().position().near(origin) }, WAIT_TICKS)

        val (after, source) = server.player { it.health to it.lastDamageSource?.type()?.msgId() }
        check(after == health) { "TpAura from $origin hurt the player from $health to $after ($source)" }

        server.runCommand("kill @e[type=husk]")
        context.waitFor({ client -> client.level?.entitiesForRendering()?.none { it is Husk } == true }, WAIT_TICKS)
    }

    /** Searches like a module does: over a copy of the world, slice by slice. */
    private fun ClientGameTestContext.route(
        goal: BlockPos,
        maxLength: Double = 80.0,
        stepLength: Double = TeleportStepSettings.DEFAULT_STEP_LENGTH,
    ): RouteResult<TeleportPlan> = fromClient { mc ->
        val level = checkNotNull(mc.level)
        val start = checkNotNull(mc.player).position()
        val region = BlockGoal(listOf(goal)).region(start, maxLength, maxLength.toInt())
        val search = TeleportRouteSearch(
            NavigationTerrain(SectionCache.capture(level, region)),
            start,
            listOf(goal),
            RouteLimits(maxLength),
            stepLength,
        )

        var result = search.advance()
        while (result == RouteResult.Searching) {
            result = search.advance()
        }
        result
    }

    private fun found(result: RouteResult<TeleportPlan>) =
        (result as? RouteResult.Found<TeleportPlan>)?.value ?: error("Expected a route, got $result")

    private fun travel(context: ClientGameTestContext, plan: TeleportPlan, finish: RunFinish): TeleportRun {
        val run = context.fromClient {
            checkNotNull(TeleportDispatcher.dispatch(Owner, plan, finish, TeleportStepSettings()))
        }
        context.waitFor({ run.state != RunState.ACTIVE }, WAIT_TICKS)
        return run
    }

    private fun arrive(context: ClientGameTestContext, server: TestServerContext, run: TeleportRun, destination: Vec3) {
        check(run.state == RunState.DONE) { "The run to $destination ended as ${run.state}" }
        server.waitFor({ it.player().position().near(destination) }, WAIT_TICKS)
        context.waitTicks(SETTLE_TICKS)
        check(context.playerPosition().near(destination)) { "Set back from $destination" }
        check(server.player { it.position() }.near(destination)) { "The server did not keep $destination" }
    }

    private companion object {
        const val WAIT_TICKS = 200
        const val TPAURA_TICKS = 400
        const val SETTLE_TICKS = 5
        const val SLOW_STEP = 2.0
    }

}
