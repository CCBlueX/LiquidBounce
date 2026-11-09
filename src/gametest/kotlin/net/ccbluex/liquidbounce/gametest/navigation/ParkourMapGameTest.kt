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
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.ccbluex.liquidbounce.utils.client.GitInfo
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.navigation.NavigationBaseValueGroup
import net.ccbluex.liquidbounce.utils.navigation.render.RouteView
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkNavigator
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkPath
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave
import net.minecraft.client.CameraType
import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.level.gamerules.GameRules
import net.minecraft.world.phys.Vec3
import org.apache.commons.io.file.PathUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A benchmark, not a check: the walking navigation runs the stages of real Jump n' Run maps listed in the manifest
 * given with `-PgameTestParkourMaps`, and the results go to a report next to it. Stage failures never fail the test;
 * only a map that cannot be loaded does.
 */
@Suppress("TooManyFunctions")
class ParkourMapGameTest : FabricClientGameTest {

    private object Owner : EventListener {
        @Volatile
        var active = false

        @Volatile
        var jumps = 0

        override val running: Boolean
            get() = active

        @Suppress("unused")
        private val jumpHandler = handler<PlayerJumpEvent> { jumps++ }
    }

    private class Walker : NavigationBaseValueGroup<Unit>(
        Owner, "ParkourWalker", enabled = true, allowBreak = false, allowPlace = false, allowParkour = true,
        showRoute = true,
    ) {
        @Volatile
        var goal: Vec3? = null

        @Volatile
        var radius = 1.0

        override fun createNavigationContext() = Unit

        override fun calculateGoalPosition(context: Unit) = goal

        override fun goalRadius(context: Unit) = radius

        /**
         * The path being followed and the state of the shown route; the engine keeps both private.
         */
        val path: WalkPath?
            get() {
                val follower = FOLLOWER.get(NAVIGATOR.get(this))
                return follower.javaClass.getMethod("getPath").invoke(follower) as WalkPath?
            }

        val routeState: String
            get() = (VIEW.get(NAVIGATOR.get(this)) as RouteView).state.name

        private companion object {
            val NAVIGATOR = NavigationBaseValueGroup::class.java.getDeclaredField("navigator").apply {
                isAccessible = true
            }
            val FOLLOWER = WalkNavigator::class.java.getDeclaredField("follower").apply { isAccessible = true }
            val VIEW = WalkNavigator::class.java.getDeclaredField("view").apply { isAccessible = true }
        }
    }

