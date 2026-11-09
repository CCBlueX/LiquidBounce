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

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.math.roundToLong

internal enum class StageOutcome {
    PASS,
    TIMEOUT,
    FELL,
    STUCK,

    /**
     * The manifest's start is not a place to stand on, so the stage did not run.
     */
    BAD_START,
}

internal class StageResult(
    val index: Int,
    val name: String,
    val outcome: StageOutcome,
    val detail: String,
    val ticks: Int,
    val limitTicks: Int,
    val straightDistance: Double,
    val travelled: Double,
    val closestDistance: Double,
    val jumps: Int,
    val falls: Int,
    val pathFound: Boolean,
    val start: List<Int>,
    val finish: List<Int>,
    val end: List<Double>,
    val failure: StageFailure?,
)

internal class StageFailure(
    val position: List<Double>,
    /**
     * Where the player last stood, and the move the path had there.
     */
    val lastGround: List<Double>?,
    val takeoffMove: String?,
    val path: List<List<Double>>?,
    val pathMoves: List<String>?,
    val pathComplete: Boolean?,
    /**
     * Positions every 5 ticks, the last 20 seconds.
     */
    val trace: List<List<Double>>,
    val effects: List<String>,
    /**
     * What the route view showed: PENDING while searching, INVALID without a route.
     */
    val routeState: String,
    val damage: String?,
    val screenshot: String?,
)

internal class MapResult(
    val slug: String,
    val name: String,
    val mcVersion: String,
    var loadSeconds: Double = 0.0,
    var runSeconds: Double = 0.0,
    var error: String? = null,
    val stages: MutableList<StageResult> = ArrayList(),
)

internal class BenchmarkReport(
    val started: String,
    val commit: String,
    val manifest: String,
    val renderDistance: Int,
    val maps: MutableList<MapResult> = ArrayList(),
) {

    fun write(json: Path, markdown: Path) {
        json.parent.createDirectories()
        json.writeText(reportGson.toJson(this))
        markdown.writeText(markdown())
    }

    private fun markdown() = buildString {
        appendLine("# Jump n' Run benchmark $started")
        appendLine()
        appendLine("Commit `$commit`, render distance $renderDistance, manifest `$manifest`.")
        appendLine()
        appendLine("| Map | MC | Passed | Median ticks (passed) | Runtime |")
        appendLine("| --- | --- | --- | --- | --- |")
        for (map in maps) {
            val passed = map.stages.filter { it.outcome == StageOutcome.PASS }
            val median = passed.map { it.ticks }.sorted().let { if (it.isEmpty()) "-" else it[it.size / 2].toString() }
            val runtime = "${(map.loadSeconds + map.runSeconds).roundToLong()} s"
            val stages = "${passed.size}/${map.stages.size}"
            appendLine("| ${map.name} (`${map.slug}`) | ${map.mcVersion} | $stages | $median | $runtime |")
        }

        val kinds = maps.flatMap { it.stages }.groupingBy { it.outcome }.eachCount()
        appendLine()
        appendLine("Outcomes: " + kinds.entries.joinToString { "${it.key} ${it.value}" })

        for (map in maps) {
            appendLine()
            appendLine("## ${map.name} (`${map.slug}`, ${map.mcVersion})")
            map.error?.let { appendLine("- Harness error: $it") }
            for (stage in map.stages) {
                append("- ${stage.index} ${stage.name}: ${stage.outcome}, ${stage.ticks}/${stage.limitTicks} ticks, ")
                append("${stage.straightDistance} blocks straight, travelled ${stage.travelled}, ")
                append("${stage.jumps} jumps, ${stage.falls} falls")
                if (stage.outcome != StageOutcome.PASS) {
                    append(". ${stage.detail}; ended at ${stage.end.joinToString(" ")}")
                    append(", closest ${stage.closestDistance}")
                    stage.failure?.let { append("; route ${it.routeState}") }
                    stage.failure?.takeoffMove?.let { append("; last ground move $it") }
                    stage.failure?.screenshot?.let { append("; shot `shots/${Path.of(it).fileName}`") }
                }
                appendLine()
            }
        }
    }

}

internal fun Double.rounded() = (this * 100.0).roundToLong() / 100.0

internal fun Vec3.rounded() = listOf(x.rounded(), y.rounded(), z.rounded())

internal fun BlockPos.list() = listOf(x, y, z)
