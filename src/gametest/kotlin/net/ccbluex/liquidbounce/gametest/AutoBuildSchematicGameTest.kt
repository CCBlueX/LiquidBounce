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
package net.ccbluex.liquidbounce.gametest

import net.ccbluex.agent.v26_3.fabric.litematica.Placements
import net.ccbluex.agent.v26_3.toMinecraft
import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.SingleplayerGameTest
import net.ccbluex.gametest.harness.action.aim
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.fill
import net.ccbluex.gametest.harness.action.query
import net.ccbluex.gametest.harness.action.setBlock
import net.ccbluex.liquidbounce.features.module.modules.world.autobuild.ModuleAutoBuild
import net.ccbluex.liquidbounce.gametest.harness.configure
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BedPart
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

// A budget of three seconds a block, at most 45 minutes
private const val TICKS_PER_BLOCK = 20 * 3
private const val MAX_BUILD_TICKS = 20 * 60 * 45
private const val RUN_TICKS = 20 * 120
private const val SLOTS_PER_CHEST = 27

// What the circuit or the neighbours set rather than the builder
private val DERIVED = setOf(
    BlockStateProperties.POWERED, BlockStateProperties.LIT, BlockStateProperties.TRIGGERED,
    BlockStateProperties.ENABLED, BlockStateProperties.EXTENDED, BlockStateProperties.POWER,
    BlockStateProperties.NORTH_REDSTONE, BlockStateProperties.EAST_REDSTONE, BlockStateProperties.SOUTH_REDSTONE,
    BlockStateProperties.WEST_REDSTONE, BlockStateProperties.NORTH_WALL, BlockStateProperties.EAST_WALL,
    BlockStateProperties.SOUTH_WALL, BlockStateProperties.WEST_WALL, BlockStateProperties.UP,
    BlockStateProperties.LEVEL_COMPOSTER, BlockStateProperties.NOTE, BlockStateProperties.NOTEBLOCK_INSTRUMENT,
    BlockStateProperties.IN_WALL, BlockStateProperties.LOCKED, BlockStateProperties.CRAFTING,
    BlockStateProperties.LEVEL, BlockStateProperties.SHORT,
)

/**
 * AutoBuild's Schematic mode builds every Litematica schematic in the directory `-PgameTestSchematics` names, one
 * after another on empty ground, in survival with the items in chests beside the site: every block comes out as the
 * schematic has it, apart from what the circuit sets, nothing it stood on is left, and the bone meal farms make bone
 * meal once their lever is flipped. Without that directory there is nothing to build.
 */
val autoBuildSchematic: GameTestScope.() -> Unit = {
    val dir = System.getProperty("net.ccbluex.liquidbounce.gametest.schematics")?.let(Path::of)
    if (dir == null || !Placements.isAvailable) {
        logger.info("No schematics to build: run with -PgameTestSchematics=<dir>")
    } else {
        context.restoring(ModuleAutoBuild) {
            run("gamemode survival $playerName")
            run("gamerule do_fire_tick false")
            configure(ModuleAutoBuild, "Mode" to "Schematic")
            val failed = dir.listDirectoryEntries("*.litematic").sorted().mapNotNull { file ->
                runCatching { buildSchematic(file) }.exceptionOrNull()?.let { file.name to it }
            }
            failed.forEach { (name, error) -> logger.error("$name failed", error) }
            check(failed.isEmpty()) { "Not built: ${failed.joinToString { it.first }}" }
        }
    }
}