    private lateinit var walker: Walker
    private lateinit var shots: Path
    private val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))

    override fun runTest(context: ClientGameTestContext) {
        val manifest = ParkourSettings.manifest ?: return
        val reports = checkNotNull(ParkourSettings.reports)
        shots = reports.resolve("shots").createDirectories()
        context.waitForClient()
        context.onClient {
            walker = Walker()
            // Jump 'n' Run courses hang over the void and wind around.
            for ((name, value) in listOf("RiskyJumps" to "true", "PathRange" to "128")) {
                walker.containedValues.first { it.name == name }.setByString(value)
            }
            Owner.active = true
            it.options.renderDistance().set(RENDER_DISTANCE)
        }

        val maps = readManifest(manifest).filter(ParkourSettings::includes)
        val report = BenchmarkReport(
            started = stamp,
            commit = GitInfo.get("git.commit.id.abbrev") ?: "unknown",
            manifest = manifest.toAbsolutePath().toString(),
            renderDistance = context.fromClient { it.options.renderDistance().get() },
        )
        val prefix = if (ParkourSettings.survey) "survey" else "maps"
        val json = reports.resolve("$prefix-$stamp.json")
        val markdown = reports.resolve("$prefix-$stamp.md")

        val write = { report.write(json, markdown) }
        try {
            for (map in maps) {
                val result = MapResult(map.slug, map.name, map.mcVersion)
                report.maps += result
                @Suppress("TooGenericExceptionCaught")
                try {
                    runMap(context, map, result, manifest, write)
                } catch (error: Throwable) {
                    result.error = error.toString()
                    logger.error("Parkour map ${map.slug} failed", error)
                }
                write()
            }
        } finally {
            context.onClient {
                Owner.active = false
                walker.goal = null
            }
            write()
        }

        val errors = report.maps.filter { it.error != null }
        check(errors.isEmpty()) { "Maps could not be run: " + errors.joinToString { "${it.slug}: ${it.error}" } }
    }

    /**
     * Runs the stages of [map], or surveys it; [write] saves the report so far, which a crashed game keeps.
     */
    private fun runMap(
        context: ClientGameTestContext,
        map: ParkourMap,
        result: MapResult,
        manifest: Path,
        write: () -> Unit,
    ) {
        check(map.directory.isDirectory()) { "No world save at ${map.directory}" }
        val loadStarted = System.nanoTime()
        val save = context.worldBuilder()
            .adjustSettings { creator ->
                creator.gameRules.set(GameRules.IMMEDIATE_RESPAWN, true, null)
                creator.gameRules.set(GameRules.COMMAND_BLOCK_OUTPUT, false, null)
                creator.gameRules.set(GameRules.SPAWN_MONSTERS, false, null)
            }
            .create()
            .use { it.worldSave }
        installMap(map, save)

        save.open().use { world ->
            val server = world.server
            server.runCommand("difficulty peaceful")
            server.runCommand("time set noon")
            server.runCommand("gamemode adventure @a")
            moveTo(context, server, Vec3.atBottomCenterOf(map.spawn), 0f, listOf(map.spawn))
            context.onClient { walker.containedValues.first { it.name == "MaxFall" }.setByString("${map.maxFall}") }
            result.loadSeconds = seconds(loadStarted)

            val runStarted = System.nanoTime()
            if (ParkourSettings.survey) {
                ParkourSurvey.run(context, server, map, manifest.resolveSibling("${map.slug}.ingame-survey.json"))
            } else {
                for (stage in map.stages.filter { ParkourSettings.includes(map, it) }) {
                    result.stages += runStage(context, server, map, stage)
                    result.runSeconds = seconds(runStarted)
                    logger.info("Parkour ${map.slug} stage ${stage.index}: ${result.stages.last().outcome}")
                    write()
                }
            }
            result.runSeconds = seconds(runStarted)
        }
    }

    /**
     * Replaces the overworld chunks of [save] with the map's; the game upgrades them as they load.
     */
    private fun installMap(map: ParkourMap, save: TestWorldSave) {
        val target = save.saveDirectory.resolve("dimensions/minecraft/overworld")
        for (name in listOf("region", "entities", "poi")) {
            val destination = target.resolve(name)
            if (destination.exists()) {
                PathUtils.deleteDirectory(destination)
            }
            val source = overworldFolder(map.directory, name) ?: continue
            PathUtils.copyDirectory(source, destination, StandardCopyOption.REPLACE_EXISTING)
        }
        check(target.resolve("region").isDirectory()) { "${map.directory} has no region folder" }

        val scoreboard = listOf("data/minecraft/scoreboard.dat", "data/scoreboard.dat")
            .map(map.directory::resolve)
            .firstOrNull(Files::isRegularFile)
        if (scoreboard != null) {
            val destination = save.saveDirectory.resolve("data/minecraft/scoreboard.dat")
            destination.parent.createDirectories()
            Files.copy(scoreboard, destination, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun runStage(
        context: ClientGameTestContext,
        server: TestServerContext,
        map: ParkourMap,
        stage: ParkourStage,
    ): StageResult {
        server.runCommand("gamemode adventure @a")
        server.runOnServer<RuntimeException> { minecraft ->
            val player = minecraft.player()
            player.removeAllEffects()
            player.health = player.maxHealth
            player.foodData.foodLevel = MAX_FOOD
            player.foodData.setSaturation(MAX_FOOD.toFloat())
        }
        moveTo(context, server, Vec3.atBottomCenterOf(stage.start), stage.yaw, listOf(stage.start, stage.finish))

        val standable = server.computeOnServer<Boolean, RuntimeException> {
            ParkourSurvey.isStandable(it.overworld(), stage.start)
        }
        if (!standable) {
            val position = context.fromClient { checkNotNull(it.player).position() }
            return StageResult(
                stage.index, stage.name, StageOutcome.BAD_START, "nothing to stand on at the start", 0, 0,
                stage.straightDistance.rounded(), 0.0, position.distanceTo(stage.goal).rounded(), 0, 0, false,
                stage.start.list(), stage.finish.list(), position.rounded(), null,
            )
        }

        context.onClient {
            Owner.jumps = 0
            // The walker stops once its position is within the radius; finishes such as pressure plates need the
            // feet in the block, which the stage's radius measures from.
            walker.radius = min(stage.finishRadius, IN_BLOCK_RADIUS)
            walker.goal = stage.goal
        }
        val recorder = StageRecorder(stage, sample(context))
        while (recorder.outcome == null) {
            context.waitTick()
            recorder.update(sample(context))
        }

        val outcome = checkNotNull(recorder.outcome)
        val jumps = context.fromClient { Owner.jumps }
        val failure = if (outcome == StageOutcome.PASS) null else describeFailure(context, server, map, recorder)
        context.onClient { walker.goal = null }
        settle(context)

        return StageResult(
            stage.index, stage.name, outcome, recorder.detail, recorder.tick, recorder.limit,
            stage.straightDistance.rounded(), recorder.travelled.rounded(), recorder.closest.rounded(), jumps,
            recorder.falls, recorder.pathFound, stage.start.list(), stage.finish.list(),
            recorder.last.position.rounded(), failure,
        )
    }

    private fun describeFailure(
        context: ClientGameTestContext,
        server: TestServerContext,
        map: ParkourMap,
        recorder: StageRecorder,
    ): StageFailure {
        val (effects, damage) = server.computeOnServer<Pair<List<String>, String?>, RuntimeException> { minecraft ->
            val player = minecraft.player()
            player.activeEffects.map { it.toString() } to player.lastDamageSource?.type()?.msgId()
        }
        val lastGround = recorder.lastGround
        val takeoffMove = lastGround?.let { ground ->
            val feet = BlockPos.containing(ground)
            recorder.takeoffPath?.steps
                ?.minByOrNull { it.from.distSqr(feet) }
                ?.let { "${it.move} ${it.from.toShortString()} -> ${it.to.toShortString()}" }
        }
        val lastPath = recorder.lastPath
        return StageFailure(
            position = recorder.last.position.rounded(),
            lastGround = lastGround?.rounded(),
            takeoffMove = takeoffMove,
            path = lastPath?.points?.take(PATH_POINTS)?.map { it.rounded() },
            pathMoves = lastPath?.steps?.take(PATH_POINTS)?.map { "${it.move} ${it.to.toShortString()}" },
            pathComplete = lastPath?.complete,
            trace = recorder.positions,
            effects = effects,
            routeState = context.fromClient { walker.routeState },
            damage = damage,
            screenshot = screenshot(context, "${map.slug}-${recorder.stage.index}"),
        )
    }

    private fun screenshot(context: ClientGameTestContext, name: String): String {
        context.onClient { it.options.cameraType = CameraType.THIRD_PERSON_BACK }
        context.waitTicks(2)
        val shot = context.takeScreenshot("parkour-$name")
        context.onClient { it.options.cameraType = CameraType.FIRST_PERSON }
        val target = shots.resolve("$stamp-$name.png")
        Files.copy(shot, target, StandardCopyOption.REPLACE_EXISTING)
        return target.toString()
    }

    /**
     * Waits until a dead player respawned and no screen is open.
     */
    private fun settle(context: ClientGameTestContext) {
        repeat(SETTLE_TICKS) {
            val alive = context.fromClient { client ->
                val player = client.player
                if (player != null && player.isDeadOrDying && client.gui.screen() != null) {
                    player.respawn()
                }
                player != null && !player.isDeadOrDying
            }
            if (alive) {
                context.onClient { client -> client.gui.screen()?.let { client.gui.setScreen(null) } }
                return
            }
            context.waitTick()
        }
    }

    private fun sample(context: ClientGameTestContext) = context.fromClient { client ->
        val player = checkNotNull(client.player)
        ParkourSample(player.position(), player.onGround(), player.isDeadOrDying, walker.path)
    }

    /**
     * Teleports the player to [position] and waits until the client has the chunks around [area] that its view
     * distance covers, and until the player stands.
     */
    private fun moveTo(
        context: ClientGameTestContext,
        server: TestServerContext,
        position: Vec3,
        yaw: Float,
        area: List<BlockPos>,
    ) {
        server.runCommand("tp @a ${position.x} ${position.y} ${position.z} $yaw 0")
        context.waitFor({ client ->
            client.player?.position()?.distanceToSqr(position)?.let { it < TELEPORT_EPSILON } == true
        }, WAIT)

        val view = context.fromClient { it.options.renderDistance().get() } - 1
        val center = BlockPos.containing(position)
        val minX = (area.minOf { it.x } - CHUNK_MARGIN) shr 4
        val maxX = (area.maxOf { it.x } + CHUNK_MARGIN) shr 4
        val minZ = (area.minOf { it.z } - CHUNK_MARGIN) shr 4
        val maxZ = (area.maxOf { it.z } + CHUNK_MARGIN) shr 4
        val chunks = (minX..maxX).flatMap { x -> (minZ..maxZ).map { z -> x to z } }.filter { (x, z) ->
            max(abs(x - (center.x shr 4)), abs(z - (center.z shr 4))) <= view
        }
        context.waitFor({ client ->
            val level = client.level ?: return@waitFor false
            chunks.all { (x, z) -> level.chunkSource.getChunk(x, z, ChunkStatus.FULL, false) != null }
        }, CHUNK_WAIT)

        repeat(GROUND_WAIT) {
            if (context.fromClient { it.player?.onGround() == true }) {
                context.waitTicks(SETTLE_AFTER_TELEPORT)
                return
            }
            context.waitTick()
        }
    }

    private fun MinecraftServer.player(): ServerPlayer = playerList.players.first()

    private fun seconds(since: Long) = ((System.nanoTime() - since) / 1.0E9).rounded()

    private companion object {
        const val RENDER_DISTANCE = 8
        const val IN_BLOCK_RADIUS = 0.45
        const val MAX_FOOD = 20
        const val PATH_POINTS = 64
        const val SETTLE_TICKS = 100
        const val SETTLE_AFTER_TELEPORT = 10
        const val TELEPORT_EPSILON = 1.0E-2
        const val WAIT = 200
        const val CHUNK_WAIT = 1200
        const val CHUNK_MARGIN = 16
        const val GROUND_WAIT = 60
    }

}
