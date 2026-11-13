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
package net.ccbluex.liquidbounce.features.module.modules.player.gamebot

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.adapter.toUnderlinedString
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.BlockChangeEvent
import net.ccbluex.liquidbounce.event.events.ChunkDeltaUpdateEvent
import net.ccbluex.liquidbounce.event.events.ChunkLoadEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.HealthUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.MapFormat.ChunkBlocks
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.navigation.Navigator
import net.ccbluex.liquidbounce.utils.navigation.walk.PlacedBlock
import net.ccbluex.liquidbounce.utils.render.route.RouteState
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.BlockTags
import net.minecraft.tags.ItemTags
import net.minecraft.util.Mth
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.PalettedContainer
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.Vec3
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.util.IdentityHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.zip.GZIPOutputStream
import net.ccbluex.liquidbounce.utils.navigation.walk.Path as WalkPath

/**
 * Records a round for review: the loaded chunks as columns and block runs, one row per tick and the events of the
 * round, into `LiquidBounce/map-review/<game>/<started>/` (`.part` until the round ends). The tools in
 * `scripts/bot/map/` read it.
 *
 * Chunk sections are copied on the render thread at most [CHUNKS_PER_TICK] a tick; decoding and every file write run
 * on one worker thread, which owns the writers and palettes.
 */
@Suppress("TooManyFunctions", "CognitiveComplexMethod", "NestedBlockDepth")
internal object MapRecorder : EventListener, MinecraftShortcuts {

    override fun parent() = ModuleGameBot

    private val logger = clientLogger("MapRecorder")

    @Volatile
    private var round: Round? = null

    private class BlockEntityMark(val x: Int, val y: Int, val z: Int, val block: String)

    private class Segment(val index: Int, val dimension: String, val minY: Int, val maxY: Int) {
        // Worker only.
        val palette = MapFormat.Palette()
        val stateIds = IdentityHashMap<BlockState, Int>()
        val chunks = LinkedHashMap<Long, Int>()
        val blockEntities = HashMap<Long, List<BlockEntityMark>>()
        var madeDirs = false
    }

    private class Route(val id: Int, val t0: Int, val path: WalkPath, val goal: Vec3?, val radius: Double)

    private class Round(val game: String, val startedAt: String, val dir: File, val finalDir: File) {
        val io: ExecutorService = Executors.newSingleThreadExecutor {
            Thread(it, "MapRecorder").apply { isDaemon = true }
        }

        @Volatile
        var tick = 0
        var segment: Segment? = null
        var segments = 0
        val queue = LongLinkedOpenHashSet()
        val dirty = LongOpenHashSet()

        var endAt: Int? = null
        var result: Map<String, Any?>? = null

        var route: Route? = null
        var routes = 0
        var viewState = RouteState.NONE
        var pendingAt = 0L
        var stuckCount = 0
        var placement: PlacedBlock? = null
        var placementVerdict: PlacedBlock.Verdict? = null
        var breaks: Set<BlockPos> = emptySet()
        var inventory = ""
        var blockEvents = 0
        var droppedBlockEvents = 0
        var captureNanos = 0L
        var captured = 0
        var maxCaptureNanos = 0L

        // Worker only.
        lateinit var ticks: BufferedWriter
        lateinit var events: BufferedWriter
        lateinit var routeLog: BufferedWriter
        val allSegments = ArrayList<Segment>()
        var dumpNanos = 0L
        var dumped = 0
        var bytes = 0L
        var eventCount = 0
        var failed = false

        fun write(task: Round.() -> Unit) {
            try {
                io.execute {
                    if (failed) {
                        return@execute
                    }
                    try {
                        task()
                    } catch (e: Exception) {
                        failed = true
                        logger.error("Map review writing failed in $dir", e)
                    }
                }
            } catch (_: RejectedExecutionException) {
                // The round is finished already.
            }
        }
    }

    val isRecording: Boolean
        get() = round != null

