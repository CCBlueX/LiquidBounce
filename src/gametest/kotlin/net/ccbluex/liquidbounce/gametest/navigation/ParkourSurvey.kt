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

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.BasePressurePlateBlock
import net.minecraft.world.level.block.entity.CommandBlockEntity
import net.minecraft.world.level.block.entity.SignBlockEntity
import net.minecraft.world.level.block.entity.SignTextSlot
import net.minecraft.world.level.block.state.BlockState
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.math.abs
import kotlin.math.max

/**
 * Dumps what a loaded map holds after the game upgraded it: command blocks with their commands, signs, pressure
 * plates, and the blocks at every stage's start and finish, so the manifest can be checked against the world.
 */
internal object ParkourSurvey {

    private const val MAX_CHUNKS = 1500
    private const val BATCH = 16

    fun run(context: ClientGameTestContext, server: TestServerContext, map: ParkourMap, output: Path) {
        val points = listOf(map.spawn) + map.stages.flatMap { listOf(it.start, it.finish) }
        val stored = overworldFolder(map.directory, "region")?.let(::storedChunks).orEmpty()
        val chunks = stored.sortedBy { (x, z) ->
            points.minOf { max(abs((it.x shr 4) - x), abs((it.z shr 4) - z)) }
        }.take(MAX_CHUNKS)

        val commandBlocks = JsonArray()
        val signs = JsonArray()
        val plates = JsonArray()
        for (batch in chunks.chunked(BATCH)) {
            server.runOnServer<RuntimeException> { minecraft ->
                val level = minecraft.overworld()
                for ((x, z) in batch) {
                    surveyChunk(level, x, z, commandBlocks, signs, plates)
                }
            }
            // Lets the server unload what it no longer needs
            context.waitTick()
        }

        val result = JsonObject()
        result.addProperty("slug", map.slug)
        result.addProperty("mcVersion", map.mcVersion)
        result.addProperty("storedChunks", stored.size)
        result.addProperty("surveyedChunks", chunks.size)
        result.add("spawn", server.computeOnServer<JsonObject, RuntimeException> {
            spot(it.overworld(), map.spawn)
        })
        result.add("stages", JsonArray().apply {
            for (stage in map.stages) {
                add(JsonObject().apply {
                    addProperty("index", stage.index)
                    addProperty("name", stage.name)
                    server.runOnServer<RuntimeException> {
                        add("start", spot(it.overworld(), stage.start))
                        add("finish", spot(it.overworld(), stage.finish))
                    }
                })
            }
        })
        result.add("commandBlocks", commandBlocks)
        result.add("signs", signs)
        result.add("pressurePlates", plates)
        output.writeText(reportGson.toJson(result))
    }

    private fun surveyChunk(
        level: ServerLevel,
        chunkX: Int,
        chunkZ: Int,
        commandBlocks: JsonArray,
        signs: JsonArray,
        plates: JsonArray,
    ) {
        val chunk = level.getChunk(chunkX, chunkZ)
        for ((pos, entity) in chunk.blockEntities) {
            when (entity) {
                is CommandBlockEntity -> commandBlocks.add(commandBlock(pos, entity))
                is SignBlockEntity -> signs.add(sign(pos, entity))
            }
        }

        for ((index, section) in chunk.sections.withIndex()) {
            if (section.hasOnlyAir() || !section.maybeHas(::isPlate)) {
                continue
            }
            val origin = BlockPos(chunkX shl 4, chunk.getSectionYFromSectionIndex(index) shl 4, chunkZ shl 4)
            for (pos in BlockPos.betweenClosed(origin, origin.offset(15, 15, 15))) {
                val state = section.getBlockState(pos.x and 15, pos.y and 15, pos.z and 15)
                if (isPlate(state)) {
                    plates.add(JsonObject().apply {
                        addBlockPos("pos", pos)
                        addProperty("block", state.id())
                    })
                }
            }
        }
    }

    private fun isPlate(state: BlockState) = state.block is BasePressurePlateBlock

    private fun commandBlock(pos: BlockPos, entity: CommandBlockEntity) = JsonObject().apply {
        addBlockPos("pos", pos)
        addProperty("block", entity.blockState.id())
        addProperty("mode", entity.mode.name)
        addProperty("auto", entity.isAutomatic)
        addProperty("conditional", entity.isConditional)
        addProperty("command", entity.commandBlock.command)
    }

    private fun sign(pos: BlockPos, entity: SignBlockEntity) = JsonObject().apply {
        addBlockPos("pos", pos)
        addProperty("block", entity.blockState.id())
        for (slot in SignTextSlot.entries) {
            val lines = entity.getText(slot).getMessages(false).map { it.string }
            if (lines.any(String::isNotBlank)) {
                add(slot.name.lowercase(), JsonArray().apply { lines.forEach(::add) })
            }
        }
    }

    /**
     * The blocks at the feet, head and ground of [feet], and whether a player can stand there.
     */
    fun spot(level: ServerLevel, feet: BlockPos) = JsonObject().apply {
        addBlockPos("pos", feet)
        val ground = level.getBlockState(feet.below())
        val body = level.getBlockState(feet)
        val head = level.getBlockState(feet.above())
        addProperty("ground", ground.id())
        addProperty("feet", body.id())
        addProperty("head", head.id())
        addProperty("standable", isStandable(level, feet))
    }

    fun isStandable(level: ServerLevel, feet: BlockPos): Boolean {
        val body = level.getBlockState(feet)
        val supported = !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty ||
            !body.getCollisionShape(level, feet).isEmpty ||
            !body.fluidState.isEmpty ||
            body.`is`(BlockTags.CLIMBABLE)
        val free = level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty
        return supported && free
    }

    private fun BlockState.id() = BuiltInRegistries.BLOCK.getKey(block).toString()

}
