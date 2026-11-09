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

import com.google.gson.GsonBuilder
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.ccbluex.liquidbounce.utils.client.logger
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.QuartPos
import net.minecraft.core.SectionPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BiomeTags
import net.minecraft.tags.BlockTags
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.world.Difficulty
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.gamerules.GameRules
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * AutoWalk walks the way the player faces through generated terrain for thousands of blocks. Only runs when
 * `-PgameTestLongWalk=<blocks>` is given. Every spot where it gets stuck, hurt, killed, walks far around or stands
 * still while searching is written to `incidents.jsonl` in the output directory with a screenshot, and the player is
 * moved past spots it cannot leave, so a single run finds many of them.
 */
class AutoWalkLongDistanceGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        val distance = System.getProperty(PROPERTY)?.toDoubleOrNull() ?: return
        val settings = Settings(
            distance = distance,
            seed = System.getProperty("$PROPERTY.seed") ?: DEFAULT_SEED,
            yaw = System.getProperty("$PROPERTY.yaw")?.toFloatOrNull() ?: 0f,
            minutes = System.getProperty("$PROPERTY.minutes")?.toDoubleOrNull() ?: DEFAULT_MINUTES,
            output = Path.of(System.getProperty("$PROPERTY.output") ?: "longwalk").toAbsolutePath(),
        )
        Files.createDirectories(settings.output.resolve("shots"))

        context.waitForClient()
        // Fewer chunks keep the client and the integrated server lighter over an hour of generating terrain.
        context.onClient { mc ->
            mc.options.renderDistance().set(VIEW_DISTANCE)
            mc.options.simulationDistance().set(VIEW_DISTANCE)
        }
        context.worldBuilder()
            .setUseConsistentSettings(false)
            .adjustSettings { creator -> configureWorld(creator, settings.seed) }
            .create()
            .use { world -> LongWalk(context, world.server, settings).run() }
    }

    private fun configureWorld(creator: WorldCreationUiState, seed: String) {
        creator.setSeed(seed)
        creator.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)
        creator.setDifficulty(Difficulty.PEACEFUL)
        creator.setAllowCommands(true)
        val rules = creator.gameRules
        rules.set(GameRules.ADVANCE_TIME, false, null)
        rules.set(GameRules.ADVANCE_WEATHER, false, null)
        rules.set(GameRules.SPAWN_MOBS, false, null)
        rules.set(GameRules.IMMEDIATE_RESPAWN, true, null)
        rules.set(GameRules.RESPAWN_RADIUS, 0, null)
    }

    private class Settings(
        val distance: Double,
        val seed: String,
        val yaw: Float,
        val minutes: Double,
        val output: Path,
    )

    private class Sample(
        val player: Any,
        val position: Vec3,
        val health: Float,
        val aheadLoaded: Boolean,
        val searching: Boolean?,
    )

    @Suppress("TooManyFunctions")
    private class LongWalk(
        private val context: ClientGameTestContext,
        private val server: TestServerContext,
        private val settings: Settings,
    ) {
        private val gson = GsonBuilder().disableHtmlEscaping().serializeSpecialFloatingPointValues().create()
        private val yawRadians = Math.toRadians(settings.yaw.toDouble())
        private val heading = Vec3(-sin(yawRadians), 0.0, cos(yawRadians))

        private lateinit var start: Vec3
        private var ticks = 0
        private val startedAt = System.nanoTime()
        private val incidents = mutableMapOf<String, Int>()
        private var incidentCount = 0
        private var sprintTicks = 0
        private var waterTicks = 0

        private var walked = 0.0
        private var progress = 0.0
        private var teleported = 0.0
        private var chunkWaitTicks = 0
        private var yawFixes = 0

        private var markProgress = 0.0
        private var markTick = 0
        private val detourWindow = ArrayDeque<Pair<Double, Double>>()
        private val still = ArrayDeque<Pair<Vec3, Boolean>>()
        private var lastStall: Vec3? = null

        private var nextMilestone = MILESTONE
        private var milestoneTick = 0
        private var milestoneNanos = startedAt

        fun run() {
            server.runCommand("time set noon")
            server.runCommand("weather clear")
            start = context.fromClient { checkNotNull(it.player).position() }
            server.runCommand("tp @a ${start.x} ${start.y} ${start.z} ${settings.yaw} $PITCH")
            context.waitTicks(SETTLE_TICKS)
            start = context.fromClient { checkNotNull(it.player).position() }
            log("start at ${format(start)}, seed ${settings.seed}, yaw ${settings.yaw}, target ${settings.distance}")
            server.computeOnServer<List<String>, RuntimeException> { s -> survey(s.overworld()) }.forEach(::log)

            context.onClient {
                ModuleAutoWalk.containedValues.first { value -> value.name == "Mode" }.setByString("Direction")
                ModuleAutoWalk.enabled = true
            }
            try {
                walk()
            } finally {
                context.onClient { ModuleAutoWalk.enabled = false }
                writeSummary()
            }
        }

        /**
         * The biomes along the heading and the share of ocean along each of the four headings, from the biome noise
         * alone, so a seed and yaw can be picked without generating any chunk.
         */
        private fun survey(level: ServerLevel): List<String> {
            val chunks = level.chunkSource
            val biomes = chunks.generator.biomeSource.createUncachedResolver(chunks.randomState())
            fun biomeAt(yaw: Double, distance: Double): Holder<Biome> {
                val radians = Math.toRadians(yaw)
                val x = Mth.floor(start.x - sin(radians) * distance)
                val z = Mth.floor(start.z + cos(radians) * distance)
                return biomes.getNoiseBiome(
                    QuartPos.fromBlock(x), QuartPos.fromBlock(SURVEY_Y), QuartPos.fromBlock(z)
                )
            }
            val distances = generateSequence(0.0) { it + SURVEY_STEP }.takeWhile { it <= settings.distance }.toList()

            val oceans = (0 until 4).joinToString { quarter ->
                val yaw = quarter * 90.0
                val wet = distances.count { biomeAt(yaw, it).`is`(BiomeTags.IS_OCEAN) }
                "yaw $yaw: ${wet * 100 / distances.size}% ocean"
            }
            val route = buildList<String> {
                var last: String? = null
                for (distance in distances) {
                    val name = biomeAt(settings.yaw.toDouble(), distance).registeredName.substringAfter(':')
                    if (name != last) {
                        add("${distance.toInt()}:$name")
                        last = name
                    }
                }
            }
            return listOf("ocean share by heading: $oceans", "biomes ahead: ${route.joinToString(" ")}")
        }

        @Suppress("CognitiveComplexMethod", "LongMethod", "CyclomaticComplexMethod")
        private fun walk() {
            val deadline = startedAt + (settings.minutes * NANOS_PER_MINUTE).toLong()
            var last = sample()
            while (progress < settings.distance && System.nanoTime() < deadline) {
                context.waitTick()
                ticks++
                val current = sample()

                if (current.player !== last.player) {
                    incident("DIED", last.position, mapOf("health" to last.health))
                    nudge(last.position)
                    last = sample()
                    continue
                }

                val step = current.position.subtract(last.position)
                walked += step.horizontalDistance()
                progress = current.position.subtract(start).dot(heading)
                if (!current.aheadLoaded) {
                    chunkWaitTicks++
                    markTick++
                }

                if (current.health < last.health) {
                    hurt(current, last.health - current.health)
                }

                if (progress >= markProgress + STUCK_PROGRESS) {
                    markProgress = progress
                    markTick = ticks
                } else if (ticks - markTick > STUCK_TICKS) {
                    incident("STUCK", current.position, mapOf("progressSince" to progress - markProgress))
                    nudge(current.position)
                    last = sample()
                    continue
                }

                if (detour(current)) {
                    last = sample()
                    continue
                }
                stall(current)

                if (progress >= nextMilestone) {
                    milestone(current.position)
                }
                last = current
            }
            log("finished: progress ${format(progress)} of ${settings.distance} after $ticks ticks")
        }

        private fun sample(): Sample = context.fromClient { mc ->
            val player = checkNotNull(mc.player)
            if (abs(Mth.wrapDegrees(player.yRot - settings.yaw)) > YAW_TOLERANCE) {
                player.yRot = settings.yaw
                yawFixes++
            }
            if (player.isSprinting) {
                sprintTicks++
            }
            if (player.isInWater) {
                waterTicks++
            }
            val ahead = player.position().add(heading.scale(LOAD_AHEAD))
            val loaded = mc.level?.hasChunk(
                SectionPos.blockToSectionCoord(ahead.x), SectionPos.blockToSectionCoord(ahead.z)
            ) == true
            Sample(
                player = player,
                position = player.position(),
                health = player.health,
                aheadLoaded = loaded,
                searching = WalkerProbe.searching(),
            )
        }

        private fun hurt(current: Sample, amount: Float) {
            val source = server.computeOnServer<String, RuntimeException> { s ->
                s.playerList.players.firstOrNull()?.lastDamageSource?.type()?.msgId() ?: "unknown"
            }
            incident("HURT", current.position, mapOf("amount" to amount, "source" to source))
        }

        /**
         * Walked more than [DETOUR_FACTOR] times the progress over the last [DETOUR_WALK] blocks.
         */
        private fun detour(current: Sample): Boolean {
            if (ticks % DETOUR_SAMPLE != 0) {
                return false
            }
            detourWindow.addLast(walked to progress)
            var from: Pair<Double, Double>? = null
            while (detourWindow.size > 1 && walked - detourWindow[1].first >= DETOUR_WALK) {
                detourWindow.removeFirst()
            }
            if (walked - detourWindow.first().first >= DETOUR_WALK) {
                from = detourWindow.first()
            }
            if (from == null) {
                return false
            }

            val walkedThere = walked - from.first
            val gained = progress - from.second
            if (gained * DETOUR_FACTOR >= walkedThere) {
                return false
            }
            incident("DETOUR", current.position, mapOf("walked" to walkedThere, "progress" to gained))
            nudge(current.position)
            return true
        }

        /**
         * Standing still for [STALL_TICKS], reported once per spot, with how many of those ticks a path was searched.
         */
        private fun stall(current: Sample) {
            still.addLast(current.position to (current.searching == true))
            if (still.size > STALL_TICKS) {
                still.removeFirst()
            }
            val moved = still.first().first.subtract(current.position).horizontalDistance()
            if (still.size < STALL_TICKS || moved >= STALL_DISTANCE || !current.aheadLoaded) {
                return
            }
            if (lastStall?.let { it.distanceTo(current.position) < STALL_SPOT } == true) {
                return
            }
            lastStall = current.position
            incident(
                "STALL",
                current.position,
                mapOf("searchingTicks" to still.count { it.second }, "ofTicks" to still.size),
            )
        }

        private fun milestone(position: Vec3) {
            val now = System.nanoTime()
            val tickRate = (ticks - milestoneTick) / ((now - milestoneNanos) / NANOS_PER_SECOND)
            val gameMinutes = ticks / TICKS_PER_MINUTE
            log(
                "${format(progress)} blocks at ${format(position)}: $ticks ticks, " +
                    "${format((now - startedAt) / NANOS_PER_SECOND)} s real, ${format(tickRate)} ticks/s, " +
                    "${format((progress - teleported) / gameMinutes)} blocks/game min, walked ${format(walked)}, " +
                    "chunk waits $chunkWaitTicks, sprinting $sprintTicks, in water $waterTicks, incidents $incidents"
            )
            milestoneTick = ticks
            milestoneNanos = now
            nextMilestone += MILESTONE
        }

        private fun incident(kind: String, position: Vec3, details: Map<String, Any?>) {
            incidentCount++
            incidents.merge(kind, 1, Int::plus)
            val surroundings = server.computeOnServer<Map<String, Any?>, RuntimeException> { s ->
                describe(s.overworld(), position)
            }
            val walker = context.fromClient { WalkerProbe.state() }
            val name = String.format(Locale.ROOT, "longwalk-%03d-%s", incidentCount, kind.lowercase())
            val shot = runCatching {
                val taken = context.takeScreenshot(name)
                val target = settings.output.resolve("shots").resolve(taken.fileName)
                Files.copy(taken, target, StandardCopyOption.REPLACE_EXISTING)
                target.toString()
            }.getOrElse { "failed: ${it.message}" }

            val record = linkedMapOf(
                "n" to incidentCount,
                "kind" to kind,
                "seed" to settings.seed,
                "yaw" to settings.yaw,
                "tick" to ticks,
                "realSeconds" to (System.nanoTime() - startedAt) / NANOS_PER_SECOND,
                "progress" to progress,
                "walked" to walked,
                "position" to listOf(position.x, position.y, position.z),
                "block" to BlockPos.containing(position).toShortString(),
                "lateral" to lateral(position),
                "details" to details,
                "surroundings" to surroundings,
                "walker" to walker,
                "screenshot" to shot,
            )
            append("incidents.jsonl", gson.toJson(record))
            log("$kind #$incidentCount at ${format(position)} (progress ${format(progress)}): $details, $surroundings")
        }

        /**
         * Puts the player on the surface [NUDGE] blocks past [position] along the heading and starts AutoWalk anew.
         */
        private fun nudge(position: Vec3) {
            val target = server.computeOnServer<Vec3, RuntimeException> { s -> landing(s, position) }
            context.onClient { ModuleAutoWalk.enabled = false }
            server.runCommand("tp @a ${target.x} ${target.y} ${target.z} ${settings.yaw} $PITCH")
            context.waitFor({ mc: Minecraft ->
                mc.player?.position()?.distanceToSqr(target)?.let { it < 1.0 } == true
            }, WAIT)
            context.waitTicks(SETTLE_TICKS)
            context.onClient { ModuleAutoWalk.enabled = true }

            val before = progress
            val now = context.fromClient { checkNotNull(it.player).position() }
            progress = now.subtract(start).dot(heading)
            teleported += progress - before
            markProgress = progress
            markTick = ticks
            detourWindow.clear()
            still.clear()
        }

        private fun landing(server: MinecraftServer, position: Vec3): Vec3 {
            val level = server.overworld()
            var distance = NUDGE
            while (distance < NUDGE_MAX) {
                val point = position.add(heading.scale(distance))
                val x = Mth.floor(point.x)
                val z = Mth.floor(point.z)
                level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))
                val y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)
                val ground = BlockPos(x, y - 1, z)
                val dry = level.getFluidState(ground).isEmpty
                val free = level.getBlockState(ground.above()).isAir && level.getBlockState(ground.above(2)).isAir
                if (dry && free) {
                    return Vec3(x + 0.5, y.toDouble(), z + 0.5)
                }
                distance += NUDGE_STEP
            }

            val point = position.add(heading.scale(NUDGE))
            val x = Mth.floor(point.x)
            val z = Mth.floor(point.z)
            return Vec3(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z).toDouble(), z + 0.5)
        }

        @Suppress("CognitiveComplexMethod", "LongMethod", "CyclomaticComplexMethod")
        private fun describe(level: ServerLevel, position: Vec3): Map<String, Any?> {
            val feet = BlockPos.containing(position)
            val surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, feet.x, feet.z)
            var water = 0
            var lava = 0
            var trees = 0
            for (pos in BlockPos.betweenClosed(feet.offset(-AROUND, -3, -AROUND), feet.offset(AROUND, 3, AROUND))) {
                val state = level.getBlockState(pos)
                val fluid = state.fluidState
                when {
                    fluid.`is`(FluidTags.WATER) -> water++
                    fluid.`is`(FluidTags.LAVA) -> lava++
                    state.`is`(BlockTags.LOGS) || state.`is`(BlockTags.LEAVES) -> trees++
                }
            }

            val ahead = ArrayList<String>()
            var cliff = false
            var ravine = false
            for (d in 1..AHEAD) {
                val point = position.add(heading.scale(d.toDouble()))
                val x = Mth.floor(point.x)
                val z = Mth.floor(point.z)
                val rise = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - feet.y
                if (d <= CLIFF_RANGE && abs(rise) >= CLIFF) {
                    cliff = true
                }
                if (rise <= -RAVINE) {
                    ravine = true
                }
                if (d <= AHEAD_BLOCKS) {
                    val column = BlockPos(x, feet.y, z)
                    ahead += "$d:${rise}:${id(level, column.below())}/${id(level, column)}/${id(level, column.above())}"
                }
            }

            val tags = buildList {
                if (water > 0) add("water")
                if (lava > 0) add("lava")
                if (trees > TREE_BLOCKS) add("tree")
                if (surface - feet.y >= CAVE_DEPTH && !level.canSeeSky(feet)) add("cave")
                if (cliff) add("cliff")
                if (ravine) add("ravine")
            }
            return linkedMapOf(
                "biome" to level.getBiome(feet).registeredName,
                "tags" to tags,
                "below" to id(level, feet.below()),
                "feet" to id(level, feet),
                "head" to id(level, feet.above()),
                "surfaceY" to surface,
                "water" to water,
                "lava" to lava,
                "treeBlocks" to trees,
                "ahead" to ahead,
            )
        }

        private fun id(level: ServerLevel, pos: BlockPos) =
            BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).block).path

        private fun lateral(position: Vec3): Double {
            val offset = position.subtract(start)
            return offset.x * heading.z - offset.z * heading.x
        }

        private fun writeSummary() {
            val realSeconds = (System.nanoTime() - startedAt) / NANOS_PER_SECOND
            val summary = linkedMapOf(
                "seed" to settings.seed,
                "yaw" to settings.yaw,
                "start" to listOf(start.x, start.y, start.z),
                "target" to settings.distance,
                "progress" to progress,
                "teleported" to teleported,
                "walked" to walked,
                "ticks" to ticks,
                "realSeconds" to realSeconds,
                "ticksPerSecond" to ticks / realSeconds,
                "blocksPerGameMinute" to (progress - teleported) / (ticks / TICKS_PER_MINUTE),
                "chunkWaitTicks" to chunkWaitTicks,
                "sprintTicks" to sprintTicks,
                "waterTicks" to waterTicks,
                "yawFixes" to yawFixes,
                "incidents" to incidents,
            )
            Files.writeString(settings.output.resolve("summary.json"), gson.toJson(summary))
            log("summary: $summary")
        }

        private fun append(file: String, line: String) {
            Files.writeString(
                settings.output.resolve(file),
                line + "\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
            )
        }

        private fun log(message: String) {
            logger.info("[LongWalk] $message")
            append("progress.log", message)
        }

        private fun format(value: Double) = String.format(Locale.ROOT, "%.1f", value)

        private fun format(position: Vec3) =
            String.format(Locale.ROOT, "%.1f %.1f %.1f", position.x, position.y, position.z)
    }

    /**
     * Reads the state of AutoWalk's navigator through reflection, as far as its fields are there.
     */
    private object WalkerProbe {
        private val navigator: Any? by lazy { read(ModuleAutoWalk, "navigator") }
        private val planner: Any? by lazy { navigator?.let { read(it, "planner") } }

        fun searching(): Boolean? = runCatching {
            planner?.let { !(it.javaClass.getMethod("isIdle").invoke(it) as Boolean) }
        }.getOrNull()

        fun state(): Map<String, Any?> {
            val navigator = navigator ?: return mapOf("unreachable" to true)
            val follower = read(navigator, "follower")
            val step = follower?.let { runCatching { it.javaClass.getMethod("getStep").invoke(it) }.getOrNull() }
            return linkedMapOf(
                "searching" to searching(),
                "navigator" to dump(navigator),
                "planner" to planner?.let(::dump),
                "search" to planner?.let { read(it, "search") }?.let(::dump),
                "follower" to follower?.let(::dump),
                "step" to step?.let(::dump),
                "jump" to step?.let { read(it, "jump") }?.let(::dump),
            )
        }

        private fun dump(target: Any): Map<String, String> = fields(target.javaClass)
            .filter { !Modifier.isStatic(it.modifiers) }
            .mapNotNull { field ->
                val value = runCatching { field.get(target) }.getOrNull()
                val text = when (value) {
                    null -> "null"
                    is Number, is Boolean, is String, is Enum<*>, is Vec3, is BlockPos -> value.toString()
                    is Collection<*> -> "size=${value.size}"
                    else -> when (field.name) {
                        "path" -> "present"
                        "result" -> value.javaClass.simpleName
                        "key" -> value.toString()
                        else -> null
                    }
                } ?: return@mapNotNull null
                field.name to text
            }
            .toMap()

        private fun read(target: Any, name: String): Any? = fields(target.javaClass)
            .firstOrNull { it.name == name }
            ?.let { runCatching { it.get(target) }.getOrNull() }

        private fun fields(type: Class<*>): List<Field> = generateSequence(type) { it.superclass }
            .flatMap { it.declaredFields.asSequence() }
            .onEach { runCatching { it.isAccessible = true } }
            .toList()
    }

    private companion object {
        const val PROPERTY = "net.ccbluex.liquidbounce.gametest.longWalk"
        const val DEFAULT_SEED = "-4172144997902289642"
        const val DEFAULT_MINUTES = 60.0
        const val PITCH = 15
        const val WAIT = 200
        const val SETTLE_TICKS = 20
        const val YAW_TOLERANCE = 0.01f
        const val LOAD_AHEAD = 24.0
        const val SURVEY_Y = 64
        const val SURVEY_STEP = 32.0
        const val VIEW_DISTANCE = 6

        const val MILESTONE = 250.0
        const val STUCK_PROGRESS = 5.0
        const val STUCK_TICKS = 30 * 20
        const val DETOUR_SAMPLE = 20
        const val DETOUR_WALK = 200.0
        const val DETOUR_FACTOR = 3.0
        const val STALL_TICKS = 5 * 20
        const val STALL_DISTANCE = 1.0
        const val STALL_SPOT = 4.0

        const val NUDGE = 20.0
        const val NUDGE_STEP = 4.0
        const val NUDGE_MAX = 120.0

        const val AROUND = 5
        const val AHEAD = 12
        const val AHEAD_BLOCKS = 4
        const val CLIFF_RANGE = 6
        const val CLIFF = 4
        const val RAVINE = 10
        const val CAVE_DEPTH = 6
        const val TREE_BLOCKS = 4

        const val TICKS_PER_MINUTE = 1200.0
        const val NANOS_PER_SECOND = 1.0E9
        const val NANOS_PER_MINUTE = 60.0E9
    }

}