    /** A new round: anything still recorded is saved as abandoned first. */
    fun begin(game: Game) {
        round?.let { finish(it, drain = true, reason = "abandoned") }

        val folder = File(ConfigSystem.rootFolder, "map-review/${game.name.lowercase()}")
        val stamp = LocalDateTime.now().toUnderlinedString()
        var name = stamp
        var suffix = 1
        while (File(folder, name).exists() || File(folder, "$name.part").exists()) {
            name = "${stamp}_${suffix++}"
        }

        val round = Round(game.name, stamp, File(folder, "$name.part"), File(folder, name))
        round.write {
            dir.mkdirs()
            ticks = writer(File(dir, "ticks.csv.gz"))
            events = writer(File(dir, "events.jsonl.gz"))
            routeLog = writer(File(dir, "routes.jsonl.gz"))
            ticks.append(TICK_HEADER).append('\n')
        }
        this.round = round
        mc.level?.let { openSegment(round, it) }
        logger.info("Recording ${game.name} round into ${round.dir}")
    }

    /** The round is won or lost; recording goes on for [TAIL_TICKS] so the reports after it land, then it is saved. */
    fun end(won: Boolean, reason: String, kills: Int?) {
        val round = round ?: return
        if (round.endAt != null) {
            return
        }

        round.endAt = round.tick + TAIL_TICKS
        round.result = mapOf("won" to won, "reason" to reason.trim(), "kills" to kills, "ticks" to round.tick)
        event("round_end", "won" to won, "reason" to reason.trim(), "kills" to kills, "pos" to mc.player?.position())
    }

    /** Saves what was recorded right away, as when GameBot is turned off. */
    fun abort(reason: String) {
        val round = round ?: return
        finish(round, drain = true, reason = reason)
    }

    /** A line of [ModuleGameBot.report], kept as a `log` event. */
    fun note(message: String) {
        event("log", "text" to message)
    }

    /**
     * One line of `events.jsonl`; [Vec3] and [BlockPos] values become `[x,y,z]`. Nothing happens while no round is
     * recorded.
     */
    fun event(kind: String, vararg fields: Pair<String, Any?>) {
        val round = round ?: return
        val line = StringBuilder(EVENT_CAPACITY).append("{\"t\":").append(round.tick).append(",\"kind\":")
        MapFormat.appendJson(line, kind)
        for ((key, value) in fields) {
            line.append(',')
            MapFormat.appendJson(line, key)
            line.append(':')
            MapFormat.appendJson(line, jsonValue(value))
        }
        line.append("}\n")
        round.write {
            events.append(line)
            eventCount++
        }
    }