private fun GameTestScope.buildSchematic(file: Path) {
    val site = origin.offset(4, 0, 4)
    fill(site.offset(-10, 0, -10), site.offset(28, 15, 28), "minecraft:air")
    fill(site.offset(-10, -1, -10), site.offset(28, -1, 28), "minecraft:grass_block")
    run("kill @e[type=item]")
    run("time set noon")
    run("clear $playerName")
    travel(origin)
    val setup = context.fromClient { placeSchematic(file, site) }
    try {
        awaitClient("Litematica to show ${file.name}") { wanted().isNotEmpty() }
        // The schematic world fills in over a few ticks
        ticks(20)
        val blocks = context.fromClient { wanted() }
        stock(blocks)
        val started = System.nanoTime()
        val startTick = context.fromClient { checkNotNull(it.level).gameTime }
        client { ModuleAutoBuild.enabled = true }
        val budget = minOf(MAX_BUILD_TICKS, 20 * 60 + TICKS_PER_BLOCK * blocks.size)
        awaitClient("AutoBuild to build ${file.name}", budget) { !ModuleAutoBuild.enabled }
        val ticks = context.fromClient { checkNotNull(it.level).gameTime } - startTick
        logger.info(
            "Built ${file.name}, ${blocks.size} blocks, in ${ticks / 20} s game time " +
                "(${"%.2f".format(ticks / 20.0 / blocks.size)} s a block), " +
                "${(System.nanoTime() - started) / 1_000_000_000} s wall",
        )
        // Liquids flow on for a moment
        ticks(40)
        checkBuilt(blocks)
        // Only the farms have composters to make bone meal with
        if (blocks.values.any { it.`is`(Blocks.COMPOSTER) }) checkWorks(blocks, setup)
    } finally {
        client {
            ModuleAutoBuild.enabled = false
            removeSchematics()
        }
    }
}

private fun wanted(): Map<BlockPos, BlockState> =
    Placements.blocks().entries.associate { it.key.toMinecraft() to it.value.toMinecraft() }

/**
 * The items building [state] takes, as a player gets them: the block, a bucket of a liquid or of the water it holds.
 */
private fun itemsFor(state: BlockState): List<Item> = when {
    state.block is LiquidBlock -> if (state.fluidState.isSource) listOf(state.fluidState.type.bucket) else emptyList()
    state.block.asItem() == Items.AIR -> emptyList()
    state.getValueOrElse(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER) == DoubleBlockHalf.UPPER ->
        emptyList()
    state.getValueOrElse(BlockStateProperties.BED_PART, BedPart.FOOT) == BedPart.HEAD -> emptyList()
    state.getValueOrElse(BlockStateProperties.WATERLOGGED, false) -> listOf(state.block.asItem(), Items.WATER_BUCKET)
    else -> listOf(state.block.asItem())
}

/** Fills chests west of the site with what [blocks] take and a stack of dirt to stand on. */
private fun GameTestScope.stock(blocks: Map<BlockPos, BlockState>) {
    val counts = blocks.values.flatMap(::itemsFor).groupingBy { it }.eachCount() + (Items.DIRT to 64)
    val stacks = counts.flatMap { (item, count) ->
        val max = item.defaultMaxStackSize
        List((count + max - 1) / max) { item to minOf(max, count - it * max) }
    }
    stacks.chunked(SLOTS_PER_CHEST).forEachIndexed { index, chest ->
        val pos = origin.offset(-2, 0, -6 + index * 2)
        setBlock(pos, "minecraft:chest[facing=east]")
        chest.forEachIndexed { slot, (item, count) ->
            val id = BuiltInRegistries.ITEM.getKey(item)
            run("item replace block ${pos.x} ${pos.y} ${pos.z} container.$slot with $id $count")
        }
    }
    val last = origin.offset(-2, 0, -6 + (stacks.size - 1) / SLOTS_PER_CHEST * 2)
    awaitServer("block ${last.x} ${last.y} ${last.z} minecraft:chest")
    logger.info("Stocked ${stacks.size} stacks: $counts")
}

/** Whether [actual] is [wanted] in all a builder decides. */
private fun matches(actual: BlockState, wanted: BlockState): Boolean {
    if (actual.block != wanted.block) return false
    val values = actual.values.toList()
    return wanted.values.allMatch { it.property() in DERIVED || it in values }
}

private fun GameTestScope.checkBuilt(blocks: Map<BlockPos, BlockState>) {
    val wrong = context.fromClient { mc ->
        val level = checkNotNull(mc.level)
        blocks.filter { (pos, state) -> !matches(level.getBlockState(pos), state) }
            .mapValues { level.getBlockState(it.key) }
    }
    check(wrong.isEmpty()) {
        "${wrong.size} of ${blocks.size} blocks wrong:\n" +
            wrong.entries.joinToString("\n") { (pos, actual) -> "  $pos: $actual instead of ${blocks[pos]}" }
    }
    val min = blocks.keys.reduce(BlockPos::min)
    val max = blocks.keys.reduce(BlockPos::max)
    val stray = context.fromClient { mc ->
        val level = checkNotNull(mc.level)
        BlockPos.betweenClosed(min.offset(-4, 0, -4), max.offset(4, 3, 4)).map(BlockPos::immutable)
            .filter { it !in blocks && !level.getBlockState(it).isAir && !level.getBlockState(it).`is`(Blocks.CHEST) }
            .associateWith(level::getBlockState)
    }
    check(stray.isEmpty()) { "Left around the build: $stray" }
}

