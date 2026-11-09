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
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.readText

private const val PROPERTY = "net.ccbluex.liquidbounce.gametest"

/**
 * The Jump n' Run benchmark settings, from system properties that build.gradle.kts fills in from
 * `-PgameTestParkourMaps`, `-PgameTestParkourFilter` and `-PgameTestParkourSurvey`.
 */
internal object ParkourSettings {

    /**
     * The manifest describing the maps; without it the benchmark does nothing.
     */
    val manifest: Path? = System.getProperty("$PROPERTY.parkourMaps")?.takeIf(String::isNotBlank)?.let(Path::of)

    /**
     * Maps and stages to run: `slug` or `slug:0,2`, several separated by `;`.
     */
    val filter: Map<String, Set<Int>?> = System.getProperty("$PROPERTY.parkourFilter").orEmpty()
        .split(';')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .associate { entry ->
            val slug = entry.substringBefore(':')
            val stages = entry.substringAfter(':', "").takeIf(String::isNotBlank)
                ?.split(',')
                ?.mapTo(HashSet()) { it.trim().toInt() }
            slug to stages
        }

    /**
     * Only dumps command blocks, signs and pressure plates of every map instead of running the stages.
     */
    val survey = System.getProperty("$PROPERTY.parkourSurvey").toBoolean()

    val reports: Path?
        get() = manifest?.toAbsolutePath()?.parent?.resolveSibling("reports")

    fun includes(map: ParkourMap) = filter.isEmpty() || map.slug in filter

    fun includes(map: ParkourMap, stage: ParkourStage) =
        includes(map) && filter[map.slug].let { it == null || stage.index in it }

}

internal class ParkourMap(
    val slug: String,
    val name: String,
    val mcVersion: String,
    val directory: Path,
    val spawn: BlockPos,
    val stages: List<ParkourStage>,

    /**
     * Highest drop the walker may take, for maps whose way on is a drop.
     */
    val maxFall: Int,
)

internal class ParkourStage(
    val index: Int,
    val name: String,
    val start: BlockPos,
    val yaw: Float,
    val finish: BlockPos,
    val finishRadius: Double,
) {

    val goal: Vec3
        get() = Vec3(finish.x + 0.5, finish.y.toDouble(), finish.z + 0.5)

    val straightDistance: Double
        get() = Vec3.atBottomCenterOf(start).distanceTo(goal)

}

internal fun readManifest(manifest: Path): List<ParkourMap> {
    val root = JsonParser.parseString(manifest.readText()).asJsonObject
    return root.getAsJsonArray("maps").map { element ->
        val map = element.asJsonObject
        val slug = map["slug"].asString
        val stages = map.getAsJsonArray("stages").mapIndexed { index, stageElement ->
            val stage = stageElement.asJsonObject
            ParkourStage(
                index = index,
                name = stage["name"]?.asString ?: "Stage ${index + 1}",
                start = stage.getAsJsonArray("start").toBlockPos(),
                yaw = stage["yaw"]?.asFloat ?: 0f,
                finish = stage.getAsJsonArray("finish").toBlockPos(),
                finishRadius = stage["finishRadius"]?.asDouble ?: 1.0,
            )
        }
        ParkourMap(
            slug = slug,
            name = map["name"]?.asString ?: slug,
            mcVersion = map["mcVersion"]?.asString ?: "unknown",
            directory = manifest.toAbsolutePath().parent.resolve(map["path"]?.asString ?: slug),
            spawn = map.getAsJsonArray("spawn")?.toBlockPos() ?: stages.first().start,
            stages = stages,
            maxFall = map["maxFall"]?.asInt ?: DEFAULT_MAX_FALL,
        )
    }
}

private fun JsonArray.toBlockPos() = BlockPos(this[0].asInt, this[1].asInt, this[2].asInt)

/**
 * The folder of a world save that holds [name] (`region`, `entities`, `poi`) for the overworld, in the layout of
 * 26.1 and later or the one before it.
 */
internal fun overworldFolder(save: Path, name: String): Path? =
    listOf(save.resolve("dimensions/minecraft/overworld/$name"), save.resolve(name)).firstOrNull { it.isDirectory() }

/**
 * Chunks stored in the region files of [regionFolder], read from the location table of each file.
 */
internal fun storedChunks(regionFolder: Path): List<Pair<Int, Int>> {
    val chunks = ArrayList<Pair<Int, Int>>()
    val pattern = Regex("""r\.(-?\d+)\.(-?\d+)\.mca""")
    Files.list(regionFolder).use { files ->
        for (file in files) {
            val match = pattern.matchEntire(file.fileName.toString()) ?: continue
            val regionX = match.groupValues[1].toInt()
            val regionZ = match.groupValues[2].toInt()
            val header = Files.newInputStream(file).use { it.readNBytes(REGION_HEADER) }
            if (header.size < REGION_HEADER) {
                continue
            }
            val locations = ByteBuffer.wrap(header).asIntBuffer()
            for (index in 0 until REGION_CHUNKS) {
                if (locations[index] != 0) {
                    val x = regionX * REGION_SIZE + index % REGION_SIZE
                    val z = regionZ * REGION_SIZE + index / REGION_SIZE
                    chunks += x to z
                }
            }
        }
    }
    return chunks
}

private const val DEFAULT_MAX_FALL = 3
private const val REGION_SIZE = 32
private const val REGION_CHUNKS = REGION_SIZE * REGION_SIZE
private const val REGION_HEADER = REGION_CHUNKS * Int.SIZE_BYTES

internal val reportGson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

internal fun JsonObject.addBlockPos(name: String, pos: BlockPos) = add(name, JsonArray().apply {
    add(pos.x)
    add(pos.y)
    add(pos.z)
})