    private fun jsonValue(value: Any?): Any? = when (value) {
        is Vec3 -> doubleArrayOf(value.x, value.y, value.z)
        is BlockPos -> intArrayOf(value.x, value.y, value.z)
        else -> value
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val round = round ?: return@handler
        val level = mc.level ?: return@handler
        round.tick++
        val game = ModuleGameBot.games.activeMode

        mc.player?.let { player ->
            val navigator = game.navigator
            navigator?.let { track(round, it) }
            row(round, game, navigator, player)
            if (round.tick % INVENTORY_TICKS == 0) {
                inventory(round)
            }
        }

        if (round.endAt == null && !game.isPlaying) {
            end(won = false, reason = "stopped playing", kills = null)
        }

        val endAt = round.endAt
        if (endAt != null && round.tick >= endAt) {
            finish(round, drain = true, reason = null)
            return@handler
        }

        if (round.tick % REDUMP_TICKS == 0 && !round.dirty.isEmpty()) {
            round.queue.addAll(round.dirty)
            round.dirty.clear()
        }
        captureQueued(round, level, CHUNKS_PER_TICK)

        if (round.tick % FLUSH_TICKS == 0) {
            round.write { flush() }
        }
        if (round.tick % CHECKPOINT_TICKS == 0) {
            round.write { writeMeta() }
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> { event ->
        val round = round ?: return@handler
        val level = event.world
        if (level == null || round.endAt != null || !ModuleGameBot.games.activeMode.isPlaying) {
            // The old world is gone, so chunks still queued cannot be copied any more.
            finish(round, drain = false, reason = "world changed")
        } else {
            openSegment(round, level)
        }
    }

    @Suppress("unused")
    private val chunkLoadHandler = handler<ChunkLoadEvent> { event ->
        val round = round ?: return@handler
        if (round.endAt == null) {
            round.queue.add(ChunkPos.pack(event.x, event.z))
        }
    }

    @Suppress("unused")
    private val blockChangeHandler = handler<BlockChangeEvent> { event ->
        val round = round ?: return@handler
        blockChanged(round, event.blockPos, event.newState)
    }

    @Suppress("unused")
    private val chunkDeltaHandler = handler<ChunkDeltaUpdateEvent> { event ->
        val round = round ?: return@handler
        var logged = 0
        event.packet.runUpdates { pos, state ->
            if (logged++ < DELTA_EVENTS) {
                blockChanged(round, pos.immutable(), state)
            } else {
                round.dirty.add(ChunkPos.pack(pos.x shr 4, pos.z shr 4))
            }
        }
    }

    // Also called before the packet is handed to the render thread, which is skipped.
    @Suppress("unused")
    private val healthHandler = handler<HealthUpdateEvent> { event ->
        if (round != null && mc.isSameThread && event.health != event.previousHealth) {
            event("hp", "hp" to event.health, "food" to event.food, "delta" to event.health - event.previousHealth)
        }
    }

    private fun blockChanged(round: Round, pos: BlockPos, state: BlockState) {
        round.dirty.add(ChunkPos.pack(pos.x shr 4, pos.z shr 4))
        if (round.blockEvents >= MAX_BLOCK_EVENTS) {
            round.droppedBlockEvents++
            return
        }
        round.blockEvents++
        event("block", "pos" to pos, "block" to blockName(state))
    }

    private fun openSegment(round: Round, level: ClientLevel) {
        val segment = Segment(
            round.segments++, level.dimension().identifier().toString(), level.minY, level.maxY,
        )
        round.segment = segment
        round.queue.clear()
        round.dirty.clear()
        round.write { allSegments += segment }
        if (round.segments > 1) {
            event("segment", "index" to segment.index, "dimension" to segment.dimension)
        }

        val player = mc.player ?: return
        val centerX = player.blockX shr 4
        val centerZ = player.blockZ shr 4
        val radius = mc.options.renderDistance().get() + 1
        val keys = ArrayList<Long>()
        for (x in centerX - radius..centerX + radius) {
            for (z in centerZ - radius..centerZ + radius) {
                keys += ChunkPos.pack(x, z)
            }
        }
        keys.sortBy { key ->
            val dx = ChunkPos.getX(key) - centerX
            val dz = ChunkPos.getZ(key) - centerZ
            dx * dx + dz * dz
        }
        keys.forEach(round.queue::add)
    }

    private fun captureQueued(round: Round, level: ClientLevel, budget: Int) {
        val start = System.nanoTime()
        var copied = 0
        while (copied < budget && !round.queue.isEmpty()) {
            if (capture(round, level, round.queue.removeFirstLong())) {
                copied++
            }
        }
        if (copied > 0) {
            val took = System.nanoTime() - start
            round.captureNanos += took
            round.captured += copied
            round.maxCaptureNanos = maxOf(round.maxCaptureNanos, took)
        }
    }

    /** Copies the sections of a loaded chunk for the worker; false when the chunk is not loaded. */
    private fun capture(round: Round, level: ClientLevel, key: Long): Boolean {
        val segment = round.segment ?: return false
        val chunkX = ChunkPos.getX(key)
        val chunkZ = ChunkPos.getZ(key)
        val chunk = level.chunkSource.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) ?: return false
        val sections = Array(chunk.sections.size) { index ->
            chunk.sections[index].takeUnless { it.hasOnlyAir() }?.states?.copy()
        }
        val marks = chunk.blockEntities.map { (pos, entity) ->
            BlockEntityMark(pos.x, pos.y, pos.z, blockName(entity.blockState))
        }
        val tick = round.tick
        round.write { dump(segment, chunkX, chunkZ, sections, marks, tick) }
        return true
    }

    private fun Round.dump(
        segment: Segment,
        chunkX: Int,
        chunkZ: Int,
        sections: Array<PalettedContainer<BlockState>?>,
        marks: List<BlockEntityMark>,
        tick: Int,
    ) {
        val start = System.nanoTime()
        val blocks = decode(segment, sections)
        val columns = MapFormat.gzip(MapFormat.columns(blocks, segment.palette.blocks))
        val world = MapFormat.gzip(MapFormat.rle(blocks))

        val segmentDir = File(dir, "seg${segment.index}")
        if (!segment.madeDirs) {
            File(segmentDir, "columns").mkdirs()
            File(segmentDir, "world").mkdirs()
            segment.madeDirs = true
        }
        File(segmentDir, "columns/c.$chunkX.$chunkZ.bin.gz").writeBytes(columns)
        File(segmentDir, "world/c.$chunkX.$chunkZ.rle.gz").writeBytes(world)

        val key = ChunkPos.pack(chunkX, chunkZ)
        segment.chunks[key] = tick
        segment.blockEntities[key] = marks
        bytes += columns.size + world.size
        dumped++
        dumpNanos += System.nanoTime() - start
    }

    private fun decode(segment: Segment, sections: Array<PalettedContainer<BlockState>?>): ChunkBlocks {
        val lowest = sections.indexOfFirst { it != null }
        if (lowest < 0) {
            return ChunkBlocks(segment.minY, 0, CharArray(0))
        }
        val highest = sections.indexOfLast { it != null }
        val height = (highest - lowest + 1) * SECTION
        val ids = CharArray(height * MapFormat.COLUMNS)
        var lastState: BlockState? = null
        var lastId = 0
        for (index in lowest..highest) {
            val section = sections[index] ?: continue
            val offset = (index - lowest) * SECTION * MapFormat.COLUMNS
            for (y in 0 until SECTION) {
                for (z in 0 until SECTION) {
                    for (x in 0 until SECTION) {
                        val state = section.get(x, y, z)
                        if (state !== lastState) {
                            lastState = state
                            lastId = segment.id(state)
                        }
                        ids[offset + (y * SECTION + z) * SECTION + x] = lastId.toChar()
                    }
                }
            }
        }
        return ChunkBlocks(segment.minY + lowest * SECTION, height, ids)
    }

    private fun Segment.id(state: BlockState): Int {
        if (state.isAir) {
            return 0
        }
        return stateIds.getOrPut(state) {
            val name = blockName(state)
            palette.id(name) {
                val color = state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
                val colored = color != MapColor.NONE
                MapFormat.Block(
                    name = name,
                    argb = if (colored) color.calculateARGBColor(MapColor.Brightness.HIGH) else UNCOLORED_ARGB,
                    colored = colored,
                    solid = !state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty,
                    fluid = !state.fluidState.isEmpty,
                    leaves = state.`is`(BlockTags.LEAVES),
                    blockEntity = state.hasBlockEntity(),
                )
            }
        }
    }

    private fun blockName(state: BlockState) = BuiltInRegistries.BLOCK.getKey(state.block).toString()

    private fun row(round: Round, game: Game, navigator: Navigator?, player: LocalPlayer) {
        val view = navigator?.view
        val position = player.position()
        val goal = view?.goal
        val out = StringBuilder(ROW_CAPACITY)
        out.append(round.tick).append(',').append(round.segment?.index ?: 0).append(',')
        fixed(out, position.x, 2)
        fixed(out, position.y, 2)
        fixed(out, position.z, 2)
        fixed(out, Mth.wrapDegrees(player.yRot).toDouble(), 1)
        fixed(out, Mth.wrapDegrees(RotationManager.currentRotation?.yaw ?: player.yRot).toDouble(), 1)
        flag(out, player.onGround())
        fixed(out, player.health.toDouble(), 1)
        out.append(player.foodData.foodLevel).append(',')
        flag(out, player.isSprinting)
        flag(out, player.isShiftKeyDown)
        out.append(game.phaseName ?: "").append(',')
        out.append(game.action ?: "").append(',')
        val route = round.route
        out.append(route?.id ?: 0).append(',')
        out.append(route?.let { nearestPoint(it.path, position) } ?: -1).append(',')
        fixed(out, goal?.x ?: Double.NaN, 2)
        fixed(out, goal?.y ?: Double.NaN, 2)
        fixed(out, goal?.z ?: Double.NaN, 2)
        fixed(out, if (goal != null) view.goalRadius else Double.NaN, 1)
        fixed(out, goal?.let { position.horizontalDistanceTo(it) } ?: Double.NaN, 2)
        flag(out, navigator?.isActing == true)
        flag(out, navigator?.isBuilding == true)
        flag(out, navigator?.isStranded == true)
        out.append(placeableBlocks()).append(',')
        navigator?.placement?.let { out.append(it.verdict.tag).append(':').append(it.pos.x).append(':')
            .append(it.pos.y).append(':').append(it.pos.z) }
        out.append('\n')
        round.write { ticks.append(out) }
    }

    private fun fixed(out: StringBuilder, value: Double, decimals: Int) {
        MapFormat.appendFixed(out, value, decimals)
        out.append(',')
    }

    private fun flag(out: StringBuilder, value: Boolean) {
        out.append(if (value) '1' else '0').append(',')
    }

    private fun nearestPoint(path: WalkPath, position: Vec3): Int {
        val points = path.points
        var best = 0
        var bestDistance = Double.MAX_VALUE
        for (index in points.indices) {
            val distance = points[index].distanceToSqr(position)
            if (distance < bestDistance) {
                bestDistance = distance
                best = index
            }
        }
        return best
    }

    private fun placeableBlocks() = Slots.OffhandWithHotbar.sumOf { slot ->
        val stack = slot.itemStack
        if (ScaffoldBlockItemSelection.isValidBlock(stack)) stack.count else 0
    }

    /** Turns changes of the navigator into `route`, `search`, `stuck`, `place` and `break` events. */
    private fun track(round: Round, navigator: Navigator) {
        val view = navigator.view
        val path = navigator.path
        if (path !== round.route?.path) {
            closeRoute(round)
            if (path != null) {
                val route = Route(++round.routes, round.tick, path, view.goal, view.goalRadius)
                round.route = route
                event(
                    "route", "id" to route.id, "start" to path.start, "end" to path.end, "goal" to route.goal,
                    "radius" to route.radius, "points" to path.points.size, "cost" to path.cost,
                    "complete" to path.complete,
                )
            }
        }

        val state = view.state
        if (state != round.viewState) {
            val ms = System.currentTimeMillis() - round.pendingAt
            when (state) {
                RouteState.PENDING -> {
                    round.pendingAt = System.currentTimeMillis()
                    event("search", "phase" to "start", "goal" to view.goal)
                }
                RouteState.ACTIVE -> if (round.viewState == RouteState.PENDING) {
                    event("search", "phase" to "found", "goal" to view.goal, "ms" to ms)
                }
                RouteState.INVALID -> event("search", "phase" to "failed", "goal" to view.goal, "ms" to ms)
                RouteState.DONE -> event("search", "phase" to "done", "goal" to view.goal)
                RouteState.NONE -> {}
            }
            round.viewState = state
        }

        if (view.stuckCount != round.stuckCount) {
            round.stuckCount = view.stuckCount
            event("stuck", "pos" to view.stuckAt, "goal" to view.goal, "route" to round.route?.id)
        }

        val placement = navigator.placement
        val previous = round.placement
        if (previous != null && (placement !== previous || previous.verdict != round.placementVerdict) &&
            previous.verdict != PlacedBlock.Verdict.PENDING) {
            event("place", "pos" to previous.pos, "verdict" to previous.verdict.tag, "route" to round.route?.id)
        }
        if (placement !== previous || placement?.verdict != round.placementVerdict) {
            round.placement = placement
            round.placementVerdict = placement?.verdict
        }

        val breaks = view.breaks
        if (round.breaks.isNotEmpty() || breaks.isNotEmpty()) {
            for (pos in round.breaks) {
                if (pos !in breaks && world.getBlockState(pos).isAir) {
                    event("break", "pos" to pos, "route" to round.route?.id)
                }
            }
            round.breaks = if (breaks.isEmpty()) emptySet() else breaks.toHashSet()
        }
    }

    private fun closeRoute(round: Round) {
        val route = round.route ?: return
        round.route = null
        val path = route.path
        val line = StringBuilder(path.points.size * ROUTE_POINT_CHARS + ROW_CAPACITY)
        MapFormat.appendJson(
            line,
            mapOf(
                "id" to route.id, "t0" to route.t0, "t1" to round.tick,
                "start" to jsonValue(path.start), "end" to jsonValue(path.end), "goal" to jsonValue(route.goal),
                "radius" to route.radius,
                "points" to path.points.map(::jsonValue),
                "moves" to path.steps.map { it.move.name },
                "cost" to path.cost, "complete" to path.complete,
            ),
        )
        line.append('\n')
        round.write { routeLog.append(line) }
    }

    private fun inventory(round: Round) {
        val blocks = placeableBlocks()
        val sword = Slots.Hotbar.any { it.itemStack.`is`(ItemTags.SWORDS) }
        val food = Slots.OffhandWithHotbar.sumOf {
            if (it.itemStack.has(DataComponents.FOOD)) it.itemStack.count else 0
        }
        val line = "$blocks,$sword,$food"
        if (line != round.inventory) {
            round.inventory = line
            event("inventory", "blocks" to blocks, "sword" to sword, "food" to food)
        }
    }

    /** Copies what is still queued unless the world is gone, then closes the files and drops `.part`. */
    private fun finish(round: Round, drain: Boolean, reason: String?) {
        if (reason != null && round.result == null) {
            round.result = mapOf("won" to false, "reason" to reason, "kills" to null, "ticks" to round.tick)
            event("round_end", "won" to false, "reason" to reason, "pos" to mc.player?.position())
        }
        closeRoute(round)
        round.placement?.let {
            event("place", "pos" to it.pos, "verdict" to it.verdict.tag, "route" to null)
        }
        if (drain) {
            mc.level?.let { level ->
                round.queue.addAll(round.dirty)
                round.dirty.clear()
                captureQueued(round, level, Int.MAX_VALUE)
            }
        }
        val leftOver = round.queue.size
        this.round = null

        round.write {
            flush()
            ticks.close()
            events.close()
            routeLog.close()
            writeMeta()
            writeSummary(leftOver)
            Files.move(dir.toPath(), finalDir.toPath(), StandardCopyOption.ATOMIC_MOVE)
            logger.info("Saved round review to $finalDir: $dumped chunk dumps, ${bytes / KILOBYTE} KiB of map")
        }
        round.io.shutdown()
    }

    private fun Round.flush() {
        ticks.flush()
        events.flush()
        routeLog.flush()
    }

    private fun Round.writeMeta() {
        val segmentsJson = allSegments.map { segment ->
            var minX = Int.MAX_VALUE
            var minZ = Int.MAX_VALUE
            var maxX = Int.MIN_VALUE
            var maxZ = Int.MIN_VALUE
            val chunks = segment.chunks.map { (key, tick) ->
                val x = ChunkPos.getX(key)
                val z = ChunkPos.getZ(key)
                minX = minOf(minX, x * SECTION)
                minZ = minOf(minZ, z * SECTION)
                maxX = maxOf(maxX, x * SECTION + SECTION - 1)
                maxZ = maxOf(maxZ, z * SECTION + SECTION - 1)
                mapOf("cx" to x, "cz" to z, "t" to tick)
            }
            mapOf(
                "index" to segment.index, "dimension" to segment.dimension,
                "minY" to segment.minY, "maxY" to segment.maxY,
                "bounds" to if (chunks.isEmpty()) {
                    null
                } else {
                    mapOf("minX" to minX, "minZ" to minZ, "maxX" to maxX, "maxZ" to maxZ)
                },
                "chunks" to chunks,
                "palette" to segment.palette.blocks.mapIndexed { id, block ->
                    mapOf(
                        "id" to id, "block" to block.name, "argb" to block.argb, "colored" to block.colored.bit,
                        "solid" to block.solid.bit, "fluid" to block.fluid.bit, "leaves" to block.leaves.bit,
                        "be" to block.blockEntity.bit,
                    )
                },
                "blockEntities" to segment.blockEntities.values.flatten().map {
                    mapOf("x" to it.x, "y" to it.y, "z" to it.z, "block" to it.block)
                },
            )
        }
        val meta = mapOf(
            "version" to MapFormat.VERSION, "instance" to mc.gameDirectory.name, "game" to game,
            "startedAt" to startedAt, "endedAt" to LocalDateTime.now().toUnderlinedString(),
            "result" to result, "segments" to segmentsJson,
        )
        File(dir, "map.json").writeText(StringBuilder().also { MapFormat.appendJson(it, meta) }.toString())
    }

    private fun Round.writeSummary(leftOver: Int) {
        val summary = mapOf(
            "ticks" to tick, "chunkCopies" to captured, "chunkDumps" to dumped,
            "copyMs" to captureNanos / NANOS_PER_MILLI.toDouble(),
            "maxCopyTickMs" to maxCaptureNanos / NANOS_PER_MILLI.toDouble(),
            "dumpMs" to dumpNanos / NANOS_PER_MILLI.toDouble(), "mapBytes" to bytes, "events" to eventCount,
            "blockEvents" to blockEvents, "droppedBlockEvents" to droppedBlockEvents, "queuedLeft" to leftOver,
        )
        File(dir, "summary.json").writeText(StringBuilder().also { MapFormat.appendJson(it, summary) }.toString())
    }

    private val Boolean.bit: Int
        get() = if (this) 1 else 0

    private fun writer(file: File) =
        BufferedWriter(OutputStreamWriter(GZIPOutputStream(FileOutputStream(file), GZIP_BUFFER, true), Charsets.UTF_8))

    private const val TICK_HEADER = "t,seg,x,y,z,yaw,serverYaw,onGround,hp,food,sprint,sneak,phase,action,route,node," +
        "goalX,goalY,goalZ,goalR,dist,acting,building,stranded,blocks,place"

    private const val SECTION = 16
    private const val CHUNKS_PER_TICK = 4
    private const val TAIL_TICKS = 40
    private const val INVENTORY_TICKS = 40
    private const val REDUMP_TICKS = 200
    private const val FLUSH_TICKS = 20
    private const val CHECKPOINT_TICKS = 1200
    private const val DELTA_EVENTS = 64
    private const val MAX_BLOCK_EVENTS = 50_000

    /** Blocks without a map color, such as glass, drawn light grey and half transparent. */
    private const val UNCOLORED_ARGB = 0x80CCCCCC.toInt()

    private const val EVENT_CAPACITY = 96
    private const val ROW_CAPACITY = 160
    private const val ROUTE_POINT_CHARS = 24
    private const val GZIP_BUFFER = 8192
    private const val KILOBYTE = 1024
    private const val NANOS_PER_MILLI = 1_000_000L

}