/**
 * Feeds the farm as its designer had it, flips its levers and waits for bone meal in its chests; flips them back and
 * waits again when the levers stood the other way.
 */
private fun GameTestScope.checkWorks(blocks: Map<BlockPos, BlockState>, setup: SchematicSetup) {
    for (feeder in setup.feeders) {
        run("item replace block ${feeder.x} ${feeder.y} ${feeder.z} container.0 with minecraft:bone_meal 64")
    }
    for ((crafter, slots) in setup.disabledSlots) {
        run("data merge block ${crafter.x} ${crafter.y} ${crafter.z} {disabled_slots:[I;${slots.joinToString(",")}]}")
    }
    val chests = blocks.filterValues { it.`is`(Blocks.CHEST) }.keys
    val levers = blocks.filterValues { it.`is`(Blocks.LEVER) }.keys
    repeat(2) {
        levers.forEach { flip(it, blocks) }
        val since = System.nanoTime()
        while (ticksSince(since) < RUN_TICKS) {
            ticks(100)
            val made = chests.sumOf { boneMeal(it) }
            if (made > 0) {
                logger.info("The farm made $made bone meal in ${ticksSince(since) / 20} s")
                return
            }
        }
    }
    error("The farm made no bone meal with its levers either way")
}

private fun ticksSince(nanos: Long) = (System.nanoTime() - nanos) / 50_000_000

/** The bone meal in the container at [pos], a bone block counting as nine. */
private fun GameTestScope.boneMeal(pos: BlockPos): Int {
    fun count(item: String) = query(
        "execute if items block ${pos.x} ${pos.y} ${pos.z} container.* $item",
        Regex("""Test (passed|failed)(?:, count: (\d+))?"""),
    ).groups[2]?.value?.toInt() ?: 0
    return count("minecraft:bone_meal") + 9 * count("minecraft:bone_block")
}

/** Walks up to [lever] from a spot of the ground it shows from and flips it with a right-click. */
private fun GameTestScope.flip(lever: BlockPos, blocks: Map<BlockPos, BlockState>) {
    val spot = context.fromClient { mc -> standSpot(mc, lever, blocks) }
        ?: error("Nowhere to flip the lever at $lever from")
    travel(spot)
    aim(lever)
    val powered = context.fromClient {
        checkNotNull(it.level).getBlockState(lever).getValue(BlockStateProperties.POWERED)
    }
    input.pressKey { it.keyUse }
    awaitClient("the lever at $lever to flip") {
        checkNotNull(it.level).getBlockState(lever).getValue(BlockStateProperties.POWERED) != powered
    }
}

private fun standSpot(mc: Minecraft, target: BlockPos, blocks: Map<BlockPos, BlockState>): BlockPos? {
    val level = checkNotNull(mc.level)
    val player = checkNotNull(mc.player)
    val centre = Vec3.atCenterOf(target)
    fun isFree(pos: BlockPos) = pos !in blocks && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty &&
        !level.getBlockState(pos).liquid()
    return BlockPos.betweenClosed(target.offset(-3, -3, -3), target.offset(3, 1, 3)).map(BlockPos::immutable)
        .filter { isFree(it) && isFree(it.above()) && !isFree(it.below()) }
        .filter {
            val eye = Vec3.atBottomCenterOf(it).add(0.0, player.eyeHeight.toDouble(), 0.0)
            eye.distanceTo(centre) < 4.0 &&
                level.clip(ClipContext(eye, centre, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player))
                    .let { hit -> hit is BlockHitResult && hit.blockPos == target }
        }
        .minByOrNull { it.distSqr(target) }
}

/** @see autoBuildSchematic */
class AutoBuildSchematicGameTest : SingleplayerGameTest(autoBuildSchematic) {
    override val retries = 0
}
